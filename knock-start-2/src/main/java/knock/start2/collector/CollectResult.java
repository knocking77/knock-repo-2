package knock.start2.collector;

import java.time.Instant;
import java.util.List;

public record CollectResult(int scanned, int imported, int unchanged, List<String> errors, Instant finishedAt) {}
