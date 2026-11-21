package knock.start2.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import knock.start2.api.Dtos.NamedCount;
import knock.start2.collector.ChannelSheetCollector;
import knock.start2.domain.ChannelMessage;
import knock.start2.domain.ChannelMessageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI 공통 채널(시트) 메시지 조회·집계. */
@RestController
@RequestMapping("/api/channel")
public class ChannelController {

    public record ChannelStats(boolean enabled, int total, int open, int participants,
                               List<NamedCount> bySender, List<NamedCount> byStatus, List<NamedCount> flows) {}

    private final ChannelMessageRepository repo;
    private final ChannelSheetCollector collector;

    public ChannelController(ChannelMessageRepository repo, ChannelSheetCollector collector) {
        this.repo = repo;
        this.collector = collector;
    }

    @GetMapping("/stats")
    public ChannelStats stats(@RequestParam(required = false) Integer days) {
        List<ChannelMessage> list = filtered(days);
        Map<String, Long> bySender = new HashMap<>(), byStatus = new HashMap<>(), flows = new HashMap<>();
        java.util.Set<String> people = new java.util.HashSet<>();
        int open = 0;
        for (ChannelMessage m : list) {
            String s = Objects.requireNonNullElse(m.getSender(), "(미상)");
            String r = Objects.requireNonNullElse(m.getReceiver(), "(미상)");
            bySender.merge(s, 1L, Long::sum);
            byStatus.merge(Objects.requireNonNullElse(m.getStatus(), "(없음)"), 1L, Long::sum);
            flows.merge(s + " → " + r, 1L, Long::sum);
            people.add(s);
            if (!"모두".equals(r)) people.add(r);
            if (!"완료".equals(m.getStatus())) open++;
        }
        boolean enabled = collector.enabled() || !list.isEmpty() || repo.count() > 0;
        return new ChannelStats(enabled, list.size(), open, people.size(),
                top(bySender, 10), top(byStatus, 5), top(flows, 10));
    }

    @GetMapping("/messages")
    public List<ChannelMessage> messages(@RequestParam(required = false) Integer days,
                                         @RequestParam(required = false) String participant,
                                         @RequestParam(defaultValue = "false") boolean openOnly) {
        return filtered(days).stream()
                .filter(m -> participant == null || participant.isBlank()
                        || participant.equals(m.getSender()) || participant.equals(m.getReceiver()))
                .filter(m -> !openOnly || !"완료".equals(m.getStatus()))
                .sorted(Comparator.comparing(ChannelMessage::getNo).reversed())
                .toList();
    }

    private List<ChannelMessage> filtered(Integer days) {
        ZoneId zone = ZoneId.systemDefault();
        Instant since = days != null && days > 0
                ? LocalDate.now(zone).minusDays(days - 1L).atStartOfDay(zone).toInstant() : null;
        return repo.findAll().stream()
                .filter(m -> since == null || m.getSentAt() == null || !m.getSentAt().isBefore(since))
                .toList();
    }

    private static List<NamedCount> top(Map<String, Long> m, int n) {
        return m.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n).map(e -> new NamedCount(e.getKey(), e.getValue())).toList();
    }
}
