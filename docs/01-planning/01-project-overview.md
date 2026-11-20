# 01. 프로젝트 개요 (Project Overview)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-19  
> **관련 문서**: [개발 로드맵](./02-roadmap.md), [전체 시스템 아키텍처](../02-architecture/01-system-architecture.md)

---

## 1. 프로젝트 배경 및 목적

가정 내 냉장고, 김치냉장고, 팬트리, 베란다 등 여러 보관 장소에 흩어져 있는 **식재료와 완제품(가공식품·음료·생활 식료품 등)**은 위치와 유통기한을 한눈에 파악하기 어려워 중복 구매나 폐기로 이어지기 쉽습니다.

본 프로젝트는 **가족 및 동거인이 함께 가정 내 모든 보관 장소의 재고와 유통기한(소비기한)을 체계적으로 관리**할 수 있는 모바일 중심의 재고 관리 시스템을 구축하는 것을 목표로 합니다.

---

## 2. 핵심 가치 및 설계 원칙

1. **유연한 보관 장소 계층화**: 냉장고뿐만 아니라 팬트리, 다용도실, 서랍 칸까지 사용자가 원하는 깊이로 자유롭게 트리 구조를 구성할 수 있습니다.
2. **놓치지 않는 기한 관리**: 유통기한/소비기한 D-Day를 직관적으로 시각화하고, 외부 푸시 서버 비용 없이 Android 로컬 알림(`WorkManager`)으로 임박 품목을 안내합니다.
3. **가족/동거인 실시간 협업**: 초대 코드를 통해 가족 구성원이 하나의 가구(Household) 공간을 공유하고, 누가 어떤 품목을 추가·소비·폐기했는지 변경 이력을 투명하게 확인합니다.
4. **무과금(Zero-Cost) 클라우드 운영**: Oracle Cloud Infrastructure(OCI)의 **Always Free** 티어만으로 백엔드 API 서버와 관리형 데이터베이스를 안정적으로 운영합니다.

---

## 3. 기술 스택 요약

| 영역 | 기술 스택 | 선정 이유 |
|---|---|---|
| **Mobile App** | Kotlin, Jetpack Compose, MVVM/Clean Architecture, Room, WorkManager | 최신 Android 표준 UI, 로컬 캐싱 및 오프라인/로컬 알림 최적화 |
| **Backend API** | Python, FastAPI, SQLAlchemy 2.0, Pydantic v2, `python-oracledb` | 빠른 REST API 개발, 비동기 지원 및 Oracle DB 공식 드라이버 연동 |
| **Database** | Oracle Autonomous Database (OCI Always Free) | 관리형 DB로 백업/패치 부담 최소화, 계층형 트리 쿼리(`CONNECT BY`/CTE) 강력 지원 |
| **Authentication** | Google / Kakao 소셜 로그인 + 자체 JWT (Access/Refresh) | 간편한 모바일 로그인 경험 및 Stateless API 인증 |
| **Infrastructure** | OCI Compute VM (Always Free) + OCI Autonomous DB (Always Free) | 월 0원 무과금 운영 보장 |

---

## 4. 프로젝트 범위 (Scope)

### 4.1 1차 구현 범위 (In-Scope)
- **인증 및 가구 공유**: Google/Kakao 소셜 로그인, 가구 생성 및 초대 코드 참여, 구성원 관리, 재고 변경 이력 조회
- **자유 트리 보관 장소 관리**: 무제한 깊이의 보관 장소(냉장고, 팬트리, 선반, 칸 등) 생성·수정·이동·삭제
- **품목(식재료/완제품) 재고 관리**: 품목 등록, 수량/단위 조정, 보관 장소 이동, 소비/폐기 상태 처리, 검색 및 필터링
- **유통기한/소비기한 알림**: D-Day 상태 분류(만료/임박/여유), Android `WorkManager` 기반 주기적 로컬 알림
- **OCI Always Free 배포**: FastAPI 서버 배포 및 Autonomous DB 연동

### 4.2 향후 확장 고려 (Out-of-Scope / Backlog)
- AI 사진/영수증 자동 인식 및 바코드 스캔 등록
- 보유 재고 기반 레시피 추천 및 장보기(쇼핑) 리스트 연동
- 소비/폐기 통계 대시보드 및 웹 클라이언트 지원
- Firebase Cloud Messaging(FCM) 기반 실시간 가족 변경 푸시 알림

---

## 5. 주요 사용자 시나리오

1. **장보기 후 정리**: 마트에서 사 온 식재료와 완제품을 앱에서 `주방 > 양문형 냉장고 > 냉장실 > 문쪽 선반` 또는 `팬트리 > 2단 선반`에 유통기한과 함께 빠르게 등록한다.
2. **유통기한 임박 확인**: 아침마다 Android 기기 로컬 알림으로 D-3 이내 임박 품목 요약 알림을 받고, 오늘 소비할 식재료를 결정한다.
3. **가족 공동 관리**: 배우자가 우유를 다 마시고 앱에서 '소비 완료' 처리하면, 변경 이력에 기록되어 다른 가족이 마트에서 장을 볼 때 재고 부족을 바로 확인한다.
