# 🧊 냉장고 관리 (Refrigerator Manager)

스마트한 냉장고 재료 관리 웹 애플리케이션입니다. AI 기반 재료 인식과 레시피 추천 기능을 통해 효율적인 식재료 관리를 도와드립니다.

![Status](https://img.shields.io/badge/status-in%20development-yellow)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## ✨ 주요 기능

- 🥬 **재료 관리**: 냉장고 속 재료를 손쉽게 등록하고 관리
- 📸 **AI 재료 인식**: 이미지를 통한 자동 재료 인식 (Gemini AI)
- ⏰ **유통기한 알림**: 유통기한 임박 재료 자동 알림
- 🍳 **레시피 추천**: 보유 재료 기반 레시피 추천
- 👨‍👩‍👧‍👦 **가족 공유**: 가족 구성원과 냉장고 공유
- 📊 **통계 대시보드**: 재료 사용 패턴 분석

---

## 🛠️ 기술 스택

### Frontend
| 기술 | 버전 | 설명 |
|------|------|------|
| Next.js | 16.x | React 프레임워크 |
| React | 19.x | UI 라이브러리 |
| TypeScript | 5.x | 타입 안전성 |
| TanStack Query | 5.x | 서버 상태 관리 |
| Zustand | 5.x | 클라이언트 상태 관리 |

### Backend
| 기술 | 버전 | 설명 |
|------|------|------|
| FastAPI | 0.111+ | Python 웹 프레임워크 |
| SQLAlchemy | 2.0+ | ORM |
| PostgreSQL | 16 | 데이터베이스 |
| Alembic | 1.13+ | 마이그레이션 |
| Pydantic | 2.x | 데이터 검증 |

### Infrastructure
| 기술 | 설명 |
|------|------|
| Docker | 컨테이너화 |
| Docker Compose | 멀티 컨테이너 오케스트레이션 |

---

## 📁 프로젝트 구조

```
refrigerator_manger/
├── 📂 backend/                 # FastAPI 백엔드
│   ├── 📂 app/
│   │   ├── 📂 api/            # API 라우터
│   │   ├── 📂 application/    # 유스케이스
│   │   ├── 📂 core/           # 설정
│   │   ├── 📂 db/             # 데이터베이스 모델
│   │   ├── 📂 domain/         # 도메인 엔티티
│   │   ├── 📂 infrastructure/ # 외부 서비스
│   │   ├── 📂 interfaces/     # 인터페이스
│   │   ├── 📂 schemas/        # Pydantic 스키마
│   │   └── 📂 services/       # 비즈니스 로직
│   ├── 📂 alembic/            # DB 마이그레이션
│   ├── 📂 tests/              # 테스트
│   ├── 📄 Dockerfile
│   └── 📄 requirements.txt
├── 📂 front/                   # Next.js 프론트엔드
│   ├── 📂 src/
│   │   ├── 📂 app/            # App Router 페이지
│   │   ├── 📂 components/     # React 컴포넌트
│   │   ├── 📂 hooks/          # 커스텀 훅
│   │   └── 📂 lib/            # 유틸리티
│   ├── 📄 Dockerfile
│   └── 📄 package.json
├── 📄 docker-compose.yml       # Docker Compose 설정
├── 📄 .env.example             # 환경변수 예시
├── 📄 .gitignore
└── 📄 README.md
```

---

## 🚀 시작하기

### 사전 요구사항

- [Docker](https://www.docker.com/get-started) & Docker Compose
- [Git](https://git-scm.com/)

### 1. 저장소 클론

```bash
git clone https://github.com/your-username/refrigerator_manger.git
cd refrigerator_manger
```

### 2. 환경변수 설정

```bash
# .env.example을 복사하여 .env 파일 생성
cp .env.example .env

# 필요에 따라 .env 파일 수정
# 특히 프로덕션 환경에서는 아래 값들을 반드시 변경하세요:
# - POSTGRES_PASSWORD
# - JWT_SECRET_KEY
# - GEMINI_API_KEY (AI 기능 사용 시)
```

### 3. Docker Compose로 실행

```bash
# 빌드 및 실행 (백그라운드)
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 서비스 중지
docker-compose down

# 볼륨 포함 완전 삭제
docker-compose down -v
```

### 4. 접속

| 서비스 | URL |
|--------|-----|
| 🌐 Frontend | http://localhost:3000 |
| 🔧 Backend API | http://localhost:8000 |
| 📖 API 문서 (Swagger) | http://localhost:8000/docs |
| 📖 API 문서 (ReDoc) | http://localhost:8000/redoc |

---

## 🧪 개발 환경

### 로컬에서 개발하기

#### Backend (Python)

```bash
cd backend

# 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 개발 서버 실행
uvicorn app.main:app --reload --port 8000
```

#### Frontend (Node.js)

```bash
cd front

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

### 테스트 실행

```bash
# Backend 테스트
cd backend
pytest

# Frontend 테스트
cd front
npm test
```

---

## 📝 API 문서

API 문서는 Swagger UI를 통해 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

### 주요 API 엔드포인트

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `POST` | `/auth/register` | 회원가입 |
| `POST` | `/auth/login` | 로그인 |
| `GET` | `/items` | 재료 목록 조회 |
| `POST` | `/items` | 재료 등록 |
| `GET` | `/items/expiring` | 유통기한 임박 재료 |
| `POST` | `/items/recognize` | AI 재료 인식 |
| `GET` | `/recipes/suggest` | 레시피 추천 |

---

## 🔧 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `POSTGRES_USER` | PostgreSQL 사용자명 | `fridge_user` |
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 | `fridge_password` |
| `POSTGRES_DB` | PostgreSQL 데이터베이스명 | `fridge` |
| `JWT_SECRET_KEY` | JWT 서명 키 | - |
| `GEMINI_API_KEY` | Google Gemini API 키 | - |
| `LLM_MODE` | LLM 모드 (`stub` / `real`) | `stub` |

전체 환경변수 목록은 [.env.example](.env.example)을 참조하세요.

---

## 🤝 기여하기

1. 이 저장소를 Fork 합니다
2. Feature 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add amazing feature'`)
4. 브랜치에 Push 합니다 (`git push origin feature/amazing-feature`)
5. Pull Request를 생성합니다

---

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 등록해주세요.

---

<p align="center">
  Made with ❤️ by Refrigerator Manager Team
</p>
