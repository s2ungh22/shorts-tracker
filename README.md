# Shorts Tracker

YouTube Shorts 조회수 속도와 재확산 징후를 추적하는 React / Spring Boot MVP.

## 필수 환경
- Java 21
- Node 20+ (Vite는 Node 20.19+ 권장)
- Docker 및 Docker Compose

## 실행
```bash
docker compose up -d
cd backend
export YOUTUBE_API_KEY=xxxxx
./gradlew bootRun
```
다른 터미널:
```bash
cd frontend
npm install
npm run dev
```
Frontend: http://localhost:5173
Backend: http://localhost:8080

선택적으로 `frontend/.env.example`을 `.env.local`로 복사합니다.
`VITE_API_BASE_URL=http://localhost:8080`이 기본값입니다. **API 키를 VITE_ 변수에 넣지 마세요.**

API 키는 백엔드의 `YOUTUBE_API_KEY` 환경변수로만 읽습니다. `.env`, `application-local.yml` 등 secret 파일은 gitignore 처리됩니다. API 키 없이 UI/API와 채널 관리를 실행할 수 있지만 실제 수집은 503으로 응답하며 스케줄러는 수집을 건너뜁니다. YouTube Data API v3를 활성화한 키가 필요합니다.

개발용 PostgreSQL: `shorts_tracker` / `shorts` / `shorts`, localhost:5432. 이 기본 암호는 로컬 개발용이며 배포 시 교체하세요. 백엔드 `DB_URL`, `DB_USER`, `DB_PASSWORD`로 변경할 수 있습니다. JPA ddl-auto는 update이며 Flyway는 아직 사용하지 않습니다.

## 사용
1. My Channel에 기본 채널 `이거괜찮네`가 생성됩니다.
2. Competitors에서 Channel ID / 이름을 등록합니다.
3. Sync 버튼으로 채널의 최신 짧은 영상 후보를 수집합니다.
4. Videos에서 11자리 Video ID를 직접 추적할 수도 있습니다.
5. Dashboard는 최근 1시간 VPH 내림차순으로 표시하고 60초마다 갱신합니다.
6. 영상 행을 클릭하면 조회수/VPH 그래프와 스냅샷 이력을 확인할 수 있습니다.

`SEED_VIDEOS=true ./gradlew bootRun`으로 `UmGZGaMMy9c`, `q3LoMSRnTpM`, `iJzJYKEs59I`를 API에서 등록할 수 있습니다. 가짜 통계는 생성하지 않습니다.

## 구조
```
backend/
  src/main/java/com/shortstracker/
    config/ controller/ service/ client/ domain/ repository/ dto/ scheduler/
  src/main/resources/application.yml
  src/test/java/com/shortstracker/
frontend/src/
  main.tsx  api.ts  style.css
docker-compose.yml
```

## 분석 / 수집 정책
- videos.list는 최대 50개씩 호출하며 기본 10분 주기로 통계를 저장합니다.
- search.list는 기본 1시간마다 채널당 1회 호출합니다. 가장 최신 추적 영상의 publishedAt 이후만 검색하고 최대 50개 결과를 처리합니다. 대량 업로드의 전체 페이지 수집은 MVP 범위 밖입니다.
- search의 short는 4분 미만 필터이므로 contentDetails에서 180초 이하로 추가 필터링합니다. YouTube API는 Shorts 여부/세로 비율을 확정하지 못하므로 일반 짧은 영상도 포함될 수 있습니다. 직접 ID 등록에는 길이 제한이 없습니다.
- `tracking.snapshot-interval-ms`, `tracking.discovery-interval-ms`와 `tracking.rules`로 주기/판정 기준을 조정합니다. `TRACKING_ENABLED=false`는 스케줄 수집을 중단합니다.
- 각 시간 창의 가장 가까운 이전 스냅샷과 실제 시간 차이를 사용합니다. 자료가 짧으면 실제 확보 구간으로 계산하므로 장시간 창은 충분한 이력이 쌓인 후 해석하세요. 평균 VPH는 마지막 수집 조회수를 업로드부터 마지막 수집까지의 시간으로 나눕니다.
- 이전 1시간 VPH가 0이면 가속도는 0입니다. 조회수 정정으로 감소한 구간은 VPH 0으로 처리합니다.
- NEW → 재확산/2차 확산 → SURGING → PLATEAU → SLOWING → RISING → NORMAL 우선순위로 판정합니다. 2차 확산은 이전 구간의 의미 있는 속도 하락, 6시간 이상 경과, 최근 VPH 최소 100 및 이전 시간 대비 2배를 요구합니다. 같은 조건에서 12시간 이상이면 REEXPANDING입니다.
- 수집 오류는 배치/채널별로 격리하며 키나 upstream 오류 본문은 로그/응답에 기록하지 않습니다.

## API
GET `/api/dashboard/trending`, `/api/videos`, `/api/videos/{videoId}`, `/api/videos/{videoId}/history`, `/api/channels`

POST `/api/channels`: `{"youtubeChannelId":"UC...","channelName":"name","isMyChannel":false}`
POST `/api/videos/track`: `{"videoId":"UmGZGaMMy9c"}`
POST `/api/videos/{videoId}/collect`, `/api/channels/{channelId}/sync`: body `{}`

## 검증
```bash
cd backend
./gradlew clean build
cd ../frontend
npm ci
npm run build
```
분석 테스트와 H2 기반 실제 JPA/REST smoke 테스트를 포함합니다. PostgreSQL 운영 호환성은 Compose 환경에서도 별도 확인하세요.

## TODO
- 인증, 접근 제어, 운영 HTTPS 및 secret 관리
- Flyway 마이그레이션, 스냅샷 보관 정책 및 큰 이력의 쿼리 최적화
- 여러 백엔드 인스턴스 간 스케줄러 잠금 및 API quota 지표/재시도 정책
- 검색 페이지네이션, Shorts 여부 보완, 재확산 규칙 실데이터 튜닝
- 브라우저 E2E 테스트 및 실제 YouTube 키를 사용한 수집 검증
