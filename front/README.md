# 🧊 냉장고 관리 웹앱

AI 기반 스마트 냉장고 관리 웹 애플리케이션입니다. 채팅, 이미지 인식으로 간편하게 식품을 등록하고, 유통기한 관리, 레시피 추천, 가족 공유 기능을 제공합니다.

## ✨ 주요 기능

### 1. 홈/대시보드
- 임박/만료 식품 카드 표시
- 카테고리별 요약
- 빠른 등록 바로가기

### 2. 채팅 + 이미지 혼합 등록 (휴먼 인 더 루프)
- 텍스트 입력 + 이미지 첨부 (여러 장)
- AI가 식품 인식 후 후보 리스트 생성
- **휴먼 인 더 루프**: 항목별 체크/수정/삭제/추가
- 최종 확인 후 저장

### 3. 이미지 단독 등록
- 영수증/식품 사진 업로드
- AI 인식 → 후보 확인 → 저장

### 4. 아이템 리스트/상세
- 필터 (임박/보관위치/카테고리)
- 검색/정렬
- 그리드/리스트 보기
- 수정/삭제

### 5. 레시피 추천
- 냉장고 재료 기반 추천
- 보유 재료 / 부족 재료 분리 표시
- 부족 재료 → 장보기 리스트 추가

### 6. 가족 공유
- 초대 코드 생성/복사
- 코드 입력하여 냉장고 합류
- 멤버 목록 확인

## 🏗️ 아키텍처

**클린 아키텍처** 패턴을 적용하여 유지보수성과 테스트 용이성을 높였습니다.

```
src/
├── domain/                 # 도메인 계층 (핵심 비즈니스 로직)
│   ├── entities/          # 엔티티 (FoodItem, Recipe, CandidateItem)
│   └── repositories/      # 리포지토리 인터페이스
│
├── application/           # 애플리케이션 계층 (유즈케이스)
│   └── usecases/          # GetItems, IngestItems, SuggestRecipes 등
│
├── infrastructure/        # 인프라 계층 (외부 의존성)
│   ├── api/               # API 클라이언트
│   └── repositories/      # 리포지토리 구현체
│
├── components/            # 프레젠테이션 계층 (UI)
│   └── common/            # 공통 컴포넌트 (Button, Card, Badge 등)
│
├── app/                   # Next.js App Router 페이지
│   ├── page.tsx           # 홈/대시보드
│   ├── register/chat/     # 채팅 등록 (휴먼 인 더 루프)
│   ├── items/             # 아이템 목록
│   ├── recipes/           # 레시피 추천
│   └── family/            # 가족 공유
│
├── lib/                   # 유틸리티
│   ├── api.ts             # 레거시 API (마이그레이션용)
│   ├── hooks/             # React Query 훅
│   └── utils.ts           # 유틸리티 함수
│
└── types/                 # 타입 정의
```

## 🛠️ 기술 스택

- **Framework**: Next.js 16 (App Router)
- **Language**: TypeScript 5.x
- **UI**: React 19
- **상태 관리**: TanStack Query (React Query) + Zustand
- **스타일링**: CSS Modules + CSS Variables
- **테스트**: Jest + React Testing Library
- **아이콘**: React Icons

## 🚀 시작하기

### 1. 패키지 설치

```bash
npm install
```

### 2. 환경변수 설정

`env.template` 파일을 `.env.local`로 복사하고 값을 설정하세요:

```bash
cp env.template .env.local
```

```env
# API Base URL (필수)
NEXT_PUBLIC_API_BASE_URL=http://localhost:8000/api/v1

# 앱 설정
NEXT_PUBLIC_APP_NAME=냉장고 관리
NEXT_PUBLIC_DEFAULT_EXPIRY_DAYS=3
NEXT_PUBLIC_DEFAULT_FRIDGE_ID=
NEXT_PUBLIC_ACCESS_TOKEN=
```

### 3. 개발 서버 실행

```bash
npm run dev
```

http://localhost:3000 에서 확인하세요.

### 4. 테스트 실행

```bash
# 테스트 실행
npm test

# 워치 모드
npm run test:watch

# 커버리지 리포트
npm run test:coverage
```

### 5. 프로덕션 빌드

```bash
npm run build
npm start
```

## 📡 API 연동

### 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/items/ingest` | 텍스트 + 이미지 → 후보 생성 |
| POST | `/items/image` | 이미지 → 후보 생성 |
| POST | `/items/confirm` | 후보 확정 저장 |
| GET | `/items` | 아이템 목록 |
| GET | `/items/:id` | 아이템 상세 |
| PUT | `/items/:id` | 아이템 수정 |
| DELETE | `/items/:id` | 아이템 삭제 |
| GET | `/items/expiring` | 임박/만료 아이템 |
| POST | `/recipes/suggest` | 레시피 추천 |
| POST | `/fridges/invite` | 초대 코드 생성 |
| POST | `/fridges/join` | 냉장고 합류 |
| GET | `/fridges/:id/members` | 멤버 목록 |

### 요청/응답 예시

**인식 요청 (multipart/form-data)**
```
POST /items/ingest
Content-Type: multipart/form-data

text: "우유 2개, 계란 1판"
images[0]: <file>
```

**인식 응답**
```json
{
  "sessionId": "abc123",
  "candidates": [
    {
      "id": "c1",
      "name": "우유",
      "category": "dairy",
      "quantity": 2,
      "unit": "개",
      "expiryDate": "2026-01-20",
      "storageLocation": "fridge",
      "confidence": 0.95
    }
  ]
}
```

## 🎨 디자인 시스템

CSS Variables 기반의 디자인 토큰을 사용합니다.

### 색상
```css
--color-primary: #10b981;      /* 민트 그린 */
--color-secondary: #6366f1;    /* 인디고 */
--color-accent: #f59e0b;       /* 앰버 */
--color-success: #22c55e;
--color-warning: #f59e0b;
--color-danger: #ef4444;
```

### 만료 상태 색상
```css
--color-expired: #ef4444;      /* 만료됨 */
--color-expiring: #f59e0b;     /* 임박 */
--color-fresh: #22c55e;        /* 신선 */
```

## 📝 TDD

도메인 엔티티에 대한 단위 테스트가 포함되어 있습니다.

```bash
# 테스트 실행
npm test

# 특정 파일 테스트
npm test -- FoodItem.test.ts
```

테스트 파일 위치:
- `src/domain/entities/__tests__/FoodItem.test.ts`

## 📁 폴더 구조 상세

```
front/
├── src/
│   ├── app/                           # Next.js App Router
│   │   ├── layout.tsx                 # 루트 레이아웃
│   │   ├── page.tsx                   # 홈 페이지
│   │   ├── page.module.css
│   │   ├── globals.css                # 글로벌 스타일
│   │   ├── providers.tsx              # React Query Provider
│   │   ├── register/
│   │   │   └── chat/
│   │   │       ├── page.tsx           # 채팅 등록 (휴먼 인 더 루프)
│   │   │       └── page.module.css
│   │   ├── items/
│   │   │   ├── page.tsx               # 아이템 목록
│   │   │   └── page.module.css
│   │   ├── recipes/
│   │   │   ├── page.tsx               # 레시피 추천
│   │   │   └── page.module.css
│   │   └── family/
│   │       ├── page.tsx               # 가족 공유
│   │       └── page.module.css
│   │
│   ├── domain/                        # 도메인 계층
│   │   ├── entities/
│   │   │   ├── FoodItem.ts
│   │   │   ├── Recipe.ts
│   │   │   ├── CandidateItem.ts
│   │   │   ├── __tests__/
│   │   │   │   └── FoodItem.test.ts
│   │   │   └── index.ts
│   │   ├── repositories/
│   │   │   ├── IFoodItemRepository.ts
│   │   │   ├── IIngestRepository.ts
│   │   │   ├── IRecipeRepository.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   │
│   ├── application/                   # 애플리케이션 계층
│   │   ├── usecases/
│   │   │   ├── GetItemsUseCase.ts
│   │   │   ├── IngestItemsUseCase.ts
│   │   │   ├── GetExpiringItemsUseCase.ts
│   │   │   ├── SuggestRecipesUseCase.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   │
│   ├── infrastructure/                # 인프라 계층
│   │   ├── api/
│   │   │   └── apiClient.ts
│   │   ├── repositories/
│   │   │   ├── FoodItemRepository.ts
│   │   │   ├── IngestRepository.ts
│   │   │   ├── RecipeRepository.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   │
│   ├── components/                    # UI 컴포넌트
│   │   └── common/
│   │       ├── Button/
│   │       ├── Card/
│   │       ├── Input/
│   │       ├── Badge/
│   │       ├── States/
│   │       └── index.ts
│   │
│   ├── lib/                           # 유틸리티
│   │   ├── api.ts
│   │   ├── utils.ts
│   │   └── hooks/
│   │       ├── useItems.ts
│   │       ├── useRecipes.ts
│   │       ├── useFridge.ts
│   │       └── index.ts
│   │
│   └── types/
│       └── index.ts
│
├── env.template                       # 환경변수 템플릿
├── jest.config.ts                     # Jest 설정
├── jest.setup.ts                      # Jest 셋업
├── package.json
├── tsconfig.json
└── README.md
```

## 🔜 향후 개선 사항

- [ ] 알림/설정 페이지 구현
- [ ] PWA 지원
- [ ] 오프라인 캐싱
- [ ] 바코드 스캔 기능
- [ ] 음성 입력 지원
- [ ] E2E 테스트 추가

## 📄 라이선스

MIT License
