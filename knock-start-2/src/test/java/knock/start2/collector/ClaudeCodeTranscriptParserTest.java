package knock.start2.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ClaudeCodeTranscriptParserTest {

    private static final String JSONL = String.join("\n",
            "{\"type\":\"queue-operation\",\"operation\":\"enqueue\",\"timestamp\":\"2026-11-21T10:00:00Z\"}",
            "{\"type\":\"user\",\"sessionId\":\"s1\",\"cwd\":\"/home/me/proj-a\",\"gitBranch\":\"main\",\"timestamp\":\"2026-11-21T10:00:01Z\",\"origin\":{\"kind\":\"human\"},\"message\":{\"role\":\"user\",\"content\":\"대시보드 만들어줘\\n자세히\"}}",
            "{\"type\":\"assistant\",\"timestamp\":\"2026-11-21T10:00:05Z\",\"message\":{\"id\":\"m1\",\"model\":\"claude-x\",\"usage\":{\"input_tokens\":10,\"output_tokens\":5,\"cache_read_input_tokens\":100},\"content\":[{\"type\":\"tool_use\",\"id\":\"t1\",\"name\":\"Edit\",\"input\":{\"file_path\":\"/a.java\"}}]}}",
            // 같은 message.id 의 스트리밍 조각: 사용량 중복 집계 금지
            "{\"type\":\"assistant\",\"timestamp\":\"2026-11-21T10:00:06Z\",\"message\":{\"id\":\"m1\",\"model\":\"claude-x\",\"usage\":{\"input_tokens\":10,\"output_tokens\":5},\"content\":[{\"type\":\"tool_use\",\"id\":\"t2\",\"name\":\"Bash\",\"input\":{}}]}}",
            "{\"type\":\"user\",\"timestamp\":\"2026-11-21T10:00:07Z\",\"toolUseResult\":{},\"message\":{\"content\":[{\"type\":\"tool_result\",\"content\":\"ok\"}]}}",
            "{\"type\":\"user\",\"timestamp\":\"2026-11-21T10:00:08Z\",\"isMeta\":true,\"message\":{\"content\":\"meta\"}}",
            "{\"type\":\"user\",\"timestamp\":\"2026-11-21T10:00:09Z\",\"isSidechain\":true,\"message\":{\"content\":\"subagent\"}}",
            "{\"type\":\"user\",\"timestamp\":\"2026-11-21T10:01:00Z\",\"message\":{\"content\":\"<command-name>/review</command-name><command-args>42</command-args>\"}}",
            "{\"type\":\"user\",\"timestamp\":\"2026-11-21T10:02:00Z\",\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"테스트도 추가\"}]}}",
            "{broken json",
            "");

    @Test
    void parsesHumanPromptsToolsAndUsage() throws Exception {
        ParsedSession p = new ClaudeCodeTranscriptParser().parse("fallback", new BufferedReader(new StringReader(JSONL)));

        assertThat(p.sessionId()).isEqualTo("s1");
        assertThat(p.project()).isEqualTo("/home/me/proj-a");
        assertThat(p.gitBranch()).isEqualTo("main");
        assertThat(p.model()).isEqualTo("claude-x");
        assertThat(p.prompts()).extracting(ParsedSession.Prompt::text)
                .containsExactly("대시보드 만들어줘\n자세히", "/review 42", "테스트도 추가");
        assertThat(p.assistantCount()).isEqualTo(1);
        assertThat(p.inputTokens()).isEqualTo(10);
        assertThat(p.outputTokens()).isEqualTo(5);
        assertThat(p.cacheReadTokens()).isEqualTo(100);
        assertThat(p.toolUsage()).containsEntry("Edit", 1).containsEntry("Bash", 1);
        assertThat(p.filesTouched()).containsExactly("/a.java");
        assertThat(p.startedAt()).hasToString("2026-11-21T10:00:01Z");
        assertThat(p.endedAt()).hasToString("2026-11-21T10:02:00Z");
        assertThat(ClaudeCodeCollector.title(p)).isEqualTo("대시보드 만들어줘");
    }
}
