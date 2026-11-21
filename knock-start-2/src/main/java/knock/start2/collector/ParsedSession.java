package knock.start2.collector;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 대화 기록 파일 1개를 파싱한 결과. */
public record ParsedSession(
        String sessionId,
        String project,
        String gitBranch,
        String model,
        String summary,
        List<Prompt> prompts,
        int assistantCount,
        Map<String, Integer> toolUsage,
        Set<String> filesTouched,
        long inputTokens,
        long outputTokens,
        long cacheReadTokens,
        Instant startedAt,
        Instant endedAt) {

    public record Prompt(String text, Instant timestamp) {}

    public int toolCallCount() {
        return toolUsage.values().stream().mapToInt(Integer::intValue).sum();
    }
}
