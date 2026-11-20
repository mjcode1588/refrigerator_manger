# 01. 백엔드 개발 및 다중 DB 배포 가이드 (FastAPI + Oracle ADB / Supabase)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-19  
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

1. **Reverse Proxy (Caddy)**:
   ```caddyfile
   api.yourdomain.duckdns.org {
       reverse_proxy 127.0.0.1:8000
   }
   ```
   - 포트 80/443만 외부 개방, FastAPI는 `127.0.0.1:8000` 바인딩.
2. **프로세스 관리**: Docker Compose 또는 `systemd` 서비스 등록으로 VM 재부팅 시 자동 기동.
3. **무료 DB 자동 중지/일시정지 방지 Cron**:
   - OCI ADB와 Supabase Free 모두 7일 미사용 시 중지되므로, 매일 1회 `curl -fsS http://127.0.0.1:8000/api/v1/health > /dev/null`을 `cron`으로 실행합니다.
