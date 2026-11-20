# 01. 인증 및 가족/동거인 공동 관리 명세

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-19  
> **관련 문서**: [DB ERD](../02-architecture/03-database-erd.md), [REST API 명세](../02-architecture/04-api-spec.md)

---

## 1. 소셜 로그인 및 JWT 인증

### 1.1 지원 제공자 및 검증 흐름
- **Google 로그인**: Android Credential Manager(Google Sign-In)로 획득한 `id_token`을 FastAPI로 전달 → 백엔드에서 Google 공개키(JWKS) 및 `aud`(Client ID) 검증.
- **Kakao 로그인**: Android Kakao SDK로 획득한 `access_token`을 FastAPI로 전달 → 백엔드에서 Kakao 사용자 정보 API(`https://kapi.kakao.com/v2/user/me`) 호출하여 `id`, 닉네임 확인.
- 최초 로그인 시 `USERS` 및 `SOCIAL_ACCOUNTS` 레코드를 자동 생성하며, 별도의 회원가입 폼 없이 즉시 진입합니다.

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
