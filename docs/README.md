# 📚 우리집 식재료·보관품 관리 시스템 문서 저장소

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-25  

이 디렉토리는 **냉장고, 팬트리 등 가정 내 모든 보관 장소의 식재료 및 완제품을 관리하는 시스템**의 기획·아키텍처·기능 명세·개발 가이드를 통합 관리하는 문서 저장소입니다.

---

## 🗂️ 문서 디렉토리 구조 및 통합 목차

### 01. 기획 (`01-planning/`)
| 문서 | 설명 | 상태 |
|---|---|---|
| [01-project-overview.md](./01-planning/01-project-overview.md) | 프로젝트 개요, 목표, 핵심 가치, 범위(In/Out) 및 기술 스택 요약 | 초안 |
| [02-roadmap.md](./01-planning/02-roadmap.md) | 단계별 개발 로드맵, 마일스톤 및 검증 기준 | 초안 |

### 02. 아키텍처 (`02-architecture/`)
| 문서 | 설명 | 상태 |
|---|---|---|
| [01-system-architecture.md](./02-architecture/01-system-architecture.md) | Kotlin Android + FastAPI + 다중 DB(Oracle ADB / Supabase 등) 전체 시스템 구조 | 초안 |
| [02-oci-always-free-infra.md](./02-architecture/02-oci-always-free-infra.md) | OCI Always Free 무과금 인프라 및 무료 DB(Oracle ADB / Supabase) 운영 전략 | 초안 |
| [03-database-erd.md](./02-architecture/03-database-erd.md) | DB 벤더 중립적 ERD, Oracle/PostgreSQL(Supabase) 호환성 및 자유 트리 설계 | 초안 |
| [04-api-spec.md](./02-architecture/04-api-spec.md) | REST API 엔드포인트, 인증 헤더, 공통 응답/에러 규격 | 초안 |

### 03. 기능 명세 (`03-features/`)
| 문서 | 설명 | 상태 |
|---|---|---|
| [01-auth-and-household.md](./03-features/01-auth-and-household.md) | 가족 전용 자체 인증(아이디·비밀번호 + 등록코드) + JWT, 가구 초대 및 변경 이력 | 초안 |
| [02-storage-and-inventory.md](./03-features/02-storage-and-inventory.md) | 자유 트리 보관 장소 관리 및 식재료·완제품 재고 CRUD 명세 | 초안 |
| [03-expiry-notification.md](./03-features/03-expiry-notification.md) | 유통기한/소비기한 D-Day 정책 및 Android WorkManager 로컬 알림 명세 | 초안 |

### 04. 개발 및 운영 가이드 (`04-guides/`)
| 문서 | 설명 | 상태 |
|---|---|---|
| [01-backend-setup.md](./04-guides/01-backend-setup.md) | FastAPI 다중 DB(Oracle ADB / Supabase PostgreSQL) 연결 설정 및 OCI 배포 가이드 | 초안 |
| [02-android-setup.md](./04-guides/02-android-setup.md) | Kotlin + Jetpack Compose + Room + WorkManager 앱 구조 및 빌드 가이드 | 초안 |
| [03-testing-strategy.md](./04-guides/03-testing-strategy.md) | 백엔드(pytest·다중 DB) 및 Android(JVM·Room·WorkManager·Compose) 테스팅 전략 | 초안 |
| [04-artemis-wireless-testing.md](./04-guides/04-artemis-wireless-testing.md) | Google ARTEMIS + 무선 디버깅 기반 Android 실기기 자동화 테스트 가이드 | 초안 |
| [05-ui-design-guidelines.md](./04-guides/05-ui-design-guidelines.md) | UI/UX 디자인 지침(Fresh Kitchen Material) 및 Stitch MCP 선 디자인 → 후 구현 워크플로우 | 초안 |

---

## ✍️ 문서 작성 및 관리 규칙

1. **문서 상단 메타데이터 필수 기재**: 모든 문서는 최상단에 아래 형식으로 상태와 수정일을 기록합니다.
   ```markdown
   > **상태**: 초안 (Draft) | 검토 중 (Review) | 확정 (Final)  
   > **최종 수정일**: YYYY-MM-DD  
   ```
2. **상태 전이 기준**:
   - `초안 (Draft)`: 요구사항 정리 및 초기 설계 단계
   - `검토 중 (Review)`: 구현 전 세부 스펙 확인 및 보완 단계
   - `확정 (Final)`: 구현 기준점으로 합의 완료된 문서
3. **변경 시 목차 동기화**: 새 문서를 추가하거나 문서 상태가 변경되면 `docs/README.md`의 표도 함께 갱신합니다.
4. **기존 코드베이스와의 관계**: 저장소 내 기존 코드(`backend/`, `front/`)는 참고용으로만 활용하며, 모든 신규 개발은 `docs/`에 정의된 설계를 단일 기준(Single Source of Truth)으로 삼습니다.
