package knock.start2.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Claude Code 대화 기록(~/.claude/projects/&lt;project&gt;/&lt;session&gt;.jsonl)을 파싱한다.
 * 한 줄이 JSON 이벤트 1개이며, 사람이 입력한 요청 / AI 응답 / 도구 호출을 추려낸다.
 */
public class ClaudeCodeTranscriptParser {

    private static final Set<String> FILE_TOOLS = Set.of("Edit", "Write", "MultiEdit", "NotebookEdit");
    private static final Pattern COMMAND_NAME = Pattern.compile("<command-name>(.*?)</command-name>", Pattern.DOTALL);
    private static final Pattern COMMAND_ARGS = Pattern.compile("<command-args>(.*?)</command-args>", Pattern.DOTALL);

    private final ObjectMapper mapper = new ObjectMapper();

    public ParsedSession parse(String fallbackSessionId, BufferedReader reader) throws IOException {
        String sessionId = null;
        String project = null;
        String gitBranch = null;
        String model = null;
        String summary = null;
        List<ParsedSession.Prompt> prompts = new ArrayList<>();
        Set<String> assistantMessageIds = new HashSet<>();
        Set<String> toolUseIds = new HashSet<>();
        Map<String, Integer> toolUsage = new LinkedHashMap<>();
        Set<String> files = new LinkedHashSet<>();
        long inTok = 0, outTok = 0, cacheTok = 0;
        Instant start = null, end = null;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            JsonNode n;
            try {
                n = mapper.readTree(line);
            } catch (IOException e) {
                continue; // 기록 중이던 마지막 줄 등 깨진 줄은 무시
            }
            String type = n.path("type").asText("");

            if ("summary".equals(type) && n.hasNonNull("summary")) {
                summary = n.get("summary").asText();
                continue;
            }
            if (n.path("isSidechain").asBoolean(false)) continue; // 서브에이전트 내부 대화 제외

            Instant ts = parseTs(n.path("timestamp").asText(null));
            if (!"user".equals(type) && !"assistant".equals(type)) continue;
            if (ts != null) {
                if (start == null || ts.isBefore(start)) start = ts;
                if (end == null || ts.isAfter(end)) end = ts;
            }
            if (sessionId == null && n.hasNonNull("sessionId")) sessionId = n.get("sessionId").asText();
            if (project == null && n.hasNonNull("cwd")) project = n.get("cwd").asText(); // 세션을 시작한 위치 기준
            if (n.hasNonNull("gitBranch") && !n.get("gitBranch").asText().isEmpty()) gitBranch = n.get("gitBranch").asText();

            JsonNode msg = n.path("message");
            if ("user".equals(type)) {
                String text = humanPromptText(n, msg);
                if (text != null) prompts.add(new ParsedSession.Prompt(text, ts));
            } else {
                String msgId = msg.path("id").asText(n.path("uuid").asText(""));
                // 스트리밍 응답은 같은 message.id 로 여러 줄에 나뉘어 기록되므로 사용량은 1번만 센다
                if (assistantMessageIds.add(msgId)) {
                    JsonNode u = msg.path("usage");
                    inTok += u.path("input_tokens").asLong(0) + u.path("cache_creation_input_tokens").asLong(0);
                    outTok += u.path("output_tokens").asLong(0);
                    cacheTok += u.path("cache_read_input_tokens").asLong(0);
                    String m = msg.path("model").asText("");
                    if (!m.isEmpty() && !m.startsWith("<")) model = m;
                }
                for (JsonNode c : msg.path("content")) {
                    if (!"tool_use".equals(c.path("type").asText())) continue;
                    String id = c.path("id").asText("");
                    if (!id.isEmpty() && !toolUseIds.add(id)) continue;
                    String name = c.path("name").asText("unknown");
                    toolUsage.merge(name, 1, Integer::sum);
                    if (FILE_TOOLS.contains(name)) {
                        String fp = c.path("input").path("file_path").asText(c.path("input").path("notebook_path").asText(""));
                        if (!fp.isEmpty()) files.add(fp);
                    }
                }
            }
        }

        return new ParsedSession(
                sessionId != null ? sessionId : fallbackSessionId,
                project, gitBranch, model, summary, prompts,
                assistantMessageIds.size(), toolUsage, files,
                inTok, outTok, cacheTok, start, end);
    }

    /** 사람이 직접 입력한 요청이면 텍스트를, 도구 결과·시스템 메시지면 null 을 돌려준다. */
    String humanPromptText(JsonNode n, JsonNode msg) {
        if (n.path("isMeta").asBoolean(false)) return null;
        if (n.has("toolUseResult")) return null;
        String kind = n.path("origin").path("kind").asText("");
        if (!kind.isEmpty() && !"human".equals(kind)) return null;

        JsonNode content = msg.path("content");
        String text;
        if (content.isTextual()) {
            text = content.asText();
        } else if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode c : content) {
                String t = c.path("type").asText();
                if ("tool_result".equals(t)) return null;
                if ("text".equals(t)) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(c.path("text").asText());
                }
            }
            text = sb.toString();
        } else {
            return null;
        }
        text = text.strip();
        if (text.isEmpty()) return null;

        Matcher cmd = COMMAND_NAME.matcher(text);
        if (cmd.find()) {
            Matcher args = COMMAND_ARGS.matcher(text);
            String a = args.find() ? args.group(1).strip() : "";
            return (cmd.group(1).strip() + " " + a).strip();
        }
        if (text.startsWith("<local-command") || text.startsWith("<system-reminder")
                || text.startsWith("Caveat:") || text.startsWith("[Request interrupted")
                || text.startsWith("<task-notification") || text.startsWith("<bash-")) {
            return null;
        }
        return text;
    }

    private static Instant parseTs(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
