# Maze Ball

가속도 센서로 공을 굴려 미로를 탈출하는 안드로이드 게임. Jetpack Compose와 Canvas로 작성됐어.

## 주요 기능

- **스플래시 → 홈 → 스테이지 선택 → 게임 → 결과** 흐름
- **가속도 센서**로 공 굴리기 (기기를 기울이면 공이 가속)
- **우측 하단 D-pad**로 센서 없이도 조작 가능 (키 입력이 있을 때는 센서 무시)
- 벽에 부딪히면 그 축 속도가 0이 되는 축별 충돌 처리, sub-step으로 고속 터널링 방지
- 마찰 적용, 최대 속도 제한
- 클리어 시간을 SharedPreferences에 저장하고 **기록 화면**에서 최근순으로 조회

## 스테이지

| ID | 이름        | 난이도   | 그리드 |
| -- | ----------- | -------- | ------ |
| 1  | 스테이지 1 | 쉬움     | 9 x 9  |
| 2  | 스테이지 2 | 보통     | 13 x 13 |
| 3  | 스테이지 3 | 어려움   | 17 x 17 |
| 4  | 스테이지 4 | 전문가   | 21 x 21 |

새 스테이지를 추가하려면 `app/src/main/java/com/rts/rys/ryy/wayfinding/game/Stages.kt`의 `all` 리스트에 `Maze.fromAscii(...)`로 만든 항목을 추가하면 돼. ASCII 규약은 `#` 벽, `S` 시작, `G` 골, 공백 통로.

## 디렉터리 구조

```
app/src/main/java/com/rts/rys/ryy/wayfinding/
├── MainActivity.kt          # NavHost 라우팅
├── data/
│   ├── GameRecord.kt        # 기록 모델
│   └── RecordsRepository.kt # SharedPreferences + org.json 영속화
├── game/
│   ├── Maze.kt              # 그리드 + ASCII 파서
│   ├── Stages.kt            # 프리셋 스테이지
│   ├── BallPhysics.kt       # 가속도/마찰/충돌
│   ├── TiltSensor.kt        # 가속도계 래퍼 (low-pass)
│   └── GameState.kt
└── ui/
    ├── Routes.kt
    ├── SplashScreen.kt
    ├── HomeScreen.kt
    ├── StageSelectScreen.kt
    ├── GameScreen.kt        # 게임 루프 + HUD + 일시정지
    ├── ResultScreen.kt
    ├── RecordsScreen.kt
    ├── MazeCanvas.kt        # Canvas 렌더러
    ├── Keypad.kt            # D-pad
    └── theme/               # 네온 다크 컬러 팔레트
```

## 빌드

이 프로젝트는 **AGP 8.6.1 / Gradle 8.7 / Kotlin 1.9.0**으로 구성돼 있어. Gradle 8.7은 JDK 22까지 지원하므로 **JDK 17 또는 21**로 빌드해야 해. (시스템 기본 `JAVA_HOME`이 JDK 25면 빌드 실패)

PowerShell 기준:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.19'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat assembleDebug
```

산출물: `app/build/outputs/apk/debug/app-debug.apk`

## 조작

- **기기 기울이기**: 기울인 방향으로 공이 가속됨
- **D-pad (우측 하단)**: 누르고 있는 방향으로 공이 가속됨. 떼면 마찰로 감속
- **← 칩 (좌측 상단)**: 일시정지 → "계속하기" 또는 "나가기"

## 물리 파라미터

`BallPhysics.kt` 생성자 인자 / `GameScreen.kt` 상단 상수로 조정 가능:

| 상수 | 기본값 | 의미 |
| --- | --- | --- |
| `radius` | 0.32 | 공 반지름 (셀 단위) |
| `maxSpeed` | 14 | 최대 속도 (cells/sec) |
| `friction` | 1.8 | 마찰 감속 (cells/sec²) |
| `SENSOR_ACCEL_GAIN` | 22 | 센서 입력 → 가속도 변환 계수 |
| `KEYPAD_ACCEL_GAIN` | 18 | D-pad 입력 → 가속도 변환 계수 |

## 요구 환경

- minSdk 26 (Android 8.0)
- targetSdk 35
- 가속도 센서가 없어도 D-pad로 플레이 가능 (`uses-feature` `required="false"`)
