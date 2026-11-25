# 01. 백엔드 개발 및 다중 DB 배포 가이드 (FastAPI + Oracle ADB / Supabase)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-25  
> **관련 문서**: [OCI Always Free 및 무료 DB 인프라 설계](../02-architecture/02-oci-always-free-infra.md), [DB ERD 및 호환성 설계](../02-architecture/03-database-erd.md)

---

## 1. 권장 백엔드 기술 스택 및 다중 DB 드라이버

| 항목 | 라이브러리 / 도구 | 비고 |
|---|---|---|
| Python | 3.12+ | ARM64(`A1.Flex`) 및 x86_64 호환 |
| Web Framework | `fastapi`, `uvicorn[standard]` | 비동기 REST API |
| ORM & Migration | `sqlalchemy>=2.0`, `alembic` | 단일 모델/마이그레이션으로 다중 DB 지원 |
| **Oracle 드라이버** | `oracledb` (`python-oracledb`) | **Thin 모드** 사용 (Instant Client 불필요) |
| **PostgreSQL / Supabase 드라이버** | `psycopg[binary]` (또는 `asyncpg`) | Supabase PostgreSQL 및 로컬 Postgres 연결 |
| Auth / Security | `PyJWT`, `httpx`, `cryptography` | Google/Kakao 토큰 검증 및 자체 JWT 발급 |
| Testing | `pytest`, `pytest-asyncio`, `httpx` | 단위/통합 테스트 (SQLite 인메모리 또는 Docker Postgres) |

---

## 2. 다중 DB 연결 추상화 구조 (`DB_PROVIDER` 기반)

백엔드는 환경 변수(`DB_PROVIDER`, `DATABASE_URL`)만 변경하면 코드 수정 없이 **Oracle Autonomous DB**, **Supabase PostgreSQL**, **로컬 PostgreSQL/SQLite** 간 전환이 가능하도록 DB 엔진 팩토리를 구성합니다.

### 2.1 Option A: Oracle Autonomous Database 연결
- **URL 형식**: `oracle+oracledb://<user>:<password>@<dsn>`
- **One-Way TLS (권장)**: OCI ADB 콘솔에서 mTLS 비활성화 + ACL에 OCI VM IP 등록 → Wallet 없이 DSN만으로 접속.
- **mTLS (Wallet 사용 시)**: `Wallet.zip`을 안전 경로(`/etc/oci-wallet`, 권한 `600`)에 두고 `connect_args`에 `config_dir`, `wallet_location`, `wallet_password` 주입.
- **헬스체크 쿼리**: `SELECT 1 FROM DUAL` (엔진 팩토리에서 `DB_PROVIDER=oracle`일 때 자동 선택).

### 2.2 Option B: Supabase (PostgreSQL) 연결
- **URL 형식**: `postgresql+psycopg://<user>:<password>@<supabase-pooler-host>:5432/postgres?sslmode=require`
- **Supavisor Pooler 권장**: OCI VM이 IPv4 전용일 수 있으므로 Supabase 대시보드 > **Connect**에서 IPv4를 지원하는 **Session Pooler (`5432`)** 또는 **Transaction Pooler (`6543`)** 문자열을 사용합니다.
- Transaction Pooler(`6543`) 사용 시 `connect_args={"prepare_threshold": None}`을 설정하여 Prepared Statement 충돌을 방지합니다.
- **헬스체크 쿼리**: `SELECT 1` (엔진 팩토리에서 `DB_PROVIDER=supabase` / `postgres`일 때 자동 선택).

> [!CAUTION]
> `Wallet.zip`, `.env`, Supabase DB 비밀번호는 절대 Git 저장소에 커밋하지 않도록 `.gitignore`에 반드시 등록합니다.

---

## 3. 환경 변수 예시 (`.env.example`)

```env
APP_ENV=production

# 사용할 DB 프로바이더 선택: oracle | supabase | postgres | sqlite
DB_PROVIDER=supabase
DATABASE_URL=postgresql+psycopg://postgres.<project-ref>:<password>@aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require

# [Option A: DB_PROVIDER=oracle 사용 시 예시]
# DB_PROVIDER=oracle
# DATABASE_URL=oracle+oracledb://ADMIN:<password>@<adb-dsn>
# ORACLE_WALLET_DIR=
# ORACLE_WALLET_PASSWORD=

# DB 커넥션 풀 설정 (무료 티어 보호용 보수적 설정)
DB_POOL_SIZE=5
DB_MAX_OVERFLOW=5

JWT_SECRET_KEY=<random-64-byte-hex>
JWT_ACCESS_EXPIRE_MINUTES=30
JWT_REFRESH_EXPIRE_DAYS=30

GOOGLE_CLIENT_ID=<android-web-client-id>
KAKAO_REST_API_KEY=<kakao-app-key>
```

---

## 4. Alembic 마이그레이션 다중 DB 호환 체크리스트

- 마이그레이션 스크립트에 특정 DB 전용 Raw SQL(`SERIAL`, `VARCHAR2`, `JSONB` 등)을 직접 작성하지 않고 SQLAlchemy 타입(`sa.BigInteger`, `sa.Identity()`, `sa.String`, `sa.Numeric`, `sa.DateTime(timezone=True)`)만 사용합니다.
- DB 전환 시 새 DB에 `alembic upgrade head`를 실행하면 동일한 스키마가 즉시 생성됩니다.

---

## 5. OCI Compute VM 배포 구성 (무과금)

`server/Dockerfile`, `server/compose.prod.yml`, `server/Caddyfile`을 통해 **테스트(VM 내 Docker PostgreSQL)**와 **실제 운영(OCI Autonomous DB)**을 `.env` 설정만으로 전환할 수 있습니다.

### 5.1 VM 초기 준비 (1회)
1. **방화벽 및 포트 개방**:
   - OCI 콘솔 VCN Security List / NSG에서 인바운드 `TCP 80, 443` 허용 (`SSH 22`는 본인 IP만).
   - Ubuntu VM 내부 `iptables`에서 `80`, `443` 허용 후 영구 저장:
     ```bash
     sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
     sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
     sudo netfilter-persistent save
     ```
2. **Docker 및 Git 설치** 후 저장소 클론:
   ```bash
   git clone <repo-url> ~/refrigerator_manger
   cd ~/refrigerator_manger/server
   cp .env.example .env
   ```

### 5.2 단계별 DB 전환 및 실행
- **1단계 (테스트: VM 내 Docker PostgreSQL)**:
  `server/.env`에 아래 항목을 설정하면 `postgres` 컨테이너가 함께 기동됩니다.
  ```env
  APP_ENV=production
  COMPOSE_PROFILES=local-db
  DB_PROVIDER=postgres
  DATABASE_URL=postgresql+psycopg://fridge_user:fridge_password@postgres:5432/fridge
  POSTGRES_USER=fridge_user
  POSTGRES_PASSWORD=fridge_password
  POSTGRES_DB=fridge
  SITE_ADDRESS=:80
  ```
- **2단계 (실제 배포: OCI Autonomous DB)**:
  OCI 콘솔에서 ATP Always Free 생성(Auto Scaling OFF, One-Way TLS + VM 공인 IP ACL 허용) 후 `server/.env`에서 `COMPOSE_PROFILES`를 비우고 Oracle 연결 정보로 교체합니다.
  ```env
  APP_ENV=production
  COMPOSE_PROFILES=
  DB_PROVIDER=oracle
  DATABASE_URL=oracle+oracledb://ADMIN:<비밀번호>@<ADB_DSN>
  SITE_ADDRESS=myfridges.duckdns.org
  ```
- **기동 명령**:
  ```bash
  docker compose -f server/compose.prod.yml up -d --build --remove-orphans
  ```
  컨테이너 시작 시 `entrypoint.sh`가 `alembic upgrade head`를 자동 실행한 뒤 `uvicorn`을 기동합니다.

### 5.3 무료 DB 자동 중지 방지 Cron
- OCI ADB와 Supabase Free 모두 7일 미사용 시 중지되므로, VM `crontab -e`에 매일 1회 헬스체크를 등록합니다.
  ```cron
  0 4 * * * curl -fsS http://127.0.0.1:8000/api/v1/health > /dev/null
  ```

### 5.4 운영 전환 전 보안 체크리스트
테스트 단계(`SITE_ADDRESS=:80`)에서 실제 운영으로 전환하기 전에 아래 항목을 반드시 적용합니다.
1. **HTTP → HTTPS 전환**: 무료 도메인(예: DuckDNS)을 VM 공인 IP에 연결하고, `server/.env`의 `SITE_ADDRESS`를 `:80`에서 실제 도메인(`myfridges.duckdns.org`)으로 변경해 Caddy 자동 TLS(HTTPS)를 활성화합니다.
2. **운영 환경(`APP_ENV=production`) 설정 Fail-Fast 강제 검증**:
   - `APP_ENV != "local"`일 때 기본 `JWT_SECRET_KEY`(`"dev-insecure-secret-change-in-production"`) 또는 32바이트 미만의 약한 시크릿 키가 설정된 경우 서버 기동을 즉시 차단합니다.
   - `APP_ENV == "production"`에서는 `ALLOW_DEV_LOGIN=false`를 강제하여 비밀번호 없는 체험 로그인(`/auth/dev-login`)이 운영 환경에 노출되지 않도록 차단합니다.
3. **내부 예외 정보 및 API 문서 노출 차단**:
   - `GET /api/v1/health` 실패(`503`) 시 DB 호스트·계정·쿼리 정보가 포함될 수 있는 `detail: str(exc)` 및 `db_provider`를 응답 본문에서 제거하고 서버 로그에만 기록합니다.
   - `APP_ENV != "local"`에서는 FastAPI의 `/docs`, `/redoc`, `/api/v1/openapi.json` 엔드포인트를 비활성화합니다.
4. **리버스 프록시(Caddy) 보안 헤더 및 Uvicorn 신뢰 프록시 제한**:
   - `entrypoint.sh`의 Uvicorn `--forwarded-allow-ips`를 `'*'` 대신 Docker 내부 브리지 대역(또는 신뢰된 프록시 IP)으로 제한하여 `X-Forwarded-For` 위조를 방지합니다.
   - `Caddyfile`에 기본 보안 헤더(`Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`)와 요청 본문 크기 상한(`request_body { max_size 2MB }`)을 적용합니다.
5. **OCI Autonomous DB ACL 제한**: One-Way TLS 사용 시 OCI ADB 콘솔의 **Access Control List(ACL)**에 반드시 Compute VM의 공인 IP만 허용 등록하여 외부 IP 접근을 차단합니다.
6. **Android Release APK 정적 배포 경로 (`APK_DOWNLOAD_PATH`)**:
   - `server/compose.prod.yml`에서 `./releases:/srv/releases:ro`를 Caddy에 마운트하고 `server/Caddyfile`의 `handle_path /releases/{$APK_DOWNLOAD_PATH:apk}/*` 블록으로 서명된 APK(`*.apk`), `latest.json`, `index.html`을 서빙합니다(디렉토리 리스팅 비활성화, `X-Robots-Tag: noindex, nofollow`, `Cache-Control: no-store`).
   - GitHub Actions(`.github/workflows/deploy-android-apk.yml`)가 `server/releases/`에 APK를 배포하며, `server/releases/`는 `.gitignore`로 제외되어 `deploy-server.yml`의 `git reset --hard origin/main`에 영향받지 않습니다.
   - `server/.env`에서 `APK_DOWNLOAD_PATH`를 추측하기 어려운 문자열로 지정해 가족 외 접근을 제한하는 것을 권장합니다(미설정 시 기본값 `apk`).

---

## 6. 코드 수정 시 자동 반영·재시작 (GitHub Actions CI/CD)

`.github/workflows/deploy-server.yml`이 `main` 브랜치의 `server/**` 변경을 감지해 테스트(`ruff`, `pytest`) 통과 후 OCI VM에 자동 배포합니다.

1. **GitHub Repository Secrets 등록** (`Settings > Secrets and variables > Actions`):
   - `OCI_HOST`: OCI Compute VM 공인 IP
   - `OCI_USER`: VM SSH 사용자 (예: `ubuntu`)
   - `OCI_SSH_KEY`: VM 접속용 SSH 개인키 전문
   - `OCI_APP_DIR` *(선택)*: VM 내 저장소 경로 (기본값: `$HOME/refrigerator_manger`)
2. **배포 흐름**:
   - `git push origin main` → GitHub Actions `uv sync --frozen --extra dev`로 개발 의존성(`ruff`, `pytest`) 설치 후 `uv run --extra dev ruff check .` + `uv run --extra dev pytest -q` 검증 → SSH 접속 후 `git reset --hard origin/main` → `docker compose -f server/compose.prod.yml up -d --build --remove-orphans` → `/api/v1/health` 200 OK 확인.
   - **주의 (`uv` optional-dependencies)**: `server/pyproject.toml`의 `ruff`, `pytest`가 `[project.optional-dependencies] dev`에 정의되어 있으므로, `uv sync` 및 `uv run` 실행 시 `--extra dev` 옵션을 반드시 함께 전달해야 가상환경에 `ruff`/`pytest` 바이너리가 설치·유지됩니다.

---

## 변경 이력

| 날짜 | 변경 내용 | 주요 파일 |
|---|---|---|
| 2026-11-21 | OCI VM 배포 구성(`local-db` 테스트 ↔ OCI ADB 운영 전환), 운영 전환 전 보안 체크리스트 및 GitHub Actions 자동 배포 절차 추가 | `server/Dockerfile`, `server/entrypoint.sh`, `server/compose.prod.yml`, `server/Caddyfile`, `.github/workflows/deploy-server.yml`, `server/.env.example` |
| 2026-11-21 | 운영 전환 전 보안 체크리스트 강화 — 운영 환경 `JWT_SECRET_KEY`·`ALLOW_DEV_LOGIN` Fail-Fast 검증, `/health` 내부 예외 미노출, Uvicorn 신뢰 프록시 IP 제한 및 Caddy 보안 헤더/요청 크기 상한 추가 | `docs/04-guides/01-backend-setup.md` |
| 2026-11-21 | GitHub Actions CI/CD에서 `uv` 실행 시 `[project.optional-dependencies] dev`(`ruff`, `pytest`) 설치·실행을 위한 `--extra dev` 옵션 명세 추가 | `docs/04-guides/01-backend-setup.md` |
| 2026-11-22 | 신규 환경변수 RATE_LIMIT_ENABLED, RATE_LIMIT_AUTH_PER_MINUTE, RATE_LIMIT_JOIN_PER_MINUTE, FORWARDED_ALLOW_IPS(기본 172.16.0.0/12) 추가, Caddy 보안 헤더·1MB 요청 상한 적용. 운영 배포 시 ALLOW_DEV_LOGIN=false + 32자 이상 JWT_SECRET_KEY 미설정이면 기동 차단 | server/app/core/config.py, server/app/core/rate_limit.py, server/app/main.py, server/app/api/routers/auth.py, server/app/api/routers/households.py, server/Caddyfile, server/entrypoint.sh, server/tests/test_security_hardening.py |
| 2026-11-22 | 운영 도메인 `myfridges.duckdns.org` 확정 — `SITE_ADDRESS` 예시 반영 | `server/.env.example`, `docs/04-guides/01-backend-setup.md` |
| 2026-11-25 | Caddy 정적 APK 호스팅(`/releases/{$APK_DOWNLOAD_PATH:apk}/*`, `./releases:/srv/releases:ro`) 및 `APK_DOWNLOAD_PATH` 환경변수 설정 반영 | `server/compose.prod.yml`, `server/Caddyfile`, `server/.env.example`, `.gitignore`, `docs/04-guides/01-backend-setup.md` |
