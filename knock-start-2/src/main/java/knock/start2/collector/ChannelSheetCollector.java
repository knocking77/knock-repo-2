package knock.start2.collector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import knock.start2.domain.ChannelMessage;
import knock.start2.domain.ChannelMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * "AI 공통 채널" Google 시트의 메시지 탭을 CSV 로 내려받아 저장한다.
 * 인증 없이 읽으므로 시트가 "링크가 있는 모든 사용자: 뷰어"로 공유되어 있어야 한다.
 * 시트 ID 가 비어 있으면 동작하지 않는다.
 */
@Service
public class ChannelSheetCollector {

    private static final Logger log = LoggerFactory.getLogger(ChannelSheetCollector.class);
    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy. M. d HH:mm"),
            DateTimeFormatter.ofPattern("yyyy. M. d H:mm:ss"),
    };

    private final ChannelMessageRepository repo;
    private final TransactionTemplate tx;
    private final String sheetId;
    private final String gid;
    private final ZoneId zone;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
    private volatile ChannelCollectResult lastResult;

    public ChannelSheetCollector(ChannelMessageRepository repo, TransactionTemplate tx,
                                 @Value("${collector.sheet.id:}") String sheetId,
                                 @Value("${collector.sheet.gid:0}") String gid,
                                 @Value("${collector.sheet.zone:Asia/Seoul}") String zone) {
        this.repo = repo;
        this.tx = tx;
        this.sheetId = sheetId.trim();
        this.gid = gid.trim();
        this.zone = ZoneId.of(zone);
    }

    public boolean enabled() {
        return !sheetId.isEmpty();
    }

    @Scheduled(initialDelay = 3000, fixedDelayString = "${collector.interval-ms}")
    public void scheduled() {
        if (!enabled()) return;
        ChannelCollectResult r = collect();
        if (r.error() != null) log.warn("공통 채널 수집 실패: {}", r.error());
    }

    public synchronized ChannelCollectResult collect() {
        if (!enabled()) {
            return lastResult = new ChannelCollectResult(false, 0, null, Instant.now());
        }
        try {
            String url = "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=csv&gid=" + gid;
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String type = res.headers().firstValue("content-type").orElse("");
            if (res.statusCode() != 200 || type.contains("text/html")) {
                throw new IOException("시트를 읽을 수 없습니다 (HTTP " + res.statusCode()
                        + "). 시트 공유 설정을 '링크가 있는 모든 사용자: 뷰어'로 바꿔주세요.");
            }
            List<ChannelMessage> messages = parse(res.body());
            tx.executeWithoutResult(s -> {
                repo.deleteAllInBatch();
                repo.saveAll(messages);
            });
            return lastResult = new ChannelCollectResult(true, messages.size(), null, Instant.now());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return lastResult = new ChannelCollectResult(true, 0, e.getMessage(), Instant.now());
        }
    }

    public ChannelCollectResult lastResult() {
        return lastResult;
    }

    /** 머리글 이름으로 열을 찾으므로 열 순서가 바뀌거나 새 열이 추가되어도 동작한다. */
    List<ChannelMessage> parse(String csv) {
        List<List<String>> rows = Csv.parse(csv);
        if (rows.isEmpty()) return List.of();
        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i).replaceAll("\\s", "");
            if (h.startsWith("시각")) h = "시각";
            col.putIfAbsent(h, i);
        }
        List<ChannelMessage> out = new ArrayList<>();
        for (List<String> r : rows.subList(1, rows.size())) {
            Integer no = toInt(get(r, col, "번호"));
            if (no == null) continue;
            out.add(new ChannelMessage(no, toInstant(get(r, col, "시각")),
                    get(r, col, "보낸이"), get(r, col, "받는이"), get(r, col, "제목"), get(r, col, "내용"),
                    toInt(get(r, col, "답장대상번호")), get(r, col, "상태")));
        }
        return out;
    }

    private static String get(List<String> row, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= row.size()) return null;
        String v = row.get(i).trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer toInt(String s) {
        if (s == null) return null;
        try {
            return Integer.valueOf(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant toInstant(String s) {
        if (s == null) return null;
        for (DateTimeFormatter f : TIME_FORMATS) {
            try {
                return LocalDateTime.parse(s, f).atZone(zone).toInstant();
            } catch (Exception ignored) {
                // 다음 형식 시도
            }
        }
        return null;
    }
}
