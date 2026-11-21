# 05. UI/UX 디자인 지침 및 Stitch MCP 워크플로우

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-21  
> **관련 문서**: [프로젝트 개요](../01-planning/01-project-overview.md), [Android 개발 가이드](./02-android-setup.md), [보관 장소 및 재고 명세](../03-features/02-storage-and-inventory.md)

---

## 1. 기본 원칙: Stitch MCP 선(先) 디자인 → 후(後) 구현

본 프로젝트의 모든 UI/화면 개발은 **Google Stitch MCP를 통해 먼저 디자인 시안을 생성·확정한 뒤, 해당 시안을 기준으로 구현을 진행**하는 것을 원칙으로 합니다.

### 작업 절차
1. **요구사항 확인**: 구현할 기능 명세(`docs/03-features/`)와 화면 구성을 파악합니다.
2. **Stitch MCP 시안 생성/수정**:
   - 신규 화면이나 UI 개편 시 `stitch` MCP(`generate_screen_from_text`, `edit_screens` 등)를 사용해 프로젝트의 공통 디자인 시스템을 적용한 시안을 생성합니다.
   - 생성된 시안 스크린샷과 레이아웃을 검토하여 사용자 흐름 및 한국어 문구·배치를 확정합니다.
3. **확정 시안 기준 코드 구현**:
   - 확정된 Stitch 시안(스크린샷 및 HTML/디자인 토큰)을 단일 기준으로 삼아 `android/`(Jetpack Compose) 및 `web-proto/` UI를 구현합니다.
4. **문서 기록**: 시안 추가·변경 시 본 문서의 스크린 목록과 변경 이력을 업데이트합니다.

---

## 2. Stitch 프로젝트 및 확정 시안 목록

- **Stitch 프로젝트**: `우리집 냉장고 (Refrigerator Manager)` (`projects/14231993319818513329`)
- **공통 디자인 시스템**: `Fresh Kitchen Material` (`assets/9c307ebc22af45ef88f956585df4f82a`)

| 화면 | 용도 | Stitch Screen ID | 상태 |
|---|---|---|---|
| **재고·장소 관리** | 메인 탭 1 — 요약 카드, 자유 트리 보관 장소 칩, D-Day 품목 카드, 빠른 수량 조절 | `projects/14231993319818513329/screens/d0e355f039074e90b1937fca6d757af1` | 시안 생성 완료 |
| **인증 및 가구 온보딩** | 카카오/Google 로그인, 새 우리집 만들기, 초대 코드 참여 | `projects/14231993319818513329/screens/5355023e5b484c05b05ecc476efc7f15` | 시안 생성 완료 |
| **새 품목 등록 (바텀시트)** | 품목명, 보관 장소 트리 경로 선택, 카테고리, 수량/단위, 기한 입력 | `projects/14231993319818513329/screens/3a9fe1d84db44a4cbb99cf1bc164f131` | 시안 생성 완료 |
| **가족·알림 설정** | 메인 탭 2 — 구성원/초대 코드, WorkManager 로컬 알림 설정, 변경 이력 타임라인 | `projects/14231993319818513329/screens/a8c6c74d1dca43e58b31c77cc6ef8f05` | 시안 생성 완료 |

---

## 3. 디자인 시스템 지침 (Fresh Kitchen Material)

### 3.1 컬러 팔레트
| 역할 | 색상 코드 | 사용처 |
|---|---|---|
| **Primary Deep (Teal)** | `#00685F` | 주요 CTA 버튼, 활성 탭/칩, FAB, 방장 배지, 프로필 아바타 |
| **Primary (Teal)** | `#0D9488` | 보조 강조, 토글/아이콘 액센트 |
| **Primary Soft / Mint** | `#CCFBF1` / `#9CF2E8` | 활성 바텀 네비 인디케이터, 추천/하위구역 배지 |
| **Canvas Background** | `#F8FAFC` | 앱 전체 배경 |
| **Surface (Card)** | `#FFFFFF` | 품목 카드, 설정 카드, 바텀시트 (소프트 섀도우 + `1px solid #E2E8F0`) |
| **Surface Muted (Soft Pill)** | `#EFF4F8` | 필(Pill) 입력창, 수량 스테퍼, 초대 코드 박스, 요약 카드 |
| **Text Primary / Muted** | `#0B1C30` / `#64748B` | 기본 본문 텍스트 / 보조 설명·경로 브레드크럼 |

### 3.2 유통기한 D-Day 상태 배지 규칙
모든 리스트 및 상세 화면에서 D-Day 배지는 아래 시맨틱 컬러를 일관되게 사용합니다.

| 상태 | 조건 | 배경색 | 텍스트색 | 표기 예시 |
|---|---|---|---|---|
| **만료 (EXPIRED)** | `d_day < 0` | `#FEE2E2` | `#DC2626` | `D+2 만료` |
| **당일 (TODAY)** | `d_day == 0` | `#FFEDD5` | `#EA580C` | `D-Day` |
| **임박 (EXPIRING_SOON)** | `1 <= d_day <= alert_days` | `#FEF3C7` | `#D97706` | `D-2 소비기한` |
| **여유 (SAFE)** | `d_day > alert_days` | `#DCFCE7` | `#16A34A` | `D-14 소비기한` |
| **기한 없음 (NONE)** | `expiry_date == null` | `#F1F5F9` | `#64748B` | `기한 없음` |

### 3.3 레이아웃 및 컴포넌트 규칙
- **Material Design 3 기반**: 카드 모서리 `16dp`, 버튼/입력창 `12dp`, 필터 칩 Pill(`9999px`), 화면 좌우 마진 `16dp`.
- **타이포그래피**: `Noto Sans` / `Pretendard` 계열 산세리프, 수량 및 D-Day 숫자는 고정폭 숫자(Tabular Figures) 사용.
- **한 손 조작(Thumb Zone) 최적화**:
  - 하단 네비게이션은 2개 핵심 탭(`재고·장소`, `가족·알림`) 유지.
  - 품목 카드 내 `-` / `+` 수량 스테퍼와 `이동` / `폐기` / `소비` 빠른 액션 버튼 배치.
  - 5단계 이상 깊은 보관 장소 경로는 브레드크럼 축약(`... > 냉동실 > 하단 서랍`) 처리.

---

## 변경 이력
- **2026-11-21**: UI/UX 디자인 지침 및 Google Stitch MCP 선 디자인 → 후 구현 워크플로우 문서 최초 작성 (`Fresh Kitchen Material` 디자인 시스템 및 4개 핵심 화면 시안 ID 등록).
- **2026-11-21**: 확정된 Stitch 디자인 시스템 및 4종 시안을 Android Jetpack Compose UI에 반영 (`Theme.kt`, `AuthOnboardingScreen.kt`, `InventoryScreen.kt`, `FamilyAndSettingsScreen.kt`, `MainActivity.kt`), `testDebugUnitTest` 빌드·테스트 통과.
- **2026-11-21**: `InventoryScreen.kt` 상단 가구 스위처 드롭다운 및 품목 카드 `이동` 액션 다이얼로그 추가, `testDebugUnitTest` 재검증 통과.
- **2026-11-21**: Stitch 시안 4종과 1:1 시각 정합성 개선 — `#00685F` 딥 틸 & `#EFF4F8` 소프트 필 입력창 적용, `material-icons-extended` 벡터 아이콘 전환, 상단 앱바·D-Day 상단 컬러 바·카테고리/보관상태 드롭다운 필터·2열 보관장소 그리드 바텀시트·수직 타임라인 이력 구현.
