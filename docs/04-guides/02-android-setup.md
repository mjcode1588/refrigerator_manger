# 02. Android 앱 개발 가이드 (Kotlin + Compose + Room + WorkManager)

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-25  
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
| 자체 인증 | 백엔드 아이디·비밀번호 + 가족 등록코드 + JWT | 2인 가족 전용 자체 로그인 및 자동 갱신 |

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

## 5. GitHub Actions Release APK 자동 서명 및 OCI 서버 배포

`.github/workflows/deploy-android-apk.yml` 워크플로우를 통해 GitHub Actions에서 단위 테스트, Release APK 서명 빌드, `apksigner` 서명 검증, **기존 OCI 서버(`myfridges.duckdns.org`) 자동 배포**를 수행합니다. (네이버 MYBOX 업로드는 `NAVER_MY_BOX`가 설정된 경우에만 동작하는 선택적 미러입니다.)

### 5.1 Release 키스토어 생성 및 Base64 인코딩

로컬에서 1회 생성한 뒤, 키스토어 파일(`*.jks`, `*.keystore`, `*.p12`)은 `.gitignore`에 의해 Git 커밋이 차단되므로 Base64 문자열로 변환해 GitHub Secrets에 등록합니다.

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias fridge-manager
```

- **Windows (PowerShell) Base64 변환**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard
  ```
- **macOS / Linux Base64 변환**:
  ```bash
  base64 -i release.jks | tr -d '\n'
  ```

### 5.2 GitHub Repository Secrets 설정

| 이름 | 필수 여부 | 설명 |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | **필수** | `release.jks`를 Base64로 인코딩한 문자열 |
| `ANDROID_KEYSTORE_PASSWORD` | **필수** | 키스토어 비밀번호 (`storePassword`) |
| `ANDROID_KEY_ALIAS` | **필수** | 키 별칭 (예: `fridge-manager`) |
| `ANDROID_KEY_PASSWORD` | **필수** | 키 비밀번호 (`keyPassword`) |
| `OCI_HOST` | **필수** | OCI Compute VM 공인 IP (`deploy-server.yml`과 공용) |
| `OCI_USER` | **필수** | OCI VM SSH 사용자 (`deploy-server.yml`과 공용) |
| `OCI_SSH_KEY` | **필수** | OCI VM SSH 개인키 (`deploy-server.yml`과 공용, 만료 없음) |
| `OCI_APP_DIR` | 선택 | VM 내 저장소 경로 (미설정 시 `$HOME/refrigerator_manger`) |
| `ANDROID_API_BASE_URL` | 선택 | Release APK에 주입할 API URL (미설정 시 `https://myfridges.duckdns.org/api/v1/`) |
| `NAVER_MY_BOX` | 선택 | 설정 시 네이버 MYBOX에도 미러 업로드 (180일 만료 시에도 OCI 배포는 정상 진행) |
| `NAVER_MYBOX_FOLDER_NAME` | 선택 | MYBOX 루트에 생성/사용할 폴더명 (미설정 시 `FridgeManager-APK`) |
| `NAVER_MYBOX_FOLDER_ID` | 선택 | 특정 MYBOX 폴더 `resourceId` 직접 지정 (`NAVER_MYBOX_FOLDER_NAME`보다 우선) |

### 5.3 OCI 서버 다운로드 경로 보호 및 모바일 설치

- **Caddy 정적 서빙 (`server/Caddyfile`, `server/compose.prod.yml`)**:
  - `server/releases/` 디렉토리를 Caddy `/srv/releases`에 읽기 전용(`:ro`)으로 마운트하고 `https://myfridges.duckdns.org/releases/<APK_DOWNLOAD_PATH>/` 경로로 서빙합니다.
  - 디렉토리 리스팅은 비활성화되며 검색엔진 색인 차단(`X-Robots-Tag: noindex, nofollow`) 및 캐시 방지(`Cache-Control: no-store`) 헤더가 적용됩니다.
  - 가족 외 타인의 접근을 줄이려면 OCI VM의 `server/.env`에 추측하기 어려운 `APK_DOWNLOAD_PATH` 값을 지정하세요(미설정 시 기본값 `apk`).
- **제공 URL**:
  - 모바일 다운로드 페이지: `https://myfridges.duckdns.org/releases/<APK_DOWNLOAD_PATH>/`
  - 최신 APK 직접 다운로드: `https://myfridges.duckdns.org/releases/<APK_DOWNLOAD_PATH>/fridge-manager-latest.apk`
  - 최신 버전 메타데이터: `https://myfridges.duckdns.org/releases/<APK_DOWNLOAD_PATH>/latest.json`

### 5.4 워크플로우 동작 요약

1. **트리거**: `main` 브랜치에 `android/**`, `.github/workflows/deploy-android-apk.yml`, `scripts/upload_to_mybox.py` 변경이 push되거나 `v*` 태그가 push될 때, 또는 `workflow_dispatch` 수동 실행 시 동작합니다.
2. **테스트 및 서명 빌드**:
   - `scripts/tests` 파이썬 단위 테스트 및 `gradle -p android testDebugUnitTest` 실행
   - `ANDROID_KEYSTORE_BASE64`를 임시 파일(`${{ runner.temp }}/release-keystore.jks`)로 복원해 `assembleRelease` 서명 빌드 후 `apksigner verify --print-certs`로 서명 검증 (완료/실패 무관하게 임시 키스토어 즉시 삭제)
3. **OCI 서버 배포 (기본, 만료 없음)**:
   - `scp`로 서명된 `fridge-manager-v{versionName}-b{versionCode}.apk`를 OCI VM `server/releases/`에 업로드합니다.
   - `fridge-manager-latest.apk` 원자적 교체, `latest.json`·모바일용 `index.html` 생성 및 최근 5개 버전만 유지하도록 구버전 APK를 자동 정리합니다.
4. **백업 및 선택적 MYBOX 미러**:
   - GitHub Actions Artifact에 14일간 백업하고, `NAVER_MY_BOX` 시크릿이 있는 경우 `scripts/upload_to_mybox.py`로 미러 업로드를 시도합니다(`continue-on-error: true`).

---

## 변경 이력

| 날짜 | 변경 내용 | 주요 파일 |
|---|---|---|
| 2026-11-21 | APK 보안 하드닝 지침 추가 — `network_security_config.xml` 기반 Release HTTPS 강제, 백업(`allowBackup`/`data_extraction_rules`) 토큰·DB 제외, 로그아웃 시 서버 토큰 폐기 및 Room·WorkManager 초기화, `Authorization` 로그 마스킹, `proguard-rules.pro` 및 `.gitignore` 규칙 명시 | `docs/04-guides/02-android-setup.md` |
| 2026-11-22 | Android APK 보안 하드닝 구현 — `network_security_config`(평문 HTTP 차단), `allowBackup=false`, 로그아웃 시 서버 호출·토큰·Room·WorkManager 일괄 정리, `Authorization` 로그 마스킹, ProGuard 규칙 | `AndroidManifest.xml`, `res/xml/network_security_config.xml`, `proguard-rules.pro`, `AppModule.kt`, `ApiService.kt`, `FridgeRepository.kt`, `MainViewModel.kt` |
| 2026-11-22 | Debug 빌드도 HTTPS 강제 — `src/debug` 네트워크 보안 예외(10.0.2.2/localhost/127.0.0.1 HTTP 허용) 제거 | `android/app/src/debug/res/xml/network_security_config.xml`(삭제) |
| 2026-11-22 | `API_BASE_URL`을 `https://myfridges.duckdns.org/api/v1/`로 확정 | `android/local.properties.example`, `docs/04-guides/02-android-setup.md` |
| 2026-11-25 | Kakao/Google 소셜 로그인 항목 제거 및 가족 전용 자체 인증(`로그인`/`가족 등록`) 안내로 현행화 | `docs/04-guides/02-android-setup.md` |
| 2026-11-25 | GitHub Actions Release APK 자동 서명(`signingConfigs`, `apksigner`) 및 네이버 MYBOX Open API 업로드(`scripts/upload_to_mybox.py`) 절차 추가 | `docs/04-guides/02-android-setup.md` |
| 2026-11-25 | 기본 APK 배포 대상을 토큰 만료 없는 기존 OCI 서버(`myfridges.duckdns.org`, `OCI_SSH_KEY` + Caddy `/releases/<APK_DOWNLOAD_PATH>/`)로 전환하고 네이버 MYBOX 업로드는 선택적 미러로 변경 | `docs/04-guides/02-android-setup.md` |
