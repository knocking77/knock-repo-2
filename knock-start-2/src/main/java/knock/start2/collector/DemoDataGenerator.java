package knock.start2.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * demo 프로필 전용. 클라우드 미리보기 서버에는 Claude Code 기록이 없으므로
 * 실제와 같은 형식의 가짜 대화 기록(jsonl)을 만들어 두고 일반 수집기로 읽어 들인다.
 * 날짜는 실행 시점 기준으로 생성되어 항상 최근 데이터처럼 보인다.
 */
@Component
@Profile("demo")
@Order(0)
public class DemoDataGenerator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataGenerator.class);

    private static final String[] PROJECTS = {"shop-api", "blog-front", "data-pipeline", "knock-start-2", "infra-scripts"};
    private static final String[][] PROMPTS = {
            {"주문 API에 페이지네이션 추가해줘", "응답 포맷을 기존과 맞춰줘", "테스트 코드도 작성해줘"},
            {"로그인 페이지 디자인 개선해줘", "모바일에서 버튼이 깨지는 문제 수정해줘", "다크 모드도 지원해줘"},
            {"매일 새벽 CSV 집계 배치 만들어줘", "실패하면 슬랙 알림 보내도록 해줘"},
            {"AI 요청 대시보드 작업 계획 세워줘", "자동 수집 기능 구현해줘", "클라우드에 미리보기 배포 준비해줘"},
            {"도커 이미지 용량 줄여줘", "GitHub Actions CI 설정해줘", "빌드 캐시 적용해줘"},
            {"이 에러 로그 원인 분석해줘: NullPointerException at OrderService", "재발 방지 코드 추가해줘"},
            {"README 정리해줘", "API 문서 표 형태로 추가해줘"},
            {"쿼리가 느린데 인덱스 추천해줘", "실행 계획 비교해줘"},
            {"리팩터링: 서비스 클래스 너무 커서 분리해줘", "기존 테스트 깨지는지 확인해줘"},
            {"보안 점검해줘", "취약한 의존성 버전 올려줘"},
    };
    private static final String[] TOOLS = {"Bash", "Read", "Edit", "Grep", "Write", "Glob", "TodoWrite", "WebSearch"};
    private static final int[] TOOL_WEIGHTS = {30, 25, 18, 10, 6, 5, 4, 2};
    private static final String[] FILES = {"src/main/java/App.java", "src/service/OrderService.java", "README.md",
            "Dockerfile", "src/pages/Login.tsx", "pipeline/daily_job.py", ".github/workflows/ci.yml", "build.gradle"};
    private static final String[] MODELS = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5"};

    private final ClaudeCodeCollector collector;
    private final Path dir;
    private final ObjectMapper mapper = new ObjectMapper();

    public DemoDataGenerator(ClaudeCodeCollector collector, @Value("${collector.claude-code.paths}") String path) {
        this.collector = collector;
        this.dir = Paths.get(path.split(",")[0].trim());
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Random rnd = new Random(42);
        Instant now = Instant.now();
        int count = 0;
        for (int i = 0; i < 48; i++) {
            // 최근일수록 세션이 많도록 분포
            double r = rnd.nextDouble();
            long minutesAgo = (long) (r * r * 45 * 24 * 60) + 30;
            String project = PROJECTS[rnd.nextInt(PROJECTS.length)];
            writeSession(rnd, project, now.minus(minutesAgo, ChronoUnit.MINUTES));
            count++;
        }
        CollectResult res = collector.collect();
        log.info("데모 데이터 생성: 세션 {}개, 반영 {}개", count, res.imported());
    }

    private void writeSession(Random rnd, String project, Instant start) throws IOException {
        String sessionId = new UUID(rnd.nextLong(), rnd.nextLong()).toString();
        String cwd = "/home/demo/" + project;
        String branch = rnd.nextInt(3) == 0 ? "main" : "feature/" + (100 + rnd.nextInt(900));
        String model = MODELS[rnd.nextInt(10) < 6 ? 0 : rnd.nextInt(10) < 8 ? 1 : 2];
        String[] script = PROMPTS[rnd.nextInt(PROMPTS.length)];
        int promptCount = 1 + rnd.nextInt(script.length);

        List<String> lines = new ArrayList<>();
        Instant t = start;
        int msg = 0, tool = 0;
        for (int p = 0; p < promptCount; p++) {
            ObjectNode u = base("user", sessionId, cwd, branch, t);
            u.putObject("origin").put("kind", "human");
            u.putObject("message").put("role", "user").put("content", script[p]);
            lines.add(mapper.writeValueAsString(u));

            int turns = 1 + rnd.nextInt(5);
            for (int k = 0; k < turns; k++) {
                t = t.plusSeconds(20 + rnd.nextInt(180));
                ObjectNode a = base("assistant", sessionId, cwd, branch, t);
                ObjectNode m = a.putObject("message");
                m.put("id", "msg_" + sessionId.substring(0, 8) + "_" + (msg++));
                m.put("model", model);
                m.putObject("usage")
                        .put("input_tokens", 200 + rnd.nextInt(3000))
                        .put("output_tokens", 150 + rnd.nextInt(2500))
                        .put("cache_read_input_tokens", 5000 + rnd.nextInt(40000));
                ArrayNode content = m.putArray("content");
                int calls = rnd.nextInt(4);
                for (int c = 0; c < calls; c++) {
                    String name = pickTool(rnd);
                    ObjectNode tu = content.addObject().put("type", "tool_use").put("id", "tu_" + sessionId.substring(0, 8) + "_" + (tool++)).put("name", name);
                    ObjectNode in = tu.putObject("input");
                    if (name.equals("Edit") || name.equals("Write")) in.put("file_path", cwd + "/" + FILES[rnd.nextInt(FILES.length)]);
                }
                content.addObject().put("type", "text").put("text", "작업을 진행했습니다.");
                lines.add(mapper.writeValueAsString(a));
            }
            t = t.plusSeconds(60 + rnd.nextInt(600));
        }

        Path projectDir = dir.resolve("-home-demo-" + project);
        Files.createDirectories(projectDir);
        Files.write(projectDir.resolve(sessionId + ".jsonl"), lines, StandardCharsets.UTF_8);
    }

    private ObjectNode base(String type, String sessionId, String cwd, String branch, Instant ts) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", type).put("sessionId", sessionId).put("cwd", cwd).put("gitBranch", branch)
                .put("isSidechain", false).put("timestamp", ts.toString());
        return n;
    }

    private static String pickTool(Random rnd) {
        int total = 0;
        for (int w : TOOL_WEIGHTS) total += w;
        int x = rnd.nextInt(total);
        for (int i = 0; i < TOOLS.length; i++) {
            x -= TOOL_WEIGHTS[i];
            if (x < 0) return TOOLS[i];
        }
        return TOOLS[0];
    }
}
