# 또르르 미로

4~8세 어린이를 위해 만든 공 굴리기 게임. 기기를 기울이거나 큰 D-pad를 눌러 공을 굴리는 조작을 바탕으로 여러 미니게임 모드를 담았어. 밝은 하늘 + 잔디 + 별 톤의 친근한 비주얼을 적용했어.

## 게임 모드

홈의 "놀러 가기" → **무엇을 할까요?** 화면에서 네 가지 모드를 고를 수 있어 (`ui/ModeSelectScreen.kt`):

- 🧩 **미로 찾기** — 공을 굴려 별 목적지까지. 레벨 1~13(테마 미로) + 14~20(무한/특수 모드)
- 🎨 **색깔 찾기** — 지시한 색 칸으로 공을 굴리기 (`game/ColorGame.kt`)
- 🎯 **굴려서 맞히기** — 공을 굴려 표적 맞히기 (`game/HitGame.kt`)
- ✨ **별자리 잇기** — 별을 손가락으로 이어 별자리 완성 (`game/Constellation.kt`, `game/Zodiac.kt`)

또한 홈에서 **🤝 1:1 대전모드**로 진입할 수 있어 — 같은 공간의 친구와 근처 기기 연결로 겨루는 실시간 대전(아래 "1:1 대전모드" 참고).

## 주요 기능

- **스플래시 → 홈 → (모드 선택 → 스테이지 선택 → 게임 → 결과)** 흐름
- **가속도 센서**로 공 굴리기 (기기를 기울이면 공이 가속). 홈에서 센서 on/off·민감도(낮음/보통/높음) 설정
- **우측 하단 D-pad**로 센서 없이도 조작 가능 (키 입력이 있을 때는 센서 무시)
- 벽에 부딪히면 그 축 속도가 0이 되는 축별 충돌 처리, sub-step으로 고속 터널링 방지. 마찰·최대 속도 제한
- **오늘의 도전** — 날짜 시드 기반 매일 새 미로 + 연속 도전(streak) 표시 (`game/DailyChallenge.kt`, `data/DailyRepository.kt`)
- **나만의 미로 만들기** — ASCII 격자 에디터로 커스텀 스테이지 제작 (`ui/MazeEditorScreen.kt`, `data/CustomMazesRepository.kt`)
- **내 기록** — 모드별·레벨별 최고기록 요약 카드 + 기록 카드 이미지 공유 (`ui/RecordsScreen.kt`, `data/ShareUtils.kt`)
- **내 도감** — 배지(업적) + 공 스킨 컬렉션. 별을 모아 프리미엄 스킨 구매 (`ui/CollectionScreen.kt`, `data/Badge.kt`, `data/BallSkin.kt`, `data/StarWallet.kt`)
- **소리·배경음악** 설정 및 사운드 매니저 (`data/SoundManager.kt`). 홈의 **⚙ 설정** 버튼에서 센서 on/off·민감도·소리·음악을 모달로 조정 (`ui/HomeScreen.kt`의 `SettingsDialog`)
- **1:1 대전모드** — 근처 기기끼리 실시간 대전 (아래 별도 섹션)
- 기록은 모두 SharedPreferences에 저장 (모드별 저장소 분리)

## 1:1 대전모드

같은 공간의 두 기기가 **Google Nearby Connections**(블루투스/Wi-Fi 직접 통신, 인터넷·서버 없음)로 연결해 실시간으로 겨루는 모드야. 홈의 **🤝 1:1 대전모드** 버튼으로 진입해 종목 허브(`ui/VersusHubScreen.kt`)에서 종목을 고르고, 로비(`ui/VersusLobbyScreen.kt`)에서 방을 만들거나(광고) 참여(탐색)해 매칭돼.

- **종목**: A 미로 찾기(`VersusMazeScreen.kt`) · B 색깔 찾기(`VersusColorScreen.kt`) · C 굴려서 맞히기(`VersusHitScreen.kt`) · D 서바이벌(`VersusSurvivalScreen.kt`)
- **동일 맵**: 호스트가 시드를 보내 양쪽이 같은 무작위 맵을 생성. 시작 카운트다운은 시각 동기화(오프셋 보정)로 맞춰
- **연결 관리**: `net/NearbyManager.kt` (advertise/discover/connect·상태), 메시지 프레임은 `net/VersusProtocol.kt` (시드·위치·완주시간 등 게임 데이터만, PII 없음)
- **닉네임**: 자유 입력이 아니라 "동물+숫자"(예: 토끼42) 프리셋에서 선택만 — 실명/식별자 입력 경로 차단 (`data/VersusNames.kt`)
- **대전 기록**: 승패·기록을 로컬에 저장 (`data/VersusRecord.kt`, `data/VersusRecordsRepository.kt`, 화면 `ui/VersusRecordsScreen.kt`)
- **요건**: Nearby가 위치 권한 없이 동작하려면 **API 32(Android 12L) 이상** 필요 → 구버전 기기에서는 홈에서 대전모드 버튼이 '지원 불가'로 표시됨 (앱 최소 버전 자체는 API 26 유지)

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
│   ├── VersusNames.kt / VersusRecord.kt / VersusRecordsRepository.kt        # 대전 닉네임·기록
│   ├── AppSettings.kt / SoundManager.kt / ShareUtils.kt
├── net/                     # 1:1 대전 통신 (Nearby Connections)
│   ├── NearbyManager.kt                                  # 광고/탐색/연결·상태 관리
│   └── VersusProtocol.kt                                 # 대전 메시지 프레임(시드·위치·완주시간)
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
    ├── VersusHubScreen.kt / VersusLobbyScreen.kt / VersusCommon.kt          # 대전 허브·로비·공용 UI
    ├── VersusMazeScreen.kt / VersusColorScreen.kt / VersusHitScreen.kt / VersusSurvivalScreen.kt  # 대전 종목
    ├── VersusRecordsScreen.kt                           # 대전 기록
    ├── MazeCanvas.kt / BallPainter.kt / EffectsLayer.kt / SkyAmbience.kt
    ├── Keypad.kt                                        # D-pad
    └── theme/                                           # 하늘·잔디 톤 컬러 팔레트
```

## 기획·작업 문서

`docs/`에 기획·작업 문서를 모아둬. **작업이 끝난 문서는 `docs/done/`으로 옮겨** 진행 중인 것과 구분해.

- `docs/` (진행 중·참조)
  - `BILLING_INTEGRATION.md` — 구글 인앱 결제 도입 계획 (미구현)
  - `RECORDS_TODO.md` — 기록 보기 기능 강화 TODO (일부 완료, 열린 항목 있음)
  - `DATA_SAFETY.md` — Play Console 데이터 안전 양식 작성 가이드 (릴리스마다 참조)
- `docs/done/` (완료)
  - `VERSUS_NEARBY_CONNECTIONS.md` — 1:1 대전모드 구현 계획 (구현·출시 완료)
  - `업적_스킨_사이드이펙트.md` — 업적·공 스킨 사전 영향 분석 (구현 완료)

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
- **1:1 대전모드**는 API 32(Android 12L) 이상에서만 지원 (Nearby를 위치 권한 없이 쓰기 위한 요건). 그 미만 기기에서는 대전 버튼이 '지원 불가'로 표시되고 나머지 기능은 정상 동작
- 대전모드 이용 시 근처 기기 연결용 **블루투스/근처 Wi-Fi 기기 권한**을 런타임 요청 (위치 권한 미사용, `neverForLocation`)
