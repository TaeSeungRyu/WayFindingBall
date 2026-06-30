# 대전 모드 (Nearby Connections) — 구현 계획

`또르르 미로`의 네 게임 모드 각각에 **가까운 친구와 같은 판을 동시에 겨루는** 1:1 로컬 대전을 추가하기 위한 계획서.
인터넷·계정·서버 없이 기기 간 직접 연결(Nearby Connections)만 사용한다.

> **상태 (2026-06-30 기준): 계획 단계 — 미구현.** 본 문서는 구현 전 설계·정책 분석서다.

---

## 0. 확정된 핵심 결정

- **대전 기능은 Android 12L(API 32) 이상에서만 노출한다.** → **위치 권한(ACCESS_FINE/COARSE_LOCATION)을 전혀 요청하지 않는다.**
  - API 32+에서 Nearby는 `NEARBY_WIFI_DEVICES` + `BLUETOOTH_*`(모두 "근처 기기" 권한 그룹)만 사용하고, 위치 권한이 필요 없다.
- **`minSdk`는 26(Android 8.0) 그대로 유지한다 — 올리지 않는다.**
  - 심사 승인은 `targetSdk`(현재 35)가 기준이고 `minSdk`는 "설치 가능 기기 범위"만 정한다. minSdk를 올려도 심사가 쉬워지지 않고, 올리면 Android 8~12 구형/물려받은 기기(아동 앱에서 비중 큼) 사용자를 잃는다.
  - 따라서 minSdk는 26 그대로 두고 **대전 기능만 런타임으로 게이팅**한다 (`Build.VERSION.SDK_INT >= 32`). 구버전 기기는 앱을 정상 설치·플레이하되 "대결" 진입점만 숨긴다.
  - **게이팅 방식: 숨김(hide).** API 32 미만에서는 `ModeSelectScreen`의 "친구와 대결" 진입점을 아예 렌더하지 않는다. (비활성+안내 방식 대신, 아동 앱 혼란을 줄이려 완전히 숨김.)
- **완전 오프라인 P2P** — `INTERNET` 권한조차 선언하지 않는다. 서버·계정·외부 전송 없음.
- **자유 채팅·텍스트 입력 없음. PII·식별자 전송 없음. 광고 SDK 없음.** (아동 앱 정책상 가장 안전한 형태)

---

## 1. 게임 설계

### 1-0. 공통 모델

- 두 기기가 **같은 판(레이아웃)**을 동시에 플레이 → **먼저 목표를 달성한 쪽 승리**.
- 호스트가 판을 정해 연결 직후 1회 전송하거나, 양쪽이 **동일 시드/레벨에 합의**해 같은 판을 보장.
- 각 기기는 **자기 플레이만 로컬에서** 처리한다. 입력 동기화(lockstep) 불필요.
- 진행 중 **가벼운 상태(좌표·점수·진행도)만** 10~20Hz로 주고받아 상대를 반투명 "고스트"로 표현.
- 목표 달성 시 `finished` 메시지 송신. 양쪽이 서로의 완료/시간을 받아 승패 표시. 동시 달성은 타임스탬프로 타이브레이크.

이 모델의 이점: 네트워크 지연에 관대(상대 상태는 보간), 패킷 손실에도 자기 게임은 멈추지 않음, 부정행위 검증 불필요(아동 로컬 대전).

### 1-1. 대전 4종과 모드별 교환 데이터

전송 계층(`NearbyManager`)은 4종이 **공유**하고, 모드별로 **프로토콜 페이로드 + 대전 화면만** 얇게 추가한다.

| 모드 | 대전 방식 | 연결 직후 1회 | 진행 중 교환 | 종료 |
|---|---|---|---|---|
| 🧩 미로 찾기 | 같은 미로 레이스, 먼저 골인 | 미로 ASCII(`Maze.fromAscii`) | 공 위치 `(x,y)` | 골인 + 시간 |
| 🎨 색깔 찾기 | 같은 지시 시퀀스, 먼저 다 맞히기 | 레벨/시드 합의 | 현재 라운드·점수 | 완료 + 시간 |
| 🎯 굴려서 맞히기 | 같은 표적 배치, 먼저 다 맞히기 | 레벨/시드 합의 | 맞힌 표적 수 (`+공 위치`) | 완료 + 시간 |
| ✨ 별자리 잇기 | 같은 별자리, 먼저 완성 | 별자리 키 | 이은 선 진행도 | 완성 + 시간 |

→ 모든 모드가 **일시적 게임 상태(좌표/점수/진행도)만** 교환. 식별자·PII·채팅 없음 → 정책 부담 동일하게 낮음.

### 1-2. 현재 코드와의 접점

- 미로 루프: `ui/GameScreen.kt` (`awaitFrame` + `BallPhysics` + `TiltSensor`). 색깔/굴리기/별자리는 각각 `ColorGameScreen`/`HitGameScreen`/`ConstellationGameScreen`.
- 상태 모델: `game/GameState.kt` (`ballX/ballY/velX/velY/elapsedMs/finished`).
- 판 정의/직렬화: 미로는 `game/Maze.kt` ASCII, 색깔/굴리기는 `ColorGame.stages`/`HitGame.stages`(레벨 합의로 동일 판), 별자리는 `Constellation`/`Zodiac` 키.
- 네비게이션: `MainActivity.kt`의 커스텀 `Screen` 백스택 + `ui/ModeSelectScreen.kt` 진입 카드.

---

## 2. 신규 파일 / 변경 지점

| 파일 | 신규/변경 | 책임 |
|---|---|---|
| `net/NearbyManager.kt` | 신규 | 광고/탐색/연결/페이로드 송수신 래퍼 (4종 **공유**). 상태를 StateFlow로 노출 |
| `net/VersusProtocol.kt` | 신규 | 공통 메시지 프레임(타입 + 바이트) + 모드별 페이로드 직렬화 |
| `ui/VersusLobbyScreen.kt` | 신규 | 대전 모드 고르기 → 방 만들기(광고)/참가하기(탐색), 연결 상태, 권한 안내 (4종 공유) |
| `ui/VersusMazeScreen.kt` | 신규 | 미로 대전 — `GameScreen` 기반 + 상대 고스트 공 + 진행률 HUD + 승패 |
| `ui/VersusColorScreen.kt` | 신규 (2차) | 색깔 찾기 대전 |
| `ui/VersusHitScreen.kt` | 신규 (2차) | 굴려서 맞히기 대전 |
| `ui/VersusConstellationScreen.kt` | 신규 (2차) | 별자리 잇기 대전 |
| `MainActivity.kt` | 변경 | `Screen`에 `VersusLobby`, `Versus{Maze/Color/Hit/Constellation}` 추가 |
| `ui/ModeSelectScreen.kt` | 변경 | API 32+에서만 "친구와 대결" 진입점 렌더, 미만은 숨김 |
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
- [ ] **인터넷·서버·계정·데이터 수집 없음** — `INTERNET` 권한 미선언. 완전 오프라인 P2P, 게임 종료 시 상대 데이터 즉시 폐기. → Data Safety 양식 "수집·공유 데이터 없음" 가능.
- [ ] **광고 SDK 없음** — 대전 흐름에 광고/분석 SDK 미도입.
- [ ] **AAID 권한 미선언** 유지 (targetSdk 35).
- [ ] **Play Console 데이터 안전(Data Safety) 양식** 갱신 — "기기 간 로컬 통신, 서버 미수집"으로 정확히 기재.
- [ ] **권한 사유** — 근처 기기 권한은 "로컬 멀티플레이 연결용(위치 추적 아님)"으로 명시.

**종합 심사 리스크: 낮음~중간.** 채팅·PII·서버·위치가 모두 없는 로컬 대전은 아동 앱 멀티플레이 중 가장 안전한 형태다.

---

## 5. 단계별 구현 순서

**먼저 전송 계층 + 미로 대전 1종을 수직 슬라이스로 완성**하고, 패턴이 검증되면 나머지 3종을 얇게 추가한다.

### 1차 — 전송 계층 + 미로 대전

1. 의존성 추가 + 매니페스트 권한(3-2) + `Build.VERSION.SDK_INT >= 32` 게이팅 골격.
2. `NearbyManager` + `VersusProtocol` — 광고/탐색/연결/페이로드 송수신 (UI 없이 로그로 검증).
3. 권한 런타임 요청 + 안내 화면 (대전 진입 시).
4. `VersusLobbyScreen` — 모드 고르기·방 만들기/참가 UI, 연결 상태 표시.
5. 미로 전송 → 양쪽 동일 미로로 동시 시작.
6. `VersusMazeScreen` — 상대 고스트 공 렌더 + 진행률 HUD + 승패 판정.
7. 재대결/나가기, **연결 끊김 처리**(상대 이탈 시 우아하게 종료·안내).
8. 실기기 2대 QA + Play Console 데이터 안전/권한 사유 갱신.

### 2차 — 나머지 3종 추가

9. `VersusColorScreen` — 색깔 찾기 대전 (레벨 합의 + 라운드/점수 교환).
10. `VersusHitScreen` — 굴려서 맞히기 대전 (표적 시드 합의 + 맞힌 수 교환).
11. `VersusConstellationScreen` — 별자리 잇기 대전 (별자리 키 + 진행도 교환).

각 단계는 독립 커밋 단위로 가능하며, 중단해도 대전 외 기존 기능에는 영향 없음. 2차의 3종은 1차에서 만든 `NearbyManager`/`VersusProtocol`/`VersusLobbyScreen`을 재사용한다.

---

## 6. 남은 결정 항목

- [ ] 1차에 미로 대전만 낼지, 4종 동시 출시할지 (권장: 미로 먼저).
- [ ] 대전 미로: **무작위 생성**(`game/MazeGen.generateRandomMaze`) vs **기존 스테이지 중 선택**?
- [ ] 판 구성: **단판제** vs **연속 대전 + 간단 스코어(예: 3판 2선승)**?
- [ ] 무승부(동시 달성) 처리 방식 — 타임스탬프 타이브레이크로 충분한지, 아니면 무승부 표시 후 재대결?
- [ ] 고스트 표현 — 상대를 반투명 동일 스킨 vs 고정 색 구분?

---

## 7. 참고 (정책·기술 근거)

- Nearby Connections 권한 매트릭스: https://developers.google.com/nearby/connections/android/get-started
- 근처 Wi-Fi 기기 권한 / `neverForLocation`: https://developer.android.com/develop/connectivity/wifi/wifi-permissions
- Android 13 동작 변경: https://developer.android.com/about/versions/13/behavior-changes-13
- Google Play Families 정책: https://support.google.com/googleplay/android-developer/answer/9893335
- Families 앱 데이터 관행(금지 식별자): https://support.google.com/googleplay/android-developer/answer/11043825
