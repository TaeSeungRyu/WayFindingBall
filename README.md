# 또르르 미로

4~8세 어린이를 위해 만든 공 굴리기 게임. 기기를 기울이거나 큰 D-pad를 눌러 공을 굴리는 조작을 바탕으로 여러 미니게임 모드를 담았어. 밝은 하늘 + 잔디 + 별 톤의 친근한 비주얼을 적용했어.

## 게임 모드

홈의 "놀러 가기" → **무엇을 할까요?** 화면에서 네 가지 모드를 고를 수 있어 (`ui/ModeSelectScreen.kt`):

- 🧩 **미로 찾기** — 공을 굴려 별 목적지까지. 레벨 1~13(테마 미로) + 14~20(무한/특수 모드)
- 🎨 **색깔 찾기** — 지시한 색 칸으로 공을 굴리기 (`game/ColorGame.kt`)
- 🎯 **굴려서 맞히기** — 공을 굴려 표적 맞히기 (`game/HitGame.kt`)
- ✨ **별자리 잇기** — 별을 손가락으로 이어 별자리 완성 (`game/Constellation.kt`, `game/Zodiac.kt`)

## 주요 기능

- **스플래시 → 홈 → (모드 선택 → 스테이지 선택 → 게임 → 결과)** 흐름
- **가속도 센서**로 공 굴리기 (기기를 기울이면 공이 가속). 홈에서 센서 on/off·민감도(낮음/보통/높음) 설정
- **우측 하단 D-pad**로 센서 없이도 조작 가능 (키 입력이 있을 때는 센서 무시)
- 벽에 부딪히면 그 축 속도가 0이 되는 축별 충돌 처리, sub-step으로 고속 터널링 방지. 마찰·최대 속도 제한
- **오늘의 도전** — 날짜 시드 기반 매일 새 미로 + 연속 도전(streak) 표시 (`game/DailyChallenge.kt`, `data/DailyRepository.kt`)
- **나만의 미로 만들기** — ASCII 격자 에디터로 커스텀 스테이지 제작 (`ui/MazeEditorScreen.kt`, `data/CustomMazesRepository.kt`)
- **내 기록** — 모드별·레벨별 최고기록 요약 카드 + 기록 카드 이미지 공유 (`ui/RecordsScreen.kt`, `data/ShareUtils.kt`)
- **내 도감** — 배지(업적) + 공 스킨 컬렉션. 별을 모아 프리미엄 스킨 구매 (`ui/CollectionScreen.kt`, `data/Badge.kt`, `data/BallSkin.kt`, `data/StarWallet.kt`)
- **소리·배경음악** 설정 및 사운드 매니저 (`data/SoundManager.kt`)
- 기록은 모두 SharedPreferences에 저장 (모드별 저장소 분리)

## 미로 스테이지

미로 찾기 모드는 레벨 1~20으로 구성돼 있고, 뒤로 갈수록 그리드가 커지고 특수 규칙이 붙어. 난이도 라벨은 `Stages.difficultyLabel(level)`:

| 레벨 | 난이도 라벨 | 비고 |
| ---- | ----------- | ---- |
| 1~4   | 쉬워요 → 매우 길어요 | 기본 미로 (그리드 점점 커짐) |
| 5~6   | 벽이 움직여요 / 별도 같이 움직여요 | 동적 미로·움직이는 골 |
| 7~10  | 깜깜해요 / 쫓아와요! / 별을 모아요! / 최종 보스! | 시야 제한·추격자·별 수집 |
| 11~13 | 순간이동! / 빙글빙글! / 끝없는 미로! | 텔레포트·회전·동적 생성 |
| 14~20 | 무한 도전! · 생존 · 얼음 · 타는 길 · 공이 커져요 · 열쇠 · 그림자 | 무한/특수 모드 (`cleared` 도달 단계로 기록) |

기본 스테이지는 `game/Stages.kt`의 `specs`에 ASCII 격자로 정의돼 있어 — `#` 벽, `S` 시작, `G` 골, 공백 통로. 관련 게임 규칙은 `game/` 하위(`MovingGoal`, `Chaser`, `ShadowChaser`, `Rotation`, `KeyDoor`, `DynamicMaze`, `MazeGen` 등)에 분리돼 있어.

## 디렉터리 구조

```
app/src/main/java/com/rts/rys/ryy/wayfinding/
├── MainActivity.kt          # 화면 스택 라우팅
├── data/                    # 모델 + SharedPreferences 영속화
│   ├── GameRecord.kt / RecordsRepository.kt          # 미로 기록
│   ├── ColorRecordsRepository.kt / HitRecordsRepository.kt
│   ├── ConstellationRecordsRepository.kt / DailyRepository.kt
│   ├── CustomMaze.kt / CustomMazesRepository.kt       # 커스텀 미로
│   ├── Badge.kt / BallSkin.kt / AchievementsRepository.kt / StarWallet.kt  # 도감
│   ├── AppSettings.kt / SoundManager.kt / ShareUtils.kt
├── game/                    # 게임 규칙·물리
│   ├── Maze.kt / Stages.kt / MazeGen.kt / DynamicMaze.kt
│   ├── BallPhysics.kt / TiltSensor.kt / GameState.kt
│   ├── MovingGoal.kt / Chaser.kt / ShadowChaser.kt / Rotation.kt / KeyDoor.kt
│   ├── Stars.kt / MazePar.kt / MazeTheme.kt / MazeValidator.kt
│   ├── ColorGame.kt / FloorColorController.kt          # 색깔 찾기
│   ├── HitGame.kt                                       # 굴려서 맞히기
│   ├── Constellation.kt / Zodiac.kt                     # 별자리 잇기
│   └── DailyChallenge.kt
└── ui/
    ├── Routes.kt / SplashScreen.kt / HomeScreen.kt / TutorialScreen.kt
    ├── ModeSelectScreen.kt                              # 모드 선택
    ├── StageSelectScreen.kt / LevelSelectScreen.kt
    ├── ColorStageSelectScreen.kt / HitStageSelectScreen.kt / ConstellationStageSelectScreen.kt
    ├── GameScreen.kt / ColorGameScreen.kt / HitGameScreen.kt / ConstellationGameScreen.kt
    ├── ResultScreen.kt / PauseDialog.kt
    ├── RecordsScreen.kt / CollectionScreen.kt / ConstellationDexScreen.kt
    ├── MazeEditorScreen.kt                              # 나만의 미로 만들기
    ├── MazeCanvas.kt / BallPainter.kt / EffectsLayer.kt / SkyAmbience.kt
    ├── Keypad.kt                                        # D-pad
    └── theme/                                           # 하늘·잔디 톤 컬러 팔레트
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
