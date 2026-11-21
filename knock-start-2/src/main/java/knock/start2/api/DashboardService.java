package knock.start2.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import knock.start2.api.Dtos.*;
import knock.start2.domain.AiSession;
import knock.start2.domain.AiSessionRepository;
import knock.start2.domain.PromptEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 혼자 쓰는 도구라 데이터가 많지 않으므로 집계는 메모리에서 단순하게 처리한다. */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final AiSessionRepository sessions;
    private final PromptEntryRepository prompts;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardService(AiSessionRepository sessions, PromptEntryRepository prompts) {
        this.sessions = sessions;
        this.prompts = prompts;
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> list(String q, String project, Integer days, boolean starredOnly) {
        Set<String> promptHits = (q == null || q.isBlank()) ? null : prompts.findSessionIdsByTextContaining(q.strip());
        String ql = q == null ? null : q.strip().toLowerCase();
        return filtered(project, days).stream()
                .filter(s -> !starredOnly || s.isStarred())
                .filter(s -> promptHits == null
                        || promptHits.contains(s.getId())
                        || contains(s.getTitle(), ql) || contains(s.getTags(), ql) || contains(s.getMemo(), ql))
                .sorted(Comparator.comparing(AiSession::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionDetail detail(String id) {
        AiSession s = find(id);
        List<PromptDto> ps = prompts.findBySessionIdOrderBySeq(id).stream()
                .map(p -> new PromptDto(p.getSeq(), p.getText(), p.getTimestamp())).toList();
        List<String> files = s.getFilesTouched() == null || s.getFilesTouched().isBlank()
                ? List.of() : Arrays.asList(s.getFilesTouched().split("\n"));
        return new SessionDetail(summary(s), s.getProject(), toolUsage(s), files,
                s.getInputTokens(), s.getCacheReadTokens(), s.getMemo(), ps);
    }

    @Transactional
    public SessionSummary update(String id, SessionUpdate u) {
        AiSession s = find(id);
        if (u.memo() != null) s.setMemo(u.memo());
        if (u.tags() != null) s.setTags(normalizeTags(u.tags()));
        if (u.starred() != null) s.setStarred(u.starred());
        return summary(sessions.save(s));
    }

    @Transactional(readOnly = true)
    public List<String> projects() {
        return sessions.findAll().stream().map(AiSession::getProjectName).distinct().sorted().toList();
    }

    @Transactional(readOnly = true)
    public Stats stats(String project, Integer days) {
        List<AiSession> list = filtered(project, days);

        int promptsSum = 0, tools = 0;
        long in = 0, out = 0, active = 0;
        Map<LocalDate, long[]> daily = new HashMap<>();
        Map<String, Long> byProject = new HashMap<>(), byTool = new HashMap<>(), byModel = new HashMap<>();
        for (AiSession s : list) {
            promptsSum += s.getPromptCount();
            tools += s.getToolCallCount();
            in += s.getInputTokens();
            out += s.getOutputTokens();
            active += duration(s);
            if (s.getStartedAt() != null) {
                long[] d = daily.computeIfAbsent(s.getStartedAt().atZone(ZONE).toLocalDate(), k -> new long[3]);
                d[0]++;
                d[1] += s.getPromptCount();
                d[2] += s.getOutputTokens();
            }
            byProject.merge(s.getProjectName(), (long) s.getPromptCount(), Long::sum);
            if (s.getModel() != null) byModel.merge(s.getModel(), 1L, Long::sum);
            toolUsage(s).forEach((k, v) -> byTool.merge(k, (long) v, Long::sum));
        }

        // 기간 내 빈 날짜도 0 으로 채워 추이가 끊기지 않게 한다
        List<DailyPoint> points = new ArrayList<>();
        if (!daily.isEmpty()) {
            LocalDate today = LocalDate.now(ZONE);
            LocalDate from = days != null && days > 0 ? today.minusDays(days - 1L)
                    : daily.keySet().stream().min(LocalDate::compareTo).orElse(today);
            for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
                long[] v = daily.getOrDefault(d, new long[3]);
                points.add(new DailyPoint(d, (int) v[0], (int) v[1], v[2]));
            }
        }

        Totals totals = new Totals(list.size(), promptsSum, tools, in, out, active,
                (int) list.stream().map(AiSession::getProjectName).distinct().count());
        return new Stats(totals, points, top(byProject, 10), top(byTool, 10), top(byModel, 8));
    }

    private List<AiSession> filtered(String project, Integer days) {
        Instant since = days != null && days > 0
                ? LocalDate.now(ZONE).minusDays(days - 1L).atStartOfDay(ZONE).toInstant() : null;
        return sessions.findAll().stream()
                .filter(s -> project == null || project.isBlank() || project.equals(s.getProjectName()))
                .filter(s -> since == null || (s.getStartedAt() != null && !s.getStartedAt().isBefore(since)))
                .toList();
    }

    private static List<NamedCount> top(Map<String, Long> m, int n) {
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n).map(e -> new NamedCount(e.getKey(), e.getValue())).toList();
    }

    private AiSession find(String id) {
        return sessions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SessionSummary summary(AiSession s) {
        return new SessionSummary(s.getId(), s.getSource(), s.getProjectName(), s.getGitBranch(), s.getModel(),
                s.getTitle(), s.getPromptCount(), s.getToolCallCount(), s.getOutputTokens(),
                s.getStartedAt(), s.getEndedAt(), duration(s), s.getTags(), s.isStarred(),
                s.getMemo() != null && !s.getMemo().isBlank());
    }

    private static long duration(AiSession s) {
        return s.getStartedAt() == null || s.getEndedAt() == null ? 0
                : Duration.between(s.getStartedAt(), s.getEndedAt()).getSeconds();
    }

    private Map<String, Integer> toolUsage(AiSession s) {
        if (s.getToolUsageJson() == null) return Map.of();
        try {
            return mapper.readValue(s.getToolUsageJson(), new TypeReference<LinkedHashMap<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static boolean contains(String hay, String needle) {
        return hay != null && needle != null && hay.toLowerCase().contains(needle);
    }

    private static String normalizeTags(String tags) {
        return Arrays.stream(tags.split(",")).map(String::strip).filter(t -> !t.isEmpty()).distinct()
                .reduce((a, b) -> a + "," + b).orElse("");
    }

}
