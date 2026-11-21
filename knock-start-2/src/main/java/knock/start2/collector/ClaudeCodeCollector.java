package knock.start2.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import knock.start2.domain.AiSession;
import knock.start2.domain.AiSessionRepository;
import knock.start2.domain.PromptEntry;
import knock.start2.domain.PromptEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Claude Code 대화 기록 폴더를 주기적으로 스캔해 바뀐 세션만 DB 에 반영한다. */
@Service
public class ClaudeCodeCollector {

    public static final String SOURCE = "claude-code";
    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeCollector.class);

    private final AiSessionRepository sessions;
    private final PromptEntryRepository prompts;
    private final TransactionTemplate tx;
    private final List<Path> roots;
    private final ClaudeCodeTranscriptParser parser = new ClaudeCodeTranscriptParser();
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile CollectResult lastResult;

    public ClaudeCodeCollector(AiSessionRepository sessions, PromptEntryRepository prompts, TransactionTemplate tx,
                               @Value("${collector.claude-code.paths}") List<String> paths) {
        this.sessions = sessions;
        this.prompts = prompts;
        this.tx = tx;
        this.roots = paths.stream().map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get).toList();
    }

    @Scheduled(initialDelay = 2000, fixedDelayString = "${collector.interval-ms}")
    public void scheduled() {
        CollectResult r = collect();
        if (r.imported() > 0 || !r.errors().isEmpty()) {
            log.info("수집 완료: 스캔 {}, 반영 {}, 오류 {}", r.scanned(), r.imported(), r.errors().size());
        }
    }

    public synchronized CollectResult collect() {
        int scanned = 0, imported = 0, unchanged = 0;
        List<String> errors = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            // <root>/<project>/<session>.jsonl 만 대상 (하위 subagents 폴더는 제외)
            try (Stream<Path> files = Files.walk(root, 2)) {
                for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".jsonl") && Files.isRegularFile(p))::iterator) {
                    scanned++;
                    try {
                        if (importFile(f)) imported++; else unchanged++;
                    } catch (Exception e) {
                        errors.add(f + ": " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                errors.add(root + ": " + e.getMessage());
            }
        }
        lastResult = new CollectResult(scanned, imported, unchanged, errors, Instant.now());
        return lastResult;
    }

    public CollectResult lastResult() {
        return lastResult;
    }

    boolean importFile(Path file) throws IOException {
        long size = Files.size(file);
        long modified = Files.getLastModifiedTime(file).toMillis();
        String fileId = file.getFileName().toString().replaceFirst("\\.jsonl$", "");

        AiSession existing = sessions.findById(fileId).orElse(null);
        if (existing != null && existing.getSourceSize() == size && existing.getSourceModified() == modified) {
            return false;
        }

        ParsedSession p;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p = parser.parse(fileId, r);
        }
        if (p.prompts().isEmpty() && p.assistantCount() == 0) {
            return false; // 내용 없는 세션은 건너뜀
        }

        AiSession s = existing != null ? existing : new AiSession();
        s.setId(fileId);
        s.setSource(SOURCE);
        s.setProject(p.project());
        s.setProjectName(projectName(p.project(), file));
        s.setGitBranch(p.gitBranch());
        s.setModel(p.model());
        s.setTitle(title(p));
        s.setPromptCount(p.prompts().size());
        s.setAssistantCount(p.assistantCount());
        s.setToolCallCount(p.toolCallCount());
        s.setToolUsageJson(toJson(p));
        s.setFilesTouched(String.join("\n", p.filesTouched()));
        s.setInputTokens(p.inputTokens());
        s.setOutputTokens(p.outputTokens());
        s.setCacheReadTokens(p.cacheReadTokens());
        s.setStartedAt(p.startedAt());
        s.setEndedAt(p.endedAt());
        s.setSourceFile(file.toString());
        s.setSourceSize(size);
        s.setSourceModified(modified);

        tx.executeWithoutResult(status -> {
            sessions.save(s);
            prompts.deleteBySessionId(fileId);
            int seq = 0;
            for (ParsedSession.Prompt pr : p.prompts()) {
                prompts.save(new PromptEntry(fileId, seq++, pr.text(), pr.timestamp()));
            }
        });
        return true;
    }

    static String title(ParsedSession p) {
        String t = p.summary();
        if ((t == null || t.isBlank()) && !p.prompts().isEmpty()) {
            t = p.prompts().get(0).text().lines().filter(l -> !l.isBlank()).findFirst().orElse("");
        }
        if (t == null || t.isBlank()) return "(요청 없음)";
        t = t.strip();
        return t.length() > 120 ? t.substring(0, 117) + "..." : t;
    }

    private static String projectName(String cwd, Path file) {
        if (cwd != null && !cwd.isBlank()) {
            Path name = Paths.get(cwd).getFileName();
            if (name != null) return name.toString();
        }
        return file.getParent().getFileName().toString();
    }

    private String toJson(ParsedSession p) {
        try {
            return mapper.writeValueAsString(p.toolUsage());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
