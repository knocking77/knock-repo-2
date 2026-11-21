# AI 요청 대시보드

AI에게 보낸 요청과 수행 이력을 자동으로 모아 요약하고 관리하는 개인용 대시보드입니다.

## 동작 방식

- **자동 수집**: Claude Code 대화 기록(`~/.claude/projects/<프로젝트>/<세션>.jsonl`)을 5분마다 스캔합니다. 파일 크기와 수정 시각이 바뀐 세션만 다시 읽습니다.
- **추출 항목**: 사람이 입력한 요청, 도구 호출 횟수, 수정한 파일, 토큰 사용량, 모델, Git 브랜치, 시작·종료 시각
- **직접 편집**: 세션별 즐겨찾기, 태그, 메모. 다시 수집해도 유지됩니다.
- **저장소**: H2 파일 DB(`~/.ai-dashboard/db`)
- **접속 범위**: `127.0.0.1`에만 바인딩되어 내 PC에서만 열립니다.

## 실행

JDK 17 이상이 필요합니다.

```bash
./gradlew bootRun
# 브라우저에서 http://localhost:8080
```

## 무료 클라우드 미리보기 (Render)

클라우드 서버에는 Claude Code 대화 기록이 없으므로 **데모 모드**(`SPRING_PROFILES_ACTIVE=demo`)로 실행됩니다.
데모 모드는 최근 45일치 예시 세션 48개를 자동 생성하며, 데이터는 메모리에만 저장되어 재시작하면 초기화됩니다.

1. [render.com](https://render.com)에 GitHub 계정으로 가입합니다.
2. **New → Blueprint**를 선택하고 이 저장소와 배포할 브랜치를 고릅니다.
3. 저장소 루트의 `render.yaml`이 자동으로 인식됩니다. **Apply**를 누르면 Docker 빌드 후 배포됩니다(처음 5~10분).
4. 발급된 `https://ai-request-dashboard-xxxx.onrender.com` 주소로 접속합니다.

무료 플랜은 15분 동안 접속이 없으면 잠들고, 다음 접속 때 깨어나는 데 1분 정도 걸립니다.

로컬에서 데모 모드로 실행하려면 다음 중 하나를 사용합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=demo'
# 또는
docker build -t ai-dashboard:0.0.1 . && docker run -p 8080:8080 ai-dashboard:0.0.1
```

## 설정 (`src/main/resources/application.properties`)

| 키 | 기본값 | 설명 |
|---|---|---|
| `collector.claude-code.paths` | `${user.home}/.claude/projects` | 수집 경로. 쉼표로 여러 개 지정 |
| `collector.interval-ms` | `300000` | 자동 수집 주기 (ms) |

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/stats?days=30&project=` | 요약 지표와 일별·프로젝트별·도구별 집계 |
| GET | `/api/sessions?q=&project=&days=&starred=` | 세션 목록과 검색 |
| GET | `/api/sessions/{id}` | 세션 상세 (요청 내역 포함) |
| PATCH | `/api/sessions/{id}` | `{memo, tags, starred}` 수정 |
| POST | `/api/collect` | 즉시 수집 |
| GET | `/api/projects` | 프로젝트 목록 |
| GET | `/api/info` | 버전, 데모 모드 여부 |

## 다음 단계 (예정)

- Claude API를 이용한 세션별 AI 요약과 주간 회고 리포트
- 다른 수집원 추가 (ChatGPT, claude.ai 내보내기 파일 등)
- CSV / Markdown 내보내기
