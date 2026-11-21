# 01. 인증 및 가족/동거인 공동 관리 명세

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-21  
> **관련 문서**: [DB ERD](../02-architecture/03-database-erd.md), [REST API 명세](../02-architecture/04-api-spec.md)

---

## 1. 소셜 로그인 및 JWT 인증

### 1.1 지원 제공자 및 검증 흐름
- **단일 소셜 로그인 정책**: 운영 환경의 로그인은 **Kakao(카카오톡/카카오계정) 단일 제공자**만 지원합니다. (Google 로그인 버튼은 제거하며, 카카오 SDK 연동 전까지 `카카오로 시작하기` 버튼은 동작하지 않는 플레이스홀더로 유지합니다. 체험 로그인 `/auth/dev-login`은 `ALLOW_DEV_LOGIN=true` 설정으로 제어합니다.)
- **Kakao 로그인 흐름**: Android Kakao SDK로 획득한 `access_token`을 FastAPI(`POST /api/v1/auth/social/kakao`)로 전달 → 백엔드에서 Kakao 사용자 정보 API(`https://kapi.kakao.com/v2/user/me`) 호출하여 `id`, 닉네임, 이메일 확인.
- 최초 로그인 시 `USERS` 및 `SOCIAL_ACCOUNTS` 레코드를 자동 생성하며, 별도의 회원가입 폼 없이 즉시 진입합니다.

### 1.1.1 카카오톡 로그인 구현 체크리스트

#### 1) Kakao Developers 콘솔 설정
1. [Kakao Developers](https://developers.kakao.com/)에서 애플리케이션 생성 후 **네이티브 앱 키**(Android용) 확인
2. **플랫폼 > Android** 등록:
   - 패키지명: `com.fridge.manager`
   - **키 해시(Key Hash)**: 로컬 디버그 키스토어 해시 + 배포용 릴리즈 키스토어 해시 등록
3. **카카오 로그인 > 활성화 설정**: 상태 `ON`
4. **카카오 로그인 > 동의항목**: `닉네임`(필수 동의), `카카오계정(이메일)`(선택 동의) 설정

#### 2) Android 앱 (`android/`) 구현 사항
1. **SDK 및 Manifest 설정**:
   - `com.kakao.sdk:v2-user` 의존성 추가 (`libs.versions.toml` / `app/build.gradle.kts`)
   - `AndroidManifest.xml`에 `AuthCodeHandlerActivity` 및 `kakao{NATIVE_APP_KEY}://oauth` 스킴 등록
   - `Application` 클래스에서 `KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)` 초기화 (키는 `local.properties`로 주입, Git 커밋 금지)
2. **로그인 UI 및 토큰 연동**:
   - 로그인 화면에서 **"카카오로 시작하기"** 단일 버튼만 노출
   - 카카오톡 설치 시 `UserApiClient.instance.loginWithKakaoTalk()`, 미설치 시 `loginWithKakaoAccount()` 호출
   - 획득한 Kakao `access_token`을 백엔드 `POST /api/v1/auth/social/kakao`로 전송해 자체 JWT(`access_token`, `refresh_token`) 발급 및 안전 저장

#### 3) FastAPI 백엔드 (`server/`) 확인 및 보완 사항
1. **구현 완료**: `POST /api/v1/auth/social/kakao` (`verify_kakao` → `https://kapi.kakao.com/v2/user/me`) 구현됨
2. **후속 작업**:
   - Google 로그인 엔드포인트(`/auth/social/google`) 비활성화/제거
   - *(권장 보안 강화)* `https://kapi.kakao.com/v1/user/access_token_info` 호출을 추가해 토큰의 `app_id`가 서비스의 `KAKAO_APP_ID`와 일치하는지 검증 (타 앱 토큰 재사용 방지)

### 1.1.2 로그인 진행 시 백엔드 서버 연결 확인 (`/health`)
- 로그인 화면에 서버 내부 상태(`db_provider` 등)를 직접 노출하지 않습니다.
- 사용자가 로그인을 진행(카카오 로그인 완료 후)하면 로딩 스피너(`"서버에 연결 중입니다..."`)를 표시하고 버튼을 비활성화(중복 클릭 방지)한 뒤 `GET /api/v1/health`로 백엔드 연결 가능 여부를 먼저 확인합니다.
  - **짧은 타임아웃**: `/health` 확인 요청은 **4초 타임아웃**을 적용해 서버 미응답 시 빠르게 안내합니다.
  - **점검 모드 및 최소 앱 버전 확인**: `/health` 응답의 `maintenance`, `maintenance_message`, `min_android_version_code`를 확인하여 서버 점검 중이거나 필수 업데이트가 필요한 경우 로그인 진입을 차단하고 안내 메시지를 표시합니다.
- 서버가 정상(`healthy`) 응답하면 백엔드 인증 및 가구 목록 조회를 완료한 뒤 홈 화면으로 진입하며, 연결 실패 시 내부 상세 정보 대신 사용자 친화적 안내(`"서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."`)를 표시합니다.
- **재방문 사용자 오프라인 우선 진입**: 이미 토큰과 가구 ID가 저장된 사용자는 앱 실행 시 로컬 Room DB 데이터를 즉시 홈 화면에 표시하고, 백그라운드에서 조용히 서버 동기화를 수행합니다(네트워크 실패 시 기존 로컬 데이터 유지).

### 1.2 토큰 정책
| 항목 | 정책 |
|---|---|
| **Access Token** | JWT (HS256 또는 RS256), 유효기간 **30분**, Payload에 `sub`(user_id), `exp`, `iat` 포함 |
| **Refresh Token** | 암호학적 난수 문자열, 유효기간 **30일**, Oracle DB `REFRESH_TOKENS`에 SHA-256 해시 저장 |
| **Token Rotation** | `/auth/refresh` 호출 시 기존 Refresh Token 폐기(`revoked_at`) 후 새 토큰 쌍 발급 |
| **Android 저장소** | Android Keystore 기반 `EncryptedSharedPreferences` 또는 Encrypted DataStore에 안전하게 보관 |

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

### 2.3 초대 코드 정책
- 혼동되기 쉬운 문자(`0`, `O`, `1`, `I`, `L`)를 제외한 영문 대문자+숫자 8자리 생성.
- 기본 만료 시간 24시간, 기본 최대 사용 횟수 5회 (생성 시 변경 가능).
- 이미 해당 가구에 소속된 사용자가 코드를 입력하면 중복 가입 없이 안내 메시지 반환.

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
