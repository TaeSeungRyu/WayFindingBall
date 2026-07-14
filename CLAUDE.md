# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

4~8세 어린이용 안드로이드 공 굴리기 게임 "또르르 미로" (Jetpack Compose, 단일 `:app` 모듈). 기기 기울임(가속도 센서) 또는 D-pad로 공을 굴려 여러 미니게임 모드를 플레이. 상세 기능 설명은 `README.md` 참고.

## 빌드

**JDK 버전이 핵심 함정이다.** AGP 8.6.1 / Gradle 8.7 / Kotlin 1.9.0 구성. Gradle 8.7은 JDK 22까지만 지원하므로 **JDK 17 또는 21**로 빌드해야 한다. 시스템 기본 `JAVA_HOME`이 JDK 25면 빌드가 실패하니 아래처럼 명시적으로 지정한다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.19'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat assembleDebug           # 디버그 APK
.\gradlew.bat assembleRelease         # 서명된 릴리스 APK (keystore.properties 필요)
.\gradlew.bat test                    # 단위 테스트 (JVM)
.\gradlew.bat connectedAndroidTest    # 계측 테스트 (기기/에뮬 필요)
```

- 디버그 산출물: `app/build/outputs/apk/debug/app-debug.apk`
- 릴리스 서명은 루트 `keystore.properties`(gitignore됨)에서 로드. 없으면 릴리스는 미서명으로 빌드됨.
- 테스트는 스캐폴드(`ExampleUnitTest`, `ExampleInstrumentedTest`)만 존재 — 실질적 테스트 커버리지 없음.
- 릴리스마다 `app/build.gradle.kts`의 `versionCode`(정수 증가)와 `versionName`을 함께 올린다.

## 아키텍처

### 네비게이션 — 직접 만든 화면 스택 (Navigation-Compose 아님)

핵심을 놓치기 쉬운 부분: **`ui/Routes.kt`는 사실상 사용되지 않는다.** 실제 네비게이션은 `MainActivity.kt`의 `MazeApp()`에서 수동 백스택(`mutableStateListOf<Screen>`)으로 구현돼 있다.

- `Screen`은 sealed class — 각 화면이 자기 파라미터를 데이터 클래스로 들고 있다 (예: `Game(stageId)`, `Result(...)`).
- 조작 함수: `push`, `pop`, `popUntil { predicate }`, `replaceTop`. 게임→결과 전환은 `replaceTop`으로 게임 화면을 백스택에서 치운다.
- `AnimatedContent`로 좌우 슬라이드 전환, `SaveableStateProvider(key = screen.toString())`로 화면별 상태 보존.
- 스플래시/튜토리얼은 백스택 밖의 `showSplash`/`showTutorial` 불리언으로 먼저 게이트.
- 새 화면 추가 = `Screen`에 항목 추가 + `when(screen)` 분기 추가. 다른 라우팅 등록 파일은 없다.

### 레이어

```
ui/    — Compose 화면 + 렌더링 (MazeCanvas, BallPainter, EffectsLayer 등)
game/  — 순수 게임 규칙·물리 (BallPhysics, Stages, Chaser, Rotation, KeyDoor, ...). Compose 비의존.
data/  — 모델 + SharedPreferences 영속화. 모드별 저장소 분리.
net/   — 1:1 대전 통신 (Nearby Connections).
```

### 싱글턴 초기화 순서

`MainActivity.onCreate`에서 순서대로 초기화되며 앱 전역에서 참조된다: `AppSettings.init` → `SoundManager.init` → `loadCustomMazes`. `AppSettings`/`SoundManager`는 object 싱글턴이고 설정값을 Compose `State`로 노출한다. BGM 생명주기는 Activity의 `onResume`/`onPause`/`onDestroy`에 묶여 있다.

### 스테이지 정의

기본 미로는 `game/Stages.kt`의 `specs`에 ASCII 격자로 정의 (`#`=벽, `S`=시작, `G`=골, 공백=통로). `Stages`는 기본/데일리/커스텀 스테이지를 런타임에 합쳐 보관하는 레지스트리 역할도 한다 (`setDailyStage`, `setCustomStages`, `byId`). 레벨 14~20은 무한/특수 모드로 별도 규칙 파일(`Chaser`, `ShadowChaser`, `DynamicMaze`, `KeyDoor` 등)이 붙는다.

### 물리

`game/BallPhysics.kt` — 축별 충돌 처리(벽에 부딪힌 축의 속도만 0), 고속 터널링 방지용 sub-step, 마찰·최대 속도 제한. 튜닝 상수는 `BallPhysics` 생성자 인자와 `ui/GameScreen.kt` 상단 상수에 있다 (`radius`, `maxSpeed`, `friction`, `SENSOR_ACCEL_GAIN`, `KEYPAD_ACCEL_GAIN`).

### 1:1 대전모드

Google Nearby Connections(BT/Wi-Fi 직접 통신, 서버 없음)로 같은 공간의 두 기기가 실시간 대전.

- `net/NearbyManager.kt` — advertise/discover/connect·연결 상태 관리. 로비~게임 화면이 하나의 인스턴스를 공유하며, `MazeApp`의 `versusManager` 상태가 소유. 화면을 벗어날 때 반드시 `mgr.stop()` + `versusManager = null`.
- `net/VersusProtocol.kt` — 메시지 프레임(시드·위치·완주시간 등 **PII 없는 게임 데이터만**). 호스트가 시드를 보내 양쪽이 동일 맵 생성.
- **API 32(Android 12L) 이상 필요** (Nearby를 위치 권한 없이 쓰기 위한 요건). 미만 기기는 홈에서 대전 버튼이 '지원 불가'. 앱 자체 minSdk는 26 유지.
- 닉네임은 자유 입력이 아니라 "동물+숫자" 프리셋 선택만 (`data/VersusNames.kt`) — 아동 대상 앱이라 실명/식별자 입력 경로를 의도적으로 차단.

## 컨벤션

- **커밋 메시지는 한국어**, `type: 설명` 형식 (`fix:`, `feat:`, `chore:`, `docs:`). 최근 로그 스타일을 따를 것.
- 코드 주석·문서는 한국어.
- `docs/`에 기획·작업 문서. **완료된 문서는 `docs/done/`으로 이동**해 진행 중인 것과 구분한다.
- 앱은 세로 고정(portrait), 화면 항상 켜짐(`FLAG_KEEP_SCREEN_ON`).
- 기록은 전부 SharedPreferences (모드별 저장소 분리). 서버·계정 없음.
