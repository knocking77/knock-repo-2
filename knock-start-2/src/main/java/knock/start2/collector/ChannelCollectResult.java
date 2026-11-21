package knock.start2.collector;

import java.time.Instant;

public record ChannelCollectResult(boolean enabled, int messages, String error, Instant finishedAt) {}
