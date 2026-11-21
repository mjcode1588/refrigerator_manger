# AGENTS.md — 프로젝트 작업 및 문서 관리 지침

이 문서는 본 저장소(`refrigerator_manger`)에서 작업하는 모든 AI 에이전트와 개발자가 따라야 할 공통 규칙입니다.

---

## 1. 프로젝트 구조 및 단일 기준(Single Source of Truth)

- **설계 및 명세 기준**: `docs/` 디렉토리를 프로젝트의 단일 기준(Single Source of Truth)으로 삼습니다. ([docs/README.md](./docs/README.md))
- **UI/UX 디자인 기준**: 신규 화면 및 UI 개편 시 **Google Stitch MCP**로 먼저 시안을 생성·확정한 뒤, 확정된 디자인을 기준으로 구현을 진행합니다. ([docs/04-guides/05-ui-design-guidelines.md](./docs/04-guides/05-ui-design-guidelines.md))
- **주요 구현 디렉토리**:
  - `server/`: FastAPI 백엔드 (다중 DB 지원, Alembic, pytest)
  - `android/`: Kotlin + Jetpack Compose Android 앱 (Room, WorkManager, Hilt)
  - `web-proto/`: Phase 3 기능 실험용 경량 웹 프로토타입
  - `backend/`, `front/`: 기존 저장소 코드 (참고용)

---

## 2. 필수 작업 순서: 문서 선(先) 확인·업데이트 후 개발 진행 (Docs-First)

**사용자의 지시(기능 추가, 수정, 설계 변경, UI 개편 등)를 받으면 바로 코드를 작성하지 않고, 반드시 아래 순서에 따라 문서를 먼저 확인·업데이트한 뒤 해당 문서를 기준으로 개발을 진행합니다.**

1. **관련 문서 먼저 확인**:
   - [docs/README.md](./docs/README.md)와 관련 영역(`01-planning/`, `02-architecture/`, `03-features/`, `04-guides/`)에서 지시 내용과 연관된 기존 문서를 먼저 조회하여 현재 설계, 스펙, 제약사항을 파악합니다.
2. **문서 먼저 업데이트 및 Phase 등록**:
   - 지시 내용에 맞춰 기획, API 스펙, DB 스키마, 기능 명세, 로드맵, 가이드 등의 변경이 필요한 경우 **코드 구현 전에 먼저 해당 `docs/` 문서를 업데이트**(또는 신규 문서 생성)합니다.
   - **구현 작업은 반드시 [docs/01-planning/02-roadmap.md](./docs/01-planning/02-roadmap.md)에 `Phase`(또는 세부 체크리스트)로 먼저 등록**한 뒤, 등록된 Phase 단위로 구현을 진행합니다.
   - 신규 화면·UI 개편 지시인 경우 구현 전 Google Stitch MCP로 시안을 먼저 생성·확정하고 디자인 문서/가이드를 반영합니다.
3. **업데이트된 문서 및 Phase를 참조하여 개발 진행 (명시적 구현 지시 시에만 착수)**:
   - **사용자가 명시적으로 구현(코드 작성·수정)을 지시하지 않은 경우, 문서 업데이트·Phase 등록·Stitch 시안 작업까지만 수행하고 임의로 코드 구현을 바로 진행하지 않습니다.**
   - 구현 지시를 받은 경우에만 갱신된 `docs/` 문서와 등록된 Phase 체크리스트를 단일 기준(Single Source of Truth)으로 삼아 구현(`server/`, `android/`, `web-proto/` 등)과 테스트를 진행하고, 완료된 항목은 `[x]`로 갱신합니다.
   - 구현 도중 스펙이나 구조가 달라질 경우 문서와 코드가 어긋나지 않도록 문서를 함께 최신화합니다.

---

## 3. 필수 규칙: 수정·업데이트 시 문서 기록 남기기

**코드, 기능, API, DB 스키마, UI/디자인, 인프라, 테스트 등 어떤 수정이나 업데이트가 이루어질 때마다 반드시 `docs/` 문서에 변경 기록을 남겨야 합니다.**

### 기록 절차
1. **관련 설계·명세 문서 업데이트**:
   - 기획/로드맵 변경 → `docs/01-planning/` (`02-roadmap.md` 체크리스트 등)
   - 시스템 구조·인프라·DB ERD·API 스펙 변경 → `docs/02-architecture/`
   - 인증/가구, 보관 장소/재고, 유통기한/알림 기능 변경 → `docs/03-features/`
   - 개발 환경, 배포, 테스트, UI/디자인 가이드 변경 → `docs/04-guides/`
2. **문서 상단 메타데이터 갱신**:
   ```markdown
   > **상태**: 초안 (Draft) | 검토 중 (Review) | 확정 (Final)  
   > **최종 수정일**: YYYY-MM-DD
   ```
3. **변경 이력(Changelog) 기록**:
   - 수정한 문서 하단(또는 `## 변경 이력` 섹션)에 **날짜(`YYYY-MM-DD`), 변경 내용 요약, 수정된 주요 파일/영향 범위**를 기록합니다.
4. **목차 동기화**:
   - 새 문서를 생성하거나 문서 상태가 바뀌면 `docs/README.md`의 통합 목차 표를 함께 갱신합니다.

---

## 4. 작업 완료 전 체크리스트

- [ ] 사용자의 명시적인 구현 지시가 없는 상태에서 임의로 코드 구현을 시작하지 않았는가?
- [ ] 개발 착수 전 관련 `docs/` 문서를 먼저 확인하고, 필요한 명세·설계 업데이트 및 `02-roadmap.md` Phase 등록을 선행한 뒤 구현했는가?
- [ ] 구현 완료된 Phase 항목을 `docs/01-planning/02-roadmap.md`에 `[x]`로 반영했는가?
- [ ] 변경한 기능/모듈의 자동화 테스트(`pytest`, Gradle 테스트 등)가 통과했는가?
- [ ] 이번 수정·업데이트 내역이 해당하는 `docs/` 문서에 반영되고, `최종 수정일` 및 변경 기록이 남겨졌는가?
- [ ] 새 문서가 추가된 경우 `docs/README.md` 목차가 갱신되었는가?
