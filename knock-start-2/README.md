# AI 요청 대시보드

AI에게 보낸 요청과 수행 이력을 자동으로 모아 요약하고 관리하는 개인용 대시보드입니다.

## 동작 방식

- **자동 수집**: Claude Code 대화 기록(`~/.claude/projects/<프로젝트>/<세션>.jsonl`)을 5분마다 스캔합니다. 파일 크기와 수정 시각이 바뀐 세션만 다시 읽습니다.
- **추출 항목**: 사람이 입력한 요청, 도구 호출 횟수, 수정한 파일, 토큰 사용량, 모델, Git 브랜치, 시작·종료 시각
- **AI 공통 채널 수집**: 여러 AI(캐드, 캐대, zu, cbot, meti 등)가 메시지를 주고받는 Google 시트 "AI 공통 채널"의 메시지 탭을 5분마다 읽어, 보낸 이별 메시지 수, 많이 오간 경로, 미완료 메시지를 보여줍니다.
- **직접 편집**: 세션별 즐겨찾기, 태그, 메모. 다시 수집해도 유지됩니다.
- **저장소**: H2 파일 DB(`~/.ai-dashboard/db`)
- **접속 범위**: `127.0.0.1`에만 바인딩되어 내 PC에서만 열립니다.

## 실행

JDK 17 이상이 필요합니다.

```bash
./gradlew bootRun
# 브라우저에서 http://localhost:8080
```

## AI 공통 채널(Google 시트) 연결

1. 시트의 **공유 → 일반 액세스**를 **"링크가 있는 모든 사용자: 뷰어"**로 바꿉니다. 앱은 로그인 없이 CSV 내보내기 주소로 읽기 때문에 이 설정이 필요합니다. 링크를 아는 사람은 누구나 시트를 볼 수 있게 되므로, 규칙대로 비밀번호·API 키·회사 문서 내용은 시트에 쓰지 않습니다.
2. 환경 변수로 시트 ID를 지정하고 실행합니다. 시트 ID는 주소의 `/d/`와 `/edit` 사이 문자열입니다.

```bash
export CHANNEL_SHEET_ID=<시트 ID>
# 메시지 탭이 첫 번째 탭이 아니면 주소의 #gid= 값도 지정
# export CHANNEL_SHEET_GID=0
./gradlew bootRun
```

열은 머리글 이름(번호, 시각 (KST), 보낸 이, 받는 이, 제목, 내용, 답장 대상 번호, 상태)으로 찾으므로, 열 순서가 바뀌거나 새 열이 추가되어도 그대로 동작합니다. 시트가 원본이라 대시보드에서는 읽기만 합니다.

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
| `collector.sheet.id` | `${CHANNEL_SHEET_ID}` | AI 공통 채널 시트 ID. 비우면 사용 안 함 |
| `collector.sheet.gid` | `${CHANNEL_SHEET_GID:0}` | 메시지 탭의 gid |

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/stats?days=30&project=` | 요약 지표와 일별·프로젝트별·도구별 집계 |
| GET | `/api/sessions?q=&project=&days=&starred=` | 세션 목록과 검색 |
| GET | `/api/sessions/{id}` | 세션 상세 (요청 내역 포함) |
| PATCH | `/api/sessions/{id}` | `{memo, tags, starred}` 수정 |
| POST | `/api/collect` | 모든 수집원 즉시 수집 |
| GET | `/api/channel/stats?days=` | 공통 채널 메시지 집계 |
| GET | `/api/channel/messages?participant=&openOnly=` | 공통 채널 메시지 목록 |
| GET | `/api/projects` | 프로젝트 목록 |
| GET | `/api/info` | 버전, 데모 모드 여부 |

## 다음 단계 (예정)

- Claude API를 이용한 세션별 AI 요약과 주간 회고 리포트
- 다른 수집원 추가 (ChatGPT, claude.ai 내보내기 파일 등)
- 공통 채널: 사람 중계 횟수, 응답 시간(요청→결과) 측정
- CSV / Markdown 내보내기
