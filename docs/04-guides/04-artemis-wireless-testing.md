# 04. Google ARTEMIS + 무선 디버깅 기반 Android 실기기 테스트 가이드

> **상태**: 초안 (Draft)  
> **최종 수정일**: 2026-11-20  
> **관련 문서**: [테스팅 전략](./03-testing-strategy.md), [Android 개발 가이드](./02-android-setup.md)  
> **외부 도구**: [google/artemis](https://github.com/google/artemis) (Apache-2.0)

---

## 1. 개요

본 프로젝트의 Android 실기기 E2E 테스트는 **Android 무선 디버깅(ADB over Wi-Fi)**과 **Google ARTEMIS**를 결합하여, 매 테스트마다 자연어 시나리오 기반으로 앱 설치 → UI 동작 검증 → Logcat·스크린샷 진단 리포트 생성을 자동화합니다.

> [!IMPORTANT]
> **실행 위치 안내**: 무선 디버깅(`adb pair` / `adb connect`)은 스마트폰과 **동일한 로컬 Wi-Fi 네트워크**에 있는 개발 PC에서 수행해야 합니다. (원격 클라우드 VM에서는 사설 Wi-Fi IP로 직접 연결할 수 없습니다.)

---

## 2. 스마트폰 무선 디버깅 연결 절차 (Android 11+)

PC와 스마트폰이 **동일한 Wi-Fi**에 연결된 상태에서 아래 순서대로 진행합니다.

1. **스마트폰 설정**:
   - `설정 > 휴대전화 정보 > 소프트웨어 정보 > 빌드번호` 7회 연속 탭하여 **개발자 옵션** 활성화
   - `설정 > 개발자 옵션 > 무선 디버깅` **ON**
2. **최초 1회 페어링 (`adb pair`)**:
   - 스마트폰 `무선 디버깅 > 페어링 코드로 기기 페어링` 선택 → 표시되는 **IP:페어링 포트** 및 **6자리 페어링 코드** 확인
   ```bash
   adb pair <스마트폰_IP>:<페어링_포트>
   # 프롬프트가 뜨면 6자리 페어링 코드 입력
   ```
3. **무선 디버깅 연결 (`adb connect`)**:
   - 스마트폰 `무선 디버깅` 메인 화면에 표시된 **IP 주소 및 포트**(페어링 포트와 다름)로 연결
   ```bash
   adb connect <스마트폰_IP>:<연결_포트>
   adb devices   # <IP>:<포트>   device 로 표시되면 연결 완료
   ```

---

## 3. Google ARTEMIS 설치 및 Antigravity MCP 연동

개발 PC(스마트폰과 같은 Wi-Fi)에 `google/artemis`를 설치합니다. (`start.sh` / `start.bat` 실행 시 `adb`, `scrcpy`, `ffmpeg`, `uv`가 자동 점검/설치됩니다.)

### macOS / Linux
```bash
git clone https://github.com/google/artemis.git
cd artemis
./start.sh
```

### Windows PowerShell
```powershell
git clone https://github.com/google/artemis.git
cd artemis
.\start.bat
```

### Antigravity / AI IDE MCP 연동
```bash
# Antigravity 글로벌 MCP 및 모바일 테스팅 규칙(rules.md) 자동 설치
uv run artemis mcp --install antigravity
```
- **참고**: ARTEMIS 첫 실행 시 기기에 **Artemis Accessibility Helper**가 자동 설치되어 UI 트리를 안정적으로 탐색합니다. 사전 설치하려면 `uv run artemis helper install`을 실행하세요.

---

## 4. 매 테스트마다 사용하는 표준 실행 절차

코드 수정 후 실기기 테스트를 수행할 때마다 아래 3단계를 반복합니다.

### Step 1: APK 빌드 및 무선 디버깅 기기 설치
```bash
cd android
./gradlew installDebug
```

### Step 2: ARTEMIS 표준 E2E 시나리오 실행
CLI(`uv run artemis run`) 또는 Antigravity 채팅창(MCP 연결 상태)에서 아래 시나리오를 실행합니다.

#### 시나리오 A: 핵심 기능 스모크 테스트 (`--profile flash` 권장, 빠른 검증)
```bash
uv run artemis run "우리집 식재료 관리 앱을 실행하고, '민수' 버튼으로 빠른 로그인한 뒤 '우리집' 가구를 생성하세요. '주방 냉장고 > 냉장실' 아래에 '문쪽 선반' 보관 장소를 추가하고, '서울우유 1L' 품목을 추가한 뒤 수량 '-' 버튼을 눌러 수량이 줄어드는지 확인하고 최종 화면 스크린샷과 Logcat 에러 여부를 보고하세요." --profile flash
```

#### 시나리오 B: 가족 공유 및 변경 이력 정밀 검증 (`--profile pro` 권장, 체크포인트 검증)
```bash
uv run artemis run "우리집 식재료 관리 앱에서 '가족·알림' 탭으로 이동해 초대 코드를 발급하고 값을 기록하세요. 로그아웃 후 '지영'으로 로그인해 해당 초대 코드로 가구에 참여하고, '재고·장소' 탭에서 기존 등록 품목이 보이는지, '가족·알림' 탭 변경 이력에 JOINED가 기록되었는지 검증하세요." --profile pro
```

#### 시나리오 C: 로컬 알림 설정 및 WorkManager 점검
```bash
uv run artemis run "우리집 식재료 관리 앱의 '가족·알림' 탭에서 로컬 알림 스위치를 끄고 다시 켠 뒤 저장 스낵바가 뜨는지 확인하고, 앱 크래시나 ANR 로그가 없는지 Logcat을 점검하세요." --profile flash
```

### Step 3: 결과 확인 및 회귀 기록
- ARTEMIS가 반환하는 **실행 리플레이 / 스크린샷 / Logcat 진단 결과**에서 오류나 UI 겹침이 발견되면 이슈를 수정하고 Step 1부터 재실행합니다.
