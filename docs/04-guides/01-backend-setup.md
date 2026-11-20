# 01. 백엔드 개발 및 OCI 배포 가이드 (FastAPI + Oracle ADB)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-19  
> **관련 문서**: [OCI Always Free 인프라 설계](../02-architecture/02-oci-always-free-infra.md), [DB ERD](../02-architecture/03-database-erd.md)

---

## 1. 권장 백엔드 기술 스택 및 패키지

| 항목 | 라이브러리 / 도구 | 비고 |
|---|---|---|
| Python | 3.12+ | ARM64(`A1.Flex`) 및 x86_64 호환 |
| Web Framework | `fastapi`, `uvicorn[standard]` | 비동기 REST API |
| ORM & Migration | `sqlalchemy>=2.0`, `alembic` | Oracle 방언(`oracle+oracledb`) 사용 |
| Oracle Driver | `oracledb` (`python-oracledb`) | **Thin 모드** 기본 사용 (별도 Oracle Instant Client 설치 불필요) |
| Auth / Security | `PyJWT`, `httpx`, `cryptography` | Google/Kakao 토큰 검증 및 자체 JWT 발급 |
| Testing | `pytest`, `pytest-asyncio`, `httpx` | 단위/통합 테스트 |

---

## 2. Oracle Autonomous Database 연결 방식

`python-oracledb`의 **Thin 모드**를 사용하면 C 라이브러리(Instant Client) 없이 순수 Python만으로 OCI ARM VM과 로컬 PC 모두에서 간편하게 접속할 수 있습니다.

### 방식 A: TLS (One-Way TLS, Wallet 불필요 — 권장)
1. OCI 콘솔 > Autonomous Database > **Network** > `Mutual TLS (mTLS) authentication`을 비활성화하고 **Access Control List(ACL)**에 개발 PC 및 OCI Compute VM IP만 등록합니다.
2. DB 연결 문자열(DSN)과 비밀번호만으로 즉시 접속 가능합니다.

### 방식 B: mTLS (Wallet 사용)
1. OCI 콘솔에서 `Wallet.zip`을 다운로드하여 서버 안전 경로(예: `/etc/oci-wallet`, 권한 `600`)에 압축 해제합니다.
2. SQLAlchemy `create_engine` 생성 시 `connect_args={"config_dir": ..., "wallet_location": ..., "wallet_password": ...}`를 전달합니다.

> [!CAUTION]
> `Wallet.zip` 파일과 `.env` 파일은 절대 Git 저장소에 커밋하지 않도록 `.gitignore`에 반드시 등록합니다.

---

## 3. 환경 변수 예시 (`.env.example`)

```env
APP_ENV=production
ORACLE_USER=ADMIN
ORACLE_PASSWORD=<strong-password>
ORACLE_DSN=<adb_high_or_tp_connection_string>
# mTLS 사용 시에만 설정
ORACLE_WALLET_DIR=
ORACLE_WALLET_PASSWORD=

JWT_SECRET_KEY=<random-64-byte-hex>
JWT_ACCESS_EXPIRE_MINUTES=30
JWT_REFRESH_EXPIRE_DAYS=30

GOOGLE_CLIENT_ID=<android-web-client-id>
KAKAO_REST_API_KEY=<kakao-app-key>
```

---

## 4. OCI Compute VM 배포 구성 (무과금)

1. **Reverse Proxy (Caddy)**:
   ```caddyfile
   api.yourdomain.duckdns.org {
       reverse_proxy 127.0.0.1:8000
   }
   ```
   - 포트 80/443만 외부에 개방하고 FastAPI는 `127.0.0.1:8000`에 바인딩합니다.
2. **프로세스 관리**: Docker Compose 또는 `systemd` 서비스로 등록하여 VM 재부팅 시 자동 기동되도록 구성합니다.
3. **ADB 자동 중지 방지 Cron**:
   - 매일 새벽 1회 `curl -fsS http://127.0.0.1:8000/api/v1/health > /dev/null` 실행하여 7일 미사용 자동 중지(Auto-Stop)를 방지합니다.
