package knock.start2.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    public record SessionSummary(String id, String source, String projectName, String gitBranch, String model,
                                 String title, int promptCount, int toolCallCount, long outputTokens,
                                 Instant startedAt, Instant endedAt, long durationSec,
                                 String tags, boolean starred, boolean hasMemo) {}

    public record PromptDto(int seq, String text, Instant timestamp) {}

    public record SessionDetail(SessionSummary summary, String project, Map<String, Integer> toolUsage,
                                List<String> filesTouched, long inputTokens, long cacheReadTokens,
                                String memo, List<PromptDto> prompts) {}

    public record SessionUpdate(String memo, String tags, Boolean starred) {}

    public record Totals(int sessions, int prompts, int toolCalls, long inputTokens, long outputTokens,
                         long activeSec, int projects) {}

    public record DailyPoint(LocalDate date, int sessions, int prompts, long outputTokens) {}

    public record NamedCount(String name, long value) {}

    public record Stats(Totals totals, List<DailyPoint> daily, List<NamedCount> byProject,
                        List<NamedCount> byTool, List<NamedCount> byModel) {}
}
