# 04. REST API 명세 (API Specification)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-19  
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
| `GET` | `/health` | 서버 및 연결된 DB(Oracle ADB / Supabase 등) 상태 확인 | 불필요 |

### 2.2 인증 (`auth`)
| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/auth/social/google` | Google ID Token 검증 후 로그인/자동 회원가입 및 JWT 발급 | 불필요 |
| `POST` | `/auth/social/kakao` | Kakao Access Token 검증 후 로그인/자동 회원가입 및 JWT 발급 | 불필요 |
| `POST` | `/auth/refresh` | Refresh Token으로 새 Access/Refresh Token 재발급 (Rotation) | 불필요 |
| `POST` | `/auth/logout` | 현재 Refresh Token 폐기 | 필요 |
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
| `POST` | `/households/join` | 초대 코드 입력하여 가구 참여 | 로그인 사용자 |

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
| `POST` | `/households/{household_id}/items` | 품목 등록 | `MEMBER`+ |
| `GET` | `/households/{household_id}/items/{item_id}` | 품목 상세 조회 | `MEMBER`+ |
| `PATCH` | `/households/{household_id}/items/{item_id}` | 품목 정보/수량/보관 장소 수정 | `MEMBER`+ |
| `POST` | `/households/{household_id}/items/{item_id}/consume` | 소비 처리 (부분 수량 차감 또는 전량 `CONSUMED` 전환) | `MEMBER`+ |
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
  "memo": "행사 상품 1+1"
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
  "unit": "팩",
  "expiry_date": "2026-11-26",
  "expiry_type": "USE_BY",
  "d_day": 7,
  "status": "ACTIVE",
  "memo": "행사 상품 1+1",
  "updated_at": "2026-11-19T18:30:00Z"
}
```
