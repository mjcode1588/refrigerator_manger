# 02. Android 앱 개발 가이드 (Kotlin + Compose + Room + WorkManager)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-22  
> **관련 문서**: [전체 시스템 아키텍처](../02-architecture/01-system-architecture.md), [유통기한 로컬 알림 명세](../03-features/03-expiry-notification.md)

---

## 1. 권장 Android 기술 스택

| 영역 | 기술 / 라이브러리 | 용도 |
|---|---|---|
| 언어 / 빌드 | Kotlin 2.x, Gradle (Kotlin DSL + Version Catalog `libs.versions.toml`) | 최신 Kotlin 표준 및 의존성 일원화 |
| UI | Jetpack Compose (Material 3), Navigation Compose | 선언형 UI 및 화면 전환 |
| DI | Hilt (`dagger-hilt`) | 의존성 주입 및 `HiltWorker` 연동 |
| 비동기 | Kotlin Coroutines + Flow | 비동기 API 호출 및 Room 리액티브 스트림 |
| 네트워크 | Retrofit2 + OkHttp3 + Kotlinx Serialization | REST API 통신 및 JWT 자동 갱신 |
| 로컬 DB / 설정 | Room, Jetpack DataStore (Preferences) | 오프라인 캐시 및 알림 사용자 설정 저장 |
| 백그라운드 | WorkManager (`androidx.work`) | 주기적 유통기한 체크 및 로컬 알림 |
| 소셜 로그인 | Android Credential Manager (Google), Kakao Android SDK v2 | 소셜 인증 토큰 획득 |

---

## 2. 권장 패키지 구조 (Clean Architecture + MVVM)

```text
app/src/main/java/com/fridge/manager/
├── core/
│   ├── network/          # Retrofit, AuthInterceptor, TokenAuthenticator
│   ├── database/         # AppDatabase (Room), DAOs, Entities
│   ├── datastore/        # TokenStorage, NotificationPreferences
│   └── notification/     # NotificationHelper, ExpiryCheckWorker (WorkManager)
├── domain/
│   ├── model/            # Household, StorageLocationNode, FoodItem, ActivityLog
│   ├── repository/       # Repository Interfaces
│   └── usecase/          # BuildLocationTreeUseCase, CalculateDDayUseCase 등
├── data/
│   ├── remote/           # API Service Interfaces, DTOs
│   ├── mapper/           # DTO <-> Entity <-> Domain 변환
│   └── repository/       # Repository Implementations (Remote + Room 캐시 전략)
└── ui/
    ├── theme/            # Material 3 Color, Typography
    ├── navigation/       # NavGraph, DeepLink 경로 정의
    ├── auth/             # 로그인 & 가구 온보딩 화면
    ├── home/             # 대시보드 (임박/만료 요약, 최근 이력)
    ├── locations/        # 자유 트리 보관 장소 탐색기 & 편집기
    ├── items/            # 품목 목록, 상세, 등록/수정 폼
    ├── family/           # 구성원 관리, 초대 코드 공유, 변경 이력
    └── settings/         # 로컬 알림 시간/기준일 설정
```

---

## 3. Room 로컬 캐시 및 동기화 전략

- **Single Source of Truth (읽기)**: UI는 Room의 `Flow<List<ItemEntity>>`를 구독하여 화면 진입 시 즉시 데이터를 표시합니다.
- **네트워크 갱신**: 화면 진입 또는 Pull-to-Refresh 시 FastAPI에서 최신 데이터를 받아 Room을 트랜잭션으로 갱신(`@Transaction` upsert/replace)합니다.
- **쓰기 작업**: 품목 등록/수정/소비/폐기는 서버 API 성공 후 응답 결과를 Room에 즉시 반영하여 데이터 일관성을 유지합니다.

---

## 4. 빌드 환경 및 보안 설정

- `local.properties`에 아래 민감 키를 보관하고 `BuildConfig`로 주입합니다 (루트 `.gitignore`에 `android/local.properties`, `android/.gradle/`, `android/.kotlin/`을 등록해 Git 커밋을 원천 차단):
  ```properties
  API_BASE_URL=https://myfridges.duckdns.org/api/v1/
  KAKAO_NATIVE_APP_KEY=...
  ```
- **최소 지원 SDK**: `minSdk = 26` (Android 8.0, Notification Channel 및 `java.time` 기본 지원), `targetSdk = 35`.
- **빌드 유형별 네트워크 보안 (`network_security_config.xml`)**:
  - `AndroidManifest.xml`에서 전역 `android:usesCleartextTraffic="true"`를 제거합니다.
  - `src/main/res/xml/network_security_config.xml`에서 `cleartextTrafficPermitted="false"`로 평문 HTTP를 차단하며, **Debug/Release 모두 HTTPS만 허용**합니다(debug 예외 없음). 개발 중에도 HTTPS 서버(도메인 + Caddy 자동 인증서) 또는 신뢰 가능한 인증서를 사용하는 터널을 `API_BASE_URL`로 지정해야 합니다.
- **클라우드/ADB 백업을 통한 토큰·DB 유출 차단**:
  - `AndroidManifest.xml`에서 `android:allowBackup="false"`를 적용하거나, `data_extraction_rules.xml` 및 `backup_rules.xml`을 지정하여 인증 토큰 저장소(`auth_prefs.preferences_pb`)와 로컬 Room DB(`fridge_manager.db`)가 클라우드 자동 백업 및 `adb backup`으로 추출되지 않도록 제외합니다.
- **로그아웃 시 완전한 세션·로컬 데이터 정리**:
  - 로그아웃 시 ① 서버 `POST /api/v1/auth/logout` 호출로 Refresh Token 폐기 → ② 로컬 `TokenStorage.clear()` → ③ Room DB(`items`, `storage_locations`) 전체 초기화(`clearAllTables`) → ④ `ExpiryCheckWorker.cancel()`을 순차 수행하여 다중 계정 전환 시 이전 사용자 데이터 잔류 및 알림 오발송을 방지합니다.
- **네트워크 디버그 로그 마스킹 및 R8/ProGuard 규칙 (`proguard-rules.pro`)**:
  - `BuildConfig.DEBUG`에서 `HttpLoggingInterceptor`를 사용할 때 `redactHeader("Authorization")`을 설정해 Logcat에 JWT가 평문 출력되지 않도록 마스킹합니다.
  - `release` 빌드(`isMinifyEnabled = true`) 시 난독화로 인한 런타임 크래시를 방지하기 위해 `android/app/proguard-rules.pro`에 `kotlinx.serialization`(`@Serializable` DTO) 및 `Retrofit2` 인터페이스 Keep 규칙을 명시합니다.

---

## 변경 이력

| 날짜 | 변경 내용 | 주요 파일 |
|---|---|---|
| 2026-11-21 | APK 보안 하드닝 지침 추가 — `network_security_config.xml` 기반 Release HTTPS 강제, 백업(`allowBackup`/`data_extraction_rules`) 토큰·DB 제외, 로그아웃 시 서버 토큰 폐기 및 Room·WorkManager 초기화, `Authorization` 로그 마스킹, `proguard-rules.pro` 및 `.gitignore` 규칙 명시 | `docs/04-guides/02-android-setup.md` |
| 2026-11-22 | Android APK 보안 하드닝 구현 — `network_security_config`(평문 HTTP 차단), `allowBackup=false`, 로그아웃 시 서버 호출·토큰·Room·WorkManager 일괄 정리, `Authorization` 로그 마스킹, ProGuard 규칙 | `AndroidManifest.xml`, `res/xml/network_security_config.xml`, `proguard-rules.pro`, `AppModule.kt`, `ApiService.kt`, `FridgeRepository.kt`, `MainViewModel.kt` |
| 2026-11-22 | Debug 빌드도 HTTPS 강제 — `src/debug` 네트워크 보안 예외(10.0.2.2/localhost/127.0.0.1 HTTP 허용) 제거 | `android/app/src/debug/res/xml/network_security_config.xml`(삭제) |
| 2026-11-22 | `API_BASE_URL`을 `https://myfridges.duckdns.org/api/v1/`로 확정 | `android/local.properties.example`, `docs/04-guides/02-android-setup.md` |
