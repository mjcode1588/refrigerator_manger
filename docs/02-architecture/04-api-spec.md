# 04. REST API 명세 (API Specification)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-22  
> **관련 문서**: [DB ERD](./03-database-erd.md), [인증 및 가구 공유](../03-features/01-auth-and-household.md), [보관 장소 및 재고](../03-features/02-storage-and-inventory.md)

---

## 1. 공통 규격

- **Base URL**: `https://<your-oci-domain>/api/v1`
- **인증 헤더**: `Authorization: Bearer <access_token>` (로그인/토큰 갱신/헬스체크 제외 필수)
- **Content-Type**: `application/json; charset=utf-8`
- **날짜/시간 포맷**:
  - 날짜(`expiry_date`): ISO 8601 `YYYY-MM-DD`
  - 타임스탬프: ISO 8601 UTC `YYYY-MM-DDTHH:MM:SSZ`

### 공통 에러 응답 포맷
```json
{
  "error": {
    "code": "LOCATION_CYCLE_DETECTED",
    "message": "하위 보관 장소를 상위 장소로 이동할 수 없습니다.",
    "details": {}
  }
}
```

---

## 2. 엔드포인트 목록

### 2.1 시스템 / 헬스체크
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| `GET` | `/health` | 서버 및 DB 상태, 점검 모드(`maintenance`), 최소 앱 버전(`min_android_version_code`) 확인. **보안상 DB 장애(`503`) 시에도 내부 예외 문자열(`detail`)이나 인프라 상세 정보는 응답에 노출하지 않고 서버 로그에만 기록** | 불필요 |

### 2.2 인증 (`auth`)
> **Rate Limiting**: `/auth/*` 엔드포인트는 IP 기준 분당 요청 횟수 제한을 적용하며 초과 시 `429 TOO_MANY_REQUESTS`를 반환합니다.

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/auth/social/google` | Google ID Token 검증 후 로그인/자동 회원가입 및 JWT 발급 *(Kakao 단일 로그인 전환에 따라 비활성화 예정)* | 불필요 |
| `POST` | `/auth/social/kakao` | Kakao Access Token(`app_id` 포함) 검증 후 로그인/자동 회원가입 및 JWT 발급 | 불필요 |
| `POST` | `/auth/refresh` | Refresh Token으로 새 Access/Refresh Token 원자적 재발급 (Rotation). **이미 폐기된 토큰 재사용 감지 시 해당 사용자의 전체 Refresh Token 일괄 폐기** | 불필요 |
| `POST` | `/auth/logout` | 현재 사용자의 본인 소유(`user_id` 일치) Refresh Token 폐기 | 필요 |
| `GET` | `/users/me` | 내 프로필 및 소속 가구 목록 조회 | 필요 |
| `PATCH` | `/users/me` | 닉네임 등 프로필 수정 | 필요 |

### 2.3 가구 및 가족 공유 (`households`)
| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| `POST` | `/households` | 새 가구(우리집) 생성 (생성자가 `OWNER`) | 로그인 사용자 |
| `GET` | `/households` | 내가 속한 가구 목록 조회 | 로그인 사용자 |
| `PATCH` | `/households/{household_id}` | 가구 이름 수정 | `OWNER` |
| `GET` | `/households/{household_id}/members` | 가구 구성원 목록 조회 | `MEMBER`+ |
| `DELETE` | `/households/{household_id}/members/{user_id}` | 구성원 추방 또는 본인 탈퇴 | `OWNER` 또는 본인 |
| `POST` | `/households/{household_id}/invites` | 초대 코드 생성 (만료시간, 최대 사용 횟수 설정) | `OWNER` |
| `POST` | `/households/join` | 초대 코드 입력하여 가구 참여 (**원자적 `used_count` 차감 및 무차별 대입 방어용 Rate Limiting 적용**) | 로그인 사용자 |

### 2.4 보관 장소 트리 (`locations`)
| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| `GET` | `/households/{household_id}/locations` | 가구의 전체 보관 장소 트리 조회 (`flat` 또는 `tree` 포맷) | `MEMBER`+ |
| `POST` | `/households/{household_id}/locations` | 새 보관 장소/하위 구역 생성 (`parent_id` 선택) | `MEMBER`+ |
| `PATCH` | `/households/{household_id}/locations/{location_id}` | 이름, 아이콘, 정렬 순서, `parent_id`(이동) 수정 | `MEMBER`+ |
| `DELETE` | `/households/{household_id}/locations/{location_id}` | 보관 장소 삭제 (`strategy=restrict\|move\|cascade`) | `MEMBER`+ |

### 2.5 품목 재고 (`items`)
| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| `GET` | `/households/{household_id}/items` | 품목 목록 조회 (필터: `location_id`, `include_sublocations`, `status`, `category`, `q`, `sort`) | `MEMBER`+ |
| `GET` | `/households/{household_id}/items/expiring` | 유통기한 임박/만료 품목 조회 (`within_days=7`, WorkManager 동기화용) | `MEMBER`+ |
| `POST` | `/households/{household_id}/items/photo-upload-url` | 품목 사진(`detail` 1280px WebP ≤500KB, `thumb` 400px WebP ≤100KB) 직접 업로드용 Pre-signed URL 발급 | `MEMBER`+ |
| `POST` | `/households/{household_id}/items` | 품목 등록 (`detail_image_key`, `thumb_image_key` 선택 포함) | `MEMBER`+ |
| `GET` | `/households/{household_id}/items/{item_id}` | 품목 상세 조회 | `MEMBER`+ |
| `PATCH` | `/households/{household_id}/items/{item_id}` | 품목 정보/수량/보관 장소 수정 | `MEMBER`+ |
| `POST` | `/households/{household_id}/items/{item_id}/consume` | 소비 처리 (`amount` 또는 `remaining_quantity` 지정 시 부분 수량 차감·남은 양 기록, 미지정 또는 0 남김 시 `CONSUMED` 전환, `note` 선택) | `MEMBER`+ |
| `POST` | `/households/{household_id}/items/{item_id}/discard` | 폐기 처리 (`DISCARDED` 전환) | `MEMBER`+ |
| `DELETE` | `/households/{household_id}/items/{item_id}` | 오등록 품목 영구 삭제 | `MEMBER`+ |

### 2.6 변경 이력 (`activities`)
| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| `GET` | `/households/{household_id}/activities` | 가구 내 재고/장소/구성원 변경 이력 커서(Cursor) 기반 페이지네이션 조회 | `MEMBER`+ |

---

## 3. 주요 요청/응답 예시

### 품목 등록 (`POST /households/{household_id}/items`)
**Request**:
```json
{
  "location_id": 12,
  "name": "서울우유 1L",
  "category": "DAIRY",
  "quantity": 2,
  "unit": "팩",
  "expiry_date": "2026-11-26",
  "expiry_type": "USE_BY",
  "memo": "행사 상품 1+1",
  "detail_image_key": "households/1/items/20261121/uuid_detail.webp",
  "thumb_image_key": "households/1/items/20261121/uuid_thumb.webp"
}
```

**Response (`201 Created`)**:
```json
{
  "id": 105,
  "household_id": 1,
  "location_id": 12,
  "location_path": "주방 냉장고 > 냉장실 > 문쪽 선반",
  "name": "서울우유 1L",
  "category": "DAIRY",
  "quantity": 2.0,
  "initial_quantity": 2.0,
  "unit": "팩",
  "expiry_date": "2026-11-26",
  "expiry_type": "USE_BY",
  "d_day": 7,
  "status": "ACTIVE",
  "memo": "행사 상품 1+1",
  "detail_image_url": "https://objectstorage.../uuid_detail.webp",
  "thumb_image_url": "https://objectstorage.../uuid_thumb.webp",
  "updated_at": "2026-11-19T18:30:00Z"
}
```

### 품목 소비 및 남은 양 기록 (`POST /households/{household_id}/items/{item_id}/consume`)
**Request** (`amount` 또는 `remaining_quantity` 중 하나만 전달, 둘 다 생략 시 전량 소비):
```json
{
  "remaining_quantity": 0.5,
  "note": "반만 먹고 지퍼백 보관"
}
```

**Response (`200 OK`)**:
- `remaining_quantity > 0`인 경우 `status`는 `"ACTIVE"`로 유지되며 `quantity`가 `0.5`로 갱신되고 `initial_quantity`는 유지되어 잔여 비율(ProgressBar) 계산에 사용됩니다.
- `activity_logs`에는 `consumed`, `remaining`, `unit`, `note`(선택)가 함께 기록됩니다.

---

## 변경 이력

| 날짜 | 변경 내용 | 주요 파일 |
|---|---|---|
| 2026-11-21 | 품목 사진 2단계 WebP(`detail` + `thumb`) 업로드를 위한 `/items/photo-upload-url` 엔드포인트 및 품목 등록 요청/응답 이미지 필드(`detail_image_key`, `thumb_image_key`, `detail_image_url`, `thumb_image_url`) 추가 | `docs/02-architecture/04-api-spec.md` |
| 2026-11-21 | `/items/{item_id}/consume` 요청 스펙에 남은 양 직접 기록(`remaining_quantity`) 및 소비 메모(`note`) 필드 추가 | `docs/02-architecture/04-api-spec.md` |
| 2026-11-21 | 품목 응답(`ItemResponse`)에 기준 수량(`initial_quantity`) 필드 추가 (남은 양 ProgressBar 백엔드 동기화용) | `docs/02-architecture/04-api-spec.md`, `docs/02-architecture/03-database-erd.md` |
| 2026-11-21 | 보안 하드닝 규격 반영 — `/health` DB 내부 에러 문자열(`detail`) 미노출, `/auth/*`·`/households/join` Rate Limiting(`429`), `/auth/refresh` 탈취 감지, `/auth/logout` 토큰 소유권 검증, `/households/join` 원자적 사용 횟수 차감 명세 추가 | `docs/02-architecture/04-api-spec.md` |
| 2026-11-22 | 백엔드 구현 반영 — 오류 코드 RATE_LIMITED(429), FORBIDDEN(403, 타인 refresh token 로그아웃), /health 503 응답에서 detail 제거 | server/app/core/config.py, server/app/core/rate_limit.py, server/app/main.py, server/app/api/routers/auth.py, server/app/api/routers/households.py, server/Caddyfile, server/entrypoint.sh, server/tests/test_security_hardening.py |
