# 01. 백엔드 개발 및 다중 DB 배포 가이드 (FastAPI + Oracle ADB / Supabase)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-21  
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
  SITE_ADDRESS=api.yourdomain.duckdns.org
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
테스트 단계(`SITE_ADDRESS=:80`)에서 실제 운영으로 전환하기 전에 아래 3가지를 반드시 적용합니다.
1. **HTTP → HTTPS 전환**: 무료 도메인(예: DuckDNS)을 VM 공인 IP에 연결하고, `server/.env`의 `SITE_ADDRESS`를 `:80`에서 실제 도메인(`api.yourdomain.duckdns.org`)으로 변경해 Caddy 자동 TLS(HTTPS)를 활성화합니다.
2. **테스트용 비밀번호·Swagger 정리**: Docker PostgreSQL을 계속 사용할 경우 `POSTGRES_PASSWORD`를 `openssl rand -hex 16` 등 랜덤 값으로 교체하고, 운영 환경(`APP_ENV=production`)에서는 FastAPI `/docs`·`/openapi.json` 비활성화를 검토합니다.
3. **OCI Autonomous DB ACL 제한**: One-Way TLS 사용 시 OCI ADB 콘솔의 **Access Control List(ACL)**에 반드시 Compute VM의 공인 IP만 허용 등록하여 외부 IP 접근을 차단합니다.

---

## 6. 코드 수정 시 자동 반영·재시작 (GitHub Actions CI/CD)

`.github/workflows/deploy-server.yml`이 `main` 브랜치의 `server/**` 변경을 감지해 테스트(`ruff`, `pytest`) 통과 후 OCI VM에 자동 배포합니다.

1. **GitHub Repository Secrets 등록** (`Settings > Secrets and variables > Actions`):
   - `OCI_HOST`: OCI Compute VM 공인 IP
   - `OCI_USER`: VM SSH 사용자 (예: `ubuntu`)
   - `OCI_SSH_KEY`: VM 접속용 SSH 개인키 전문
   - `OCI_APP_DIR` *(선택)*: VM 내 저장소 경로 (기본값: `$HOME/refrigerator_manger`)
2. **배포 흐름**:
   - `git push origin main` → GitHub Actions `ruff` + `pytest` 검증 → SSH 접속 후 `git reset --hard origin/main` → `docker compose -f server/compose.prod.yml up -d --build --remove-orphans` → `/api/v1/health` 200 OK 확인.

---

## 변경 이력

| 날짜 | 변경 내용 | 주요 파일 |
|---|---|---|
| 2026-11-21 | OCI VM 배포 구성(`local-db` 테스트 ↔ OCI ADB 운영 전환), 운영 전환 전 보안 체크리스트 및 GitHub Actions 자동 배포 절차 추가 | `server/Dockerfile`, `server/entrypoint.sh`, `server/compose.prod.yml`, `server/Caddyfile`, `.github/workflows/deploy-server.yml`, `server/.env.example` |
