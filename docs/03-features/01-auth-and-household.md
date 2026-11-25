# 01. 인증 및 가족/동거인 공동 관리 명세

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-25  
> **관련 문서**: [DB ERD](../02-architecture/03-database-erd.md), [REST API 명세](../02-architecture/04-api-spec.md)

---

## 1. 가족 전용 자체 인증 (아이디·비밀번호 + 등록코드) 및 JWT

### 1.1 지원 인증 방식 및 보안 정책
- **가족 전용 자체 인증**: 외부 소셜 로그인(Kakao/Google)을 사용하지 않고, 부부 2인 전용 백엔드 자체 인증(`POST /api/v1/auth/register`, `POST /api/v1/auth/login`)만 지원합니다. (로컬 개발·테스트용 체험 로그인 `/auth/dev-login`은 `ALLOW_DEV_LOGIN=true`일 때만 동작하며, 운영 환경에서는 기동 시 차단됩니다.)
- **가족 등록코드 가입 (`POST /api/v1/auth/register`)**:
  - 입력 필드: `username`(영문·숫자·`_`·`-`, 3~32자, 소문자 정규화 저장), `password`(8~128자, 공백만 입력 불가), `nickname`(1~100자), `registration_code`(서버 `.env`의 `FAMILY_REGISTRATION_CODE`와 상수시간 비교 `hmac.compare_digest`).
  - `FAMILY_REGISTRATION_CODE`가 비어 있으면 신규 가입을 비활성화(`403 REGISTRATION_DISABLED`)합니다.
  - 등록된 비밀번호 계정(`users.username IS NOT NULL`) 수가 `MAX_REGISTERED_USERS`(기본 `2`명)에 도달하면 이후 모든 신규 가입 요청을 자동 차단(`403 REGISTRATION_CLOSED`)하여 외부 제3자의 가입을 원천 봉쇄합니다.
  - 동일 `username` 중복 가입 시 `409 USERNAME_ALREADY_EXISTS`를 반환합니다.
- **아이디·비밀번호 로그인 (`POST /api/v1/auth/login`)**:
  - `hashlib.scrypt`(N=16384, r=8, p=1, 16B 솔트, 32B 다이제스트)로 비밀번호를 해싱·검증하며, `hmac.compare_digest`로 상수시간 비교를 수행합니다.
  - 존재하지 않는 아이디로 로그인 시도 시에도 더미 scrypt 검증을 수행하여 계정 존재 유무를 응답 시간으로 유추하는 타이밍 공격을 방지합니다.
  - **계정 잠금 (Brute-Force 방어)**: 연속 `AUTH_MAX_FAILED_ATTEMPTS`(기본 `5`회) 로그인 실패 시 해당 계정을 `AUTH_LOCKOUT_MINUTES`(기본 `15`분) 동안 잠그고 `429 ACCOUNT_LOCKED`를 반환합니다. 로그인 성공 시 실패 카운터(`failed_login_attempts`)와 잠금 시각(`locked_until`)을 즉시 초기화합니다.
- **최초 등록 후 가구 참여**: 첫 번째 가족 구성원이 로그인 후 **새 우리집 만들기**로 가구를 생성하고 초대 코드를 발급하면, 두 번째 구성원이 로그인 후 **초대 코드로 참여하기**로 합류합니다.

### 1.1.2 로그인 진행 시 백엔드 서버 연결 확인 (`/health`)
- 로그인 화면에 서버 내부 상태(`db_provider` 등)를 직접 노출하지 않습니다.
- 사용자가 로그인 또는 가족 등록을 진행하면 로딩 스피너(`"서버에 연결 중입니다..."`)를 표시하고 버튼을 비활성화(중복 클릭 방지)한 뒤 `GET /api/v1/health`로 백엔드 연결 가능 여부를 먼저 확인합니다.
  - **짧은 타임아웃**: `/health` 확인 요청은 **4초 타임아웃**을 적용해 서버 미응답 시 빠르게 안내합니다.
  - **점검 모드 및 최소 앱 버전 확인**: `/health` 응답의 `maintenance`, `maintenance_message`, `min_android_version_code`를 확인하여 서버 점검 중이거나 필수 업데이트가 필요한 경우 로그인 진입을 차단하고 안내 메시지를 표시합니다.
- 서버가 정상(`healthy`) 응답하면 백엔드 인증 및 가구 목록 조회를 완료한 뒤 홈 화면으로 진입하며, 연결 실패 시 내부 상세 정보 대신 사용자 친화적 안내(`"서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."`)를 표시합니다.
- **재방문 사용자 오프라인 우선 진입**: 이미 토큰과 가구 ID가 저장된 사용자는 앱 실행 시 로컬 Room DB 데이터를 즉시 홈 화면에 표시하고, 백그라운드에서 조용히 서버 동기화를 수행합니다(네트워크 실패 시 기존 로컬 데이터 유지).

### 1.2 토큰 및 세션 보안 정책
| 항목 | 정책 |
|---|---|
| **Access Token** | JWT (HS256 또는 RS256), 유효기간 **30분**, Payload에 `sub`(user_id), `exp`, `iat` 포함 |
| **Refresh Token** | 암호학적 난수 문자열, 유효기간 **30일**, DB `REFRESH_TOKENS`에 SHA-256 해시 저장 |
| **Token Rotation & 탈취 감지** | `/auth/refresh` 호출 시 원자적으로 기존 Refresh Token을 폐기(`revoked_at`)한 뒤 새 토큰 쌍 발급. **이미 폐기된 Refresh Token이 재사용될 경우 토큰 탈취로 간주하여 해당 사용자의 모든 활성 Refresh Token을 즉시 일괄 폐기(Reuse Detection)** |
| **운영 환경 Fail-Fast 검증** | `APP_ENV != "local"`일 때 기본 `JWT_SECRET_KEY`(또는 32바이트 미만 키) 사용, `ALLOW_DEV_LOGIN=true` 설정, 또는 8자 미만의 `FAMILY_REGISTRATION_CODE` 설정 시 서버 기동을 즉시 차단 |
| **로그아웃 소유권 검증 및 세션 정리** | 백엔드 `POST /auth/logout`은 요청자의 `user_id`와 일치하는 Refresh Token만 폐기. Android 앱은 로그아웃 시 ① 서버 `/auth/logout` 호출 → ② `TokenStorage.clear()` → ③ 로컬 Room DB(`items`, `storage_locations`) 전체 삭제 → ④ `ExpiryCheckWorker.cancel()`을 일괄 수행하여 계정 전환 시 이전 데이터 잔류 및 알림 오발송을 방지 |
| **Android 저장소 및 백업 차단** | Android Keystore 기반 암호화 저장소에 보관하며, `allowBackup="false"` 또는 `data_extraction_rules.xml`을 통해 인증 토큰과 로컬 DB가 클라우드/ADB 백업으로 추출되지 않도록 차단 |

---

## 2. 가구(Household) 및 초대 코드 공유

### 2.1 가구 온보딩
- 로그인 직후 소속된 가구가 없는 사용자는 다음 두 가지 중 하나를 선택합니다:
  1. **새 우리집 만들기**: 가구 이름(예: "우리집") 입력 → 기본 보관 장소 템플릿(냉장고 > 냉장실/냉동실, 팬트리 등) 자동 생성 옵션 제공.
  2. **초대 코드로 참여하기**: 가족이 공유한 6~8자리 초대 코드 입력 → 즉시 가구 구성원(`MEMBER`)으로 합류.

### 2.2 역할 및 권한 매트릭스
| 기능 | OWNER (방장) | MEMBER (구성원) |
|---|:---:|:---:|
| 보관 장소 / 품목 조회·등록·수정·소비·폐기 | O | O |
| 변경 이력 조회 | O | O |
| 초대 코드 발급 및 관리 | O | X |
| 가구 이름 변경 / 구성원 추방 | O | X |
| 가구 탈퇴 | (다른 OWNER 위임 후 가능) | O |

### 2.3 초대 코드 및 남용 방지 정책
- 혼동되기 쉬운 문자(`0`, `O`, `1`, `I`, `L`)를 제외한 영문 대문자+숫자 8자리 생성.
- 기본 만료 시간 24시간, 기본 최대 사용 횟수 5회 (생성 시 변경 가능).
- 이미 해당 가구에 소속된 사용자가 코드를 입력하면 중복 가입 없이 안내 메시지 반환.
- **동시 참여 경합(Race Condition) 방지**: `/households/join` 처리 시 `used_count < max_uses` 조건을 포함한 원자적 업데이트(또는 행 잠금)로 동시 요청 시 최대 사용 횟수 초과 가입을 방지합니다.
- **Rate Limiting (무차별 대입 방어)**: 8자리 초대 코드 무차별 대입 및 인증 남용을 막기 위해 `/households/join` 및 `/auth/*` 엔드포인트에 IP/사용자 기준 요청 횟수 제한(`429 TOO_MANY_REQUESTS`)을 적용합니다.

---

## 3. 가족 변경 이력 (Activity Log)

가족 구성원 간 "누가 마지막 우유를 마셨는지", "새로 사 온 식재료를 어디에 넣었는지" 쉽게 파악할 수 있도록 모든 주요 변경 사항을 기록합니다.

### 기록 대상 이벤트
- **품목**: 등록(`CREATED`), 수량/정보 수정(`UPDATED`), 보관 장소 이동(`MOVED`), 소비 완료(`CONSUMED`), 폐기(`DISCARDED`), 삭제(`DELETED`)
- **보관 장소**: 생성(`CREATED`), 이름/위치 변경(`UPDATED`), 삭제(`DELETED`)
- **구성원**: 초대 코드로 합류(`JOINED`), 탈퇴(`LEFT`)

### UI 표시 예시
- *"민수님이 `주방 냉장고 > 냉장실`에 **서울우유 1L (2팩)**을 등록했습니다. · 10분 전"*
- *"지영님이 **계란 30구** 수량을 `12개 → 8개`로 소비했습니다. · 1시간 전"*

---

## 변경 이력

| 날짜 | 변경 내용 | 주요 파일 |
|---|---|---|
| 2026-11-21 | 운영 로그인 제공자를 Kakao 단일로 변경하고 카카오톡 로그인 구현 체크리스트(Kakao Developers·Android·FastAPI) 추가 | `docs/03-features/01-auth-and-household.md` |
| 2026-11-21 | `/health` 점검 모드·최소 앱 버전 필드 추가, 4초 짧은 헬스체크 타임아웃 및 재방문 사용자 오프라인 우선 진입 구현 | `docs/03-features/01-auth-and-household.md`, `server/app/core/config.py`, `server/app/main.py`, `android/...` |
| 2026-11-21 | Google 로그인 버튼 제거, `카카오로 시작하기` 버튼 임시 무반응 처리, `/auth/dev-login` 404 해소를 위해 `ALLOW_DEV_LOGIN` 설정 도입 (`true` 기본값) | `docs/03-features/01-auth-and-household.md`, `server/app/core/config.py`, `server/app/api/routers/auth.py`, `android/app/src/main/java/com/fridge/manager/ui/screens/AuthOnboardingScreen.kt` |
| 2026-11-21 | 보안 하드닝 명세 추가 — 운영 환경 Fail-Fast 검증, Refresh Token 재사용 탈취 감지(Reuse Detection), 로그아웃 소유권 검증 및 로컬 Room 캐시/워커 일괄 정리, 초대 코드 원자적 차감 및 Rate Limiting 정책 반영 | `docs/03-features/01-auth-and-household.md` |
| 2026-11-22 | 백엔드 보안 하드닝 구현 완료 (Fail-Fast, Reuse Detection, 로그아웃 소유권 403, 초대 코드 원자적 차감, Rate Limiting 429) | server/app/core/config.py, server/app/core/rate_limit.py, server/app/main.py, server/app/api/routers/auth.py, server/app/api/routers/households.py, server/Caddyfile, server/entrypoint.sh, server/tests/test_security_hardening.py |
| 2026-11-22 | Android 로그아웃 일괄 정리(/auth/logout 호출 + TokenStorage.clear() + Room clearAllTables() + ExpiryCheckWorker.cancel()) 구현 | android/.../FridgeRepository.kt, MainViewModel.kt |
| 2026-11-25 | Kakao/Google 소셜 로그인 제거 및 부부 2인 전용 백엔드 자체 인증(아이디·비밀번호 + `FAMILY_REGISTRATION_CODE`, 최대 2명 제한, scrypt 해싱, 5회 실패 시 15분 잠금) 명세로 개편 | `docs/03-features/01-auth-and-household.md` |
