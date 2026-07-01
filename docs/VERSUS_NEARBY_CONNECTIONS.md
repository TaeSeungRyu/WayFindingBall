# 대전 모드 (Nearby Connections) — 구현 계획

`또르르 미로`의 네 게임 모드 각각에 **가까운 친구와 같은 판을 동시에 겨루는** 1:1 로컬 대전을 추가하기 위한 계획서.
인터넷·계정·서버 없이 기기 간 직접 연결(Nearby Connections)만 사용한다.

> **상태 (2026-07-01 기준): 구현 진행 중 (`feature/versus-mode-phase1` 브랜치).**
> A(미로, 시드 무작위 동일 맵)·B(색깔)·C(굴리기)·D(서바이벌) 4개 모드 구현 완료(컴파일 통과). **실기기 2대 QA는 미완**(Nearby는 실하드웨어 필요). 재대결 UI 미구현.

---

## 0. 확정된 핵심 결정

- **대전 기능은 Android 12L(API 32) 이상에서만 노출한다.** → **위치 권한(ACCESS_FINE/COARSE_LOCATION)을 전혀 요청하지 않는다.**
  - API 32+에서 Nearby는 `NEARBY_WIFI_DEVICES` + `BLUETOOTH_*`(모두 "근처 기기" 권한 그룹)만 사용하고, 위치 권한이 필요 없다.
- **`minSdk`는 26(Android 8.0) 그대로 유지한다 — 올리지 않는다.**
  - 심사 승인은 `targetSdk`(현재 35)가 기준이고 `minSdk`는 "설치 가능 기기 범위"만 정한다. minSdk를 올려도 심사가 쉬워지지 않고, 올리면 Android 8~12 구형/물려받은 기기(아동 앱에서 비중 큼) 사용자를 잃는다.
  - 따라서 minSdk는 26 그대로 두고 **대전 기능만 런타임으로 게이팅**한다 (`Build.VERSION.SDK_INT >= 32`). 구버전 기기는 앱을 정상 설치·플레이하되 "대결" 진입점만 숨긴다.
  - **게이팅 방식: 숨김(hide).** API 32 미만에서는 홈 화면의 **"1:1 대전모드"** 버튼을 아예 렌더하지 않는다. (비활성+안내 방식 대신, 아동 앱 혼란을 줄이려 완전히 숨김.)
- **완전 오프라인 P2P** — `INTERNET` 권한조차 선언하지 않는다. 서버·계정·외부 전송 없음.
- **자유 채팅·텍스트 입력 없음. PII·식별자 전송 없음. 광고 SDK 없음.** (아동 앱 정책상 가장 안전한 형태)

---

## 1. 게임 설계

### 1-0. 대전 일반 규칙 (확정)

1. **맵은 랜덤이되 양쪽이 완전히 동일한 맵.** (단, v1은 고정 "미로 찾기 2단계" 사용 — 아래 1-2 참고)
2. **맵에서 나오는 모든 효과도 양쪽이 동일.**
3. **시간제한은 60초.**
4. **양쪽 움직임은 매치 내내 연속으로 공유된다.** (상대가 항상 실시간 고스트로 보임)
5. **한쪽이 먼저 끝나면, 상대가 5초 안에 끝내지 못하면 매치를 자동 종료한다.** (먼저 끝낸 쪽 승리)
6. **양쪽 모두 준비 완료된 상태에서 3-2-1 카운트다운 후 바로 시작.**
7. **누군가 매치 도중 나가면 즉시 종료하고 결과를 기록한다.** (남은 쪽 승리)
8. **대전이 종료되면 결과를 기록한다** — 상대 이름 / 몇 번째 모드(A·B·C·D) / 결과(승·패·무·상대 이탈) / 시각.

### 1-1. 공통 모델

- 두 기기가 **같은 판**을 동시에 플레이 → **먼저 목표를 달성한 쪽 승리**.
- 각 기기는 **자기 플레이만 로컬에서** 처리한다. 입력 동기화(lockstep) 불필요.
- 진행 중 **공 위치·진행도를 끊김 없이 연속 송신**(10~20Hz)해 상대를 반투명 "고스트"로 실시간 표현(규칙 4). 일정 시간 패킷이 끊기면 연결 끊김으로 간주.
- 매치 종료는 **세 조건 중 먼저 충족되는 것**으로(아래 1-3): ① 한쪽 완료 후 5초 경과(규칙 5), ② 양쪽 모두 완료, ③ 전체 시간제한 종료(규칙 3).

이 모델의 이점: 네트워크 지연에 관대(상대 상태는 보간), 패킷 손실에도 자기 게임은 멈추지 않음, 부정행위 검증 불필요(아동 로컬 대전).

### 1-2. 규칙 1·2·6 실현 — 맵 동기화 + 준비·카운트다운

**v1(첫 개발 단계)은 고정 맵으로 단순하게 시작한다.**

- **v1 맵:** 기존 "미로 찾기"의 **2단계 스테이지를 고정 사용**(`Stages.byId`로 양쪽이 같은 프리셋 로드). 프리셋이라 결정론·시드 동기화 작업이 필요 없고, 2단계는 효과가 없어 규칙 2도 자동 충족. 맵 형식은 기존 `Maze` 그대로.
- **이후 확장(규칙 1 본래 목표):** 무작위지만 동일한 맵 → `generateRandomMaze(size, Random(seed), ...)`가 이미 시드 기반 결정론 생성을 지원(일일 도전이 사용). 호스트가 `seed: Long`을 정해 연결 직후 전송 → 양쪽 동일 생성.
- **효과 동기화(규칙 2, 효과 레벨 확장 시):** 효과는 두 종류로 나뉘며 둘 다 "동일 구성·규칙"을 공유한다.
  - **자율 효과** (`RotatingMazeController.tick(dt)` 등 시간만으로 진행): 공유 시드 + 공유 시작시각으로 **완전히 동일**. 컨트롤러 내부 난수(`Chaser.advanceRandom()` 등)도 **공유 시드 파생 `Random`** 주입.
  - **반응형 효과** (`Chaser`/`MovingGoal`/`Stars`/`KeyDoor`.tick(.., ball)): 각자의 공을 추적/수집하므로 **동일 규칙·파라미터·시작 위치**를 공유하되 실시간 상태는 플레이어별로 다름(공정함은 동일). 상대 화면에선 고스트로만 참고.
- **준비 + 카운트다운 (규칙 6):** 연결 후 양쪽이 "준비 완료" 상태가 되면 → **3-2-1 카운트다운** → 카운트 종료 시점을 공유 시작시각(T0)으로 삼아 동시 시작. 이 공유 경과시간이 60초 시간제한·(확장 시)자율 효과를 함께 구동.

### 1-3. 규칙 3·4·5 실현 — 연속 공유와 종료 조건

**연속 공유 (규칙 4):** 매치 시작부터 종료까지 자기 공 위치/진행도를 끊김 없이 송신. 상대는 그 스트림을 보간해 실시간 고스트로 렌더. 일정 시간(예: 2초) 무수신이면 연결 끊김 처리.

**종료 조건 (먼저 충족되는 것 적용):**

1. **상대 이탈 (규칙 7):** 매치 도중 한쪽이 나가거나 연결이 끊기면 **즉시 종료, 남은 쪽 승리**, 결과 기록.
2. **5초 룰 (규칙 5):** 한쪽이 `finished` 송신 → **5초 카운트다운 시작**. 그 안에 상대도 `finished`면 둘 다 완료로 비교(빠른 시간 승), 못 끝내면 **자동 종료·먼저 끝낸 쪽 승리**.
3. **양쪽 완료:** 둘 다 골인하면 즉시 시간 비교로 승부.
4. **시간제한 60초 (규칙 3):** 아무도 못 끝낸 채 60초 종료 시 **진행도 메트릭으로 판정** (미로=목표까지 BFS 남은 거리 적은 쪽 / 색깔·굴리기=점수·맞힌 수 / 별자리=이은 선 수). 동률이면 무승부.

→ 5초 카운트다운과 60초 시간제한 모두 **공유 시작시각(T0) 기준**으로 양쪽이 독립 계산(완료 시각만 메시지로 교환)하므로 한쪽이 끊겨도 판정 가능. 모든 종료 시 1-6의 결과 기록을 남긴다.

### 1-4. 대전 4종(A·B·C·D)과 모드별 교환 데이터

전송 계층(`NearbyManager`)은 4종이 **공유**하고, 모드별로 **프로토콜 페이로드 + 대전 화면만** 얇게 추가한다.
허브 화면의 게임 리스트 A·B·C·D가 아래 4모드에 대응한다. **v1은 A(미로 찾기)만, 그중 2단계 고정으로 구현**한다.

| 게임 | 모드 | 대전 방식 | 연결 직후 1회 | 진행 중 교환 | 종료 |
|---|---|---|---|---|---|
| **A** | 🧩 미로 찾기 | 같은 미로 레이스, 먼저 골인 | **v1: 2단계 고정** / 이후: `seed`+레벨 | 공 위치 `(x,y)` | 골인 + 시간 |
| B | 🎨 색깔 찾기 | 같은 지시 시퀀스, 먼저 다 맞히기 | `seed`+레벨 → 동일 시퀀스 | 현재 라운드·점수 | 완료 + 시간 |
| C | 🎯 굴려서 맞히기 | 같은 표적 배치, 먼저 다 맞히기 | `seed`+레벨 → 동일 표적 배치 | 맞힌 표적 수 (`+공 위치`) | 완료 + 시간 |
| D | 🏃 서바이벌 | 쫓아오는 적을 피해 더 오래 버티기 | `seed` → 동일 적 스폰/타이밍 | 공 위치 `(x,y)` | 잡힘 + 생존 시간 |

> D(서바이벌)는 별자리 잇기 대신 채택. 목표(골)가 없고 **더 오래 살아남는 쪽이 승리**. 적은 각자의 공을 쫓지만(반응형) 스폰 위치·수·타이밍은 공유 시드로 동일해 공정하다. 둘 다 60초 생존 시 무승부.

→ 모든 모드가 **일시적 게임 상태(좌표/점수/진행도)만** 교환. 식별자·PII·채팅 없음 → 정책 부담 동일하게 낮음.

### 1-5. 결과 기록 (규칙 8)

대전 종료 시(상대 이탈 포함) 결과를 로컬에 저장하고 허브의 "기록"에서 조회한다.

- 모델 `VersusRecord`: `opponentName: String`, `game: Char`(A·B·C·D), `result`(WIN/LOSE/DRAW/OPPONENT_LEFT), `elapsedMs`, `timestamp`.
- 저장소 `data/VersusRecordsRepository.kt` — 기존 기록 저장소들(`RecordsRepository` 등)과 동일하게 SharedPreferences + JSON, 보관 한도 적용.
- 화면 `ui/VersusRecordsScreen.kt` — 최근순 리스트(상대 이름·게임·결과·시각).

### 1-6. 화면 흐름 (구현 요구)

```
홈 ──[1:1 대전모드 버튼 · API 32+에서만 노출]──▶ 대전 허브
대전 허브 ── 게임 리스트 A · B · C · D
          └ 하단: [기록]  [이름]
게임 선택 ──▶ [방 만들기]  [방 참여하기]
  · 방 만들기  → 광고 시작, "친구를 기다리는 중…" 대기 화면
  · 방 참여하기 → 탐색, 생성된 방 목록 → 항목 탭하면 참여 요청
양쪽 연결 수락 ──▶ 준비 완료 → 3-2-1 카운트다운 → 매치 시작 (규칙 6)
  [기록] → 대전 결과 기록 화면 (1-5)
  [이름] → 상대에게 보일 이름 설정 (1-7)
```

### 1-7. "이름" 기능 (선택형 닉네임, 확정)

- 상대에게 표시될 닉네임을 설정하는 간단한 기능. `AppSettings`에 `versusName` 저장, Nearby `endpoint 이름`과 결과 기록의 `opponentName`에 사용.
- **자유 텍스트 입력은 만들지 않는다.** 대신 **미리 준비한 닉네임 목록(귀여운 동물 + 숫자, 예: "토끼17")에서 선택**한다. 아동 앱 UGC 리스크를 원천 차단(비속어·실명·식별자 입력 불가).
- 닉네임 목록은 코드 상수로 정의. 미선택 시 첫 진입에 무작위 기본값 자동 배정. 풀은 충돌 확률을 낮추도록 넉넉히(동물 종류 × 두세 자리 숫자).

**이름이 겹칠 때 (양쪽이 같은 닉네임 선택):**

- 1:1이라 기능은 깨지지 않는다 — 각 기기는 자기 쪽을 "나", 상대를 닉네임으로 표시하고, 호스트/참가자 역할로 이미 구분된다. 문제는 표시 혼동뿐.
- **매치 화면:** 연결 시 이름을 교환해 동일하면 **역할 기반으로 자동 구분**한다(호스트는 그대로, 참가자에 작은 표식 또는 공 색 구분). 역할이 정해져 있어 양쪽이 협상 없이 같은 결론에 도달.
- **방 목록:** 각 방은 Nearby `endpointId`가 고유. 같은 이름 방이 여럿이면 뒤에 짧은 꼬리표(예: "토끼17 ②")만 붙여 구분.

### 1-8. 현재 코드와의 접점

- 미로 루프: `ui/GameScreen.kt` (`awaitFrame` + `BallPhysics` + `TiltSensor`). 색깔/굴리기/별자리는 각각 `ColorGameScreen`/`HitGameScreen`/`ConstellationGameScreen`.
- v1 맵: `game/Stages.kt`의 **2단계 프리셋**을 `Stages.byId`로 로드(양쪽 동일).
- 상태 모델: `game/GameState.kt` (`ballX/ballY/velX/velY/elapsedMs/finished`).
- 결정론 생성: `game/MazeGen.generateRandomMaze(size, random, ...)`가 시드된 `Random`을 받음 → 공유 시드로 동일 미로 보장.
- 효과 컨트롤러: `Chaser`/`MovingGoal`/`Rotation`/`DynamicMaze`/`KeyDoor`/`Stars`/`ShadowChaser`Controller. 자율 효과는 `tick(dt)`만으로 진행(동일화 쉬움), 반응형은 `tick(.., ball)`로 자기 공 추적. **내부 난수가 있는 컨트롤러는 공유 시드 파생 `Random` 주입이 필요**(현재는 기본 RNG → 대전용 시드 인자 추가 작업 발생).
- 네비게이션: `MainActivity.kt`의 커스텀 `Screen` 백스택. 진입점은 **`ui/HomeScreen.kt`의 신규 "1:1 대전모드" 버튼**(API 32+에서만 렌더). 이름 저장은 `data/AppSettings.kt`.

---

## 2. 신규 파일 / 변경 지점

| 파일 | 신규/변경 | 책임 |
|---|---|---|
| `net/NearbyManager.kt` | 신규 | 광고/탐색/연결/페이로드 송수신 래퍼 (4종 **공유**). 상태를 StateFlow로 노출 |
| `net/VersusProtocol.kt` | 신규 | 공통 메시지 프레임(준비 / START 카운트다운 / 위치·진행도 / 완료+시각 / 이탈) + 모드별 페이로드 |
| `ui/VersusHubScreen.kt` | 신규 | "1:1 대전모드" 허브 — 게임 리스트 A·B·C·D + 하단 [기록]·[이름] |
| `ui/VersusLobbyScreen.kt` | 신규 | 게임 선택 후 방 만들기(광고)/참여하기(탐색), 대기중·방 목록, 연결 상태, 권한 안내 (4종 공유) |
| `ui/VersusMazeScreen.kt` | 신규 | 미로 대전(A) — `GameScreen` 기반 + 상대 고스트 공 + 진행률·타이머 HUD + 승패. **v1: 2단계 고정** |
| `ui/VersusRecordsScreen.kt` | 신규 | 대전 결과 기록 리스트 |
| `data/VersusRecordsRepository.kt` | 신규 | `VersusRecord` 저장/로드 (SharedPreferences + JSON) |
| `ui/VersusColorScreen.kt` | 신규 (2차) | 색깔 찾기 대전(B) |
| `ui/VersusHitScreen.kt` | 신규 (2차) | 굴려서 맞히기 대전(C) |
| `ui/VersusSurvivalScreen.kt` | 신규 (2차) | 서바이벌 대전(D) — 적 회피, 더 오래 버티기 |
| `MainActivity.kt` | 변경 | `Screen`에 `VersusHub`, `VersusLobby`, `VersusRecords`, `Versus{Maze/Color/Hit/Survival}` 추가 |
| `ui/HomeScreen.kt` | 변경 | API 32+에서만 "1:1 대전모드" 버튼 렌더, 미만은 숨김 (`onVersus` 콜백 추가) |
| `data/AppSettings.kt` | 변경 | `versusName`(상대에게 보일 이름) 저장 |
| `AndroidManifest.xml` | 변경 | 아래 3-2 권한 추가 (위치·인터넷·저장소 **미선언**) |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | 변경 | Nearby 의존성 추가 |

---

## 3. Nearby Connections 통합

### 3-1. 의존성 / API

- 의존성: `com.google.android.gms:play-services-nearby`
- 전략: **`Strategy.P2P_POINT_TO_POINT`** (1:1 고대역폭, 레이스에 적합)
- 흐름: 한쪽 `startAdvertising` ↔ 다른쪽 `startDiscovery` → `requestConnection` → 양쪽 `acceptConnection` → `sendPayload(Payload.fromBytes(...))`
- 페이로드는 모두 **BYTES**(작게 유지). → `READ_EXTERNAL_STORAGE`(FILE 페이로드용) **불필요, 선언하지 않음.**
- 위치 업데이트는 `[seq:Int, x:Float, y:Float]` 수준. 미로는 연결 직후 1회만 전송.

### 3-2. 매니페스트 권한 (위치 없음)

API 32+ 게이팅 전제이므로 위치 권한·저장소 권한을 **선언하지 않는다.**

```xml
<!-- Nearby Connections — API 32+ 한정, 위치 권한 미사용 -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" android:maxSdkVersion="31" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" android:maxSdkVersion="31" />
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" android:minSdkVersion="31" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:minSdkVersion="31" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:minSdkVersion="31"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:minSdkVersion="32"
    android:usesPermissionFlags="neverForLocation" />
```

→ API 32+ 기기에서 런타임 요청 대상 위험 권한은 **`BLUETOOTH_ADVERTISE / BLUETOOTH_CONNECT / BLUETOOTH_SCAN / NEARBY_WIFI_DEVICES` 뿐** (모두 "근처 기기" 그룹). **위치·저장소 권한 없음.**

### 3-3. 권한 런타임 요청

- 앱 시작이 아니라 **대전 모드 진입 시점**에만 요청.
- 요청 직전 아이/보호자용 안내 화면: "가까운 친구와 놀려면 블루투스를 켤게요" 수준의 친화적 설명.
- 거부 시 대전 모드만 비활성화하고 나머지 게임은 정상 동작.

---

## 4. 아동 앱(Families) 정책 준수 체크리스트

확인된 Google Play Families 정책 기준, 본 설계가 지켜야 할 항목:

- [ ] **위치 권한 미요청** — API 32+ 게이팅 + `neverForLocation`으로 달성 (위 3-2).
- [ ] **금지 식별자 전송 금지** — Nearby `endpoint 이름`과 페이로드에 **기기명/MAC/BSSID/SSID/IMEI/AAID/전화번호 등 절대 미사용.** 무작위 일회성 닉네임("토끼12")만 사용.
- [ ] **자유 채팅/UGC 없음** — 텍스트 입력 채널을 만들지 않음. 필요 시 고정 이모지 몇 개만.
- [ ] **"이름" 기능 안전** — **선택형 닉네임으로 확정**: 자유 텍스트 입력 없이 준비된 목록에서 선택만. UGC·실명·식별자 입력 경로 자체가 없음.
- [ ] **인터넷·서버·계정·데이터 수집 없음** — `INTERNET` 권한 미선언. 완전 오프라인 P2P, 게임 종료 시 상대 데이터 즉시 폐기. → Data Safety 양식 "수집·공유 데이터 없음" 가능.
- [ ] **광고 SDK 없음** — 대전 흐름에 광고/분석 SDK 미도입.
- [ ] **AAID 권한 미선언** 유지 (targetSdk 35).
- [ ] **Play Console 데이터 안전(Data Safety) 양식** 갱신 — "기기 간 로컬 통신, 서버 미수집"으로 정확히 기재.
- [ ] **권한 사유** — 근처 기기 권한은 "로컬 멀티플레이 연결용(위치 추적 아님)"으로 명시.

**종합 심사 리스크: 낮음~중간.** 채팅·PII·서버·위치가 모두 없는 로컬 대전은 아동 앱 멀티플레이 중 가장 안전한 형태다.

---

## 5. 단계별 구현 순서

**먼저 전송 계층 + 미로 대전 1종을 수직 슬라이스로 완성**하고, 패턴이 검증되면 나머지 3종을 얇게 추가한다.

### 1차 — 전송 계층 + 미로 대전(A, 2단계 고정)

1. 의존성 추가 + 매니페스트 권한(3-2) + `Build.VERSION.SDK_INT >= 32` 게이팅 골격.
2. 홈 화면 "1:1 대전모드" 버튼(API 32+ 노출) → `VersusHubScreen`(A·B·C·D 리스트 + [기록]·[이름]). v1은 A만 활성.
3. `data/AppSettings.versusName` + "이름" 설정 화면(준비된 닉네임 목록에서 **선택만**).
4. `NearbyManager` + `VersusProtocol` — 광고/탐색/연결/페이로드 송수신 (UI 없이 로그로 검증).
5. 권한 런타임 요청 + 안내 화면 (대전 진입 시).
6. `VersusLobbyScreen` — 방 만들기("기다리는 중…") / 방 참여하기(방 목록 → 탭 참여), 연결 상태.
7. **준비 → 3-2-1 카운트다운**(규칙 6) → 양쪽이 **2단계 프리셋** 로드, 공유 시작시각(T0)으로 동시 시작.
8. `VersusMazeScreen` — 연속 위치 스트림 기반 상대 고스트 + 진행률·60초 타이머 HUD.
9. **종료 조건**(1-3) — 상대 이탈 즉시 종료 / 5초 룰 / 양쪽 완료 / 60초 제한(BFS 거리 판정).
10. **결과 기록**(1-5) — `VersusRecordsRepository` 저장 + `VersusRecordsScreen`("기록"에서 조회).
11. 실기기 2대 QA + Play Console 데이터 안전/권한 사유 갱신.

### 2차 — 맵 확장 + 나머지 3종

12. 미로 대전 맵을 **시드 기반 무작위 동일 맵**으로 확장(규칙 1 본래 목표) + 효과 레벨 도입 시 자율 효과 결정론화.
13. `VersusColorScreen`(B) — 색깔 찾기 대전 (시드 합의 + 라운드/점수 교환).
14. `VersusHitScreen`(C) — 굴려서 맞히기 대전 (시드 합의 + 맞힌 수 교환).
15. `VersusSurvivalScreen`(D) — 서바이벌 대전 (공유 시드 적 스폰 + 위치/생존시간 교환, 더 오래 버티면 승).

각 단계는 독립 커밋 단위로 가능하며, 중단해도 대전 외 기존 기능에는 영향 없음. 2차는 1차의 `NearbyManager`/`VersusProtocol`/`VersusHubScreen`/`VersusLobbyScreen`을 재사용한다.

---

## 6. 남은 결정 항목

- [ ] 판 구성: **단판제** vs **연속 대전 + 간단 스코어(예: 3판 2선승)**? (v1 권장: 단판제)
- [ ] 60초 종료 시 동률 처리 — 무승부 표시 vs 재대결?
- [ ] 고스트 표현 — 상대를 반투명 동일 스킨 vs 고정 색 구분?

### 확정됨

- 시간제한 **60초** (규칙 3).
- "이름"은 **선택형 닉네임**(준비된 동물+숫자 목록에서 선택, 자유 입력 없음).
- v1 맵: **미로 찾기 2단계 고정**(효과 없음). 시드 무작위·효과 동일화는 2차로 이연.
- 반응형 효과(추격자 등)는 "동일 규칙·시드 공유 + 각자 자기 공 추적"으로 구현(효과 레벨 도입 시).
- 1차는 **A(미로)만** 출시, B·C·D는 2차.

---

## 7. 참고 (정책·기술 근거)

- Nearby Connections 권한 매트릭스: https://developers.google.com/nearby/connections/android/get-started
- 근처 Wi-Fi 기기 권한 / `neverForLocation`: https://developer.android.com/develop/connectivity/wifi/wifi-permissions
- Android 13 동작 변경: https://developer.android.com/about/versions/13/behavior-changes-13
- Google Play Families 정책: https://support.google.com/googleplay/android-developer/answer/9893335
- Families 앱 데이터 관행(금지 식별자): https://support.google.com/googleplay/android-developer/answer/11043825
