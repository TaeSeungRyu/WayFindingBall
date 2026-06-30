# 구글 인앱 결제 (Google Play Billing) 적용 계획

`또르르 미로`에 구글 플레이 인앱 결제를 도입해서, 결제 전에는 일부 기능을 잠금 처리하고 결제 완료 후 잠금 해제하는 흐름을 만들기 위한 작업 목록.

> **상태 (2026-06-30 기준): 미구현 — 계획 단계.** 코드베이스에 `BillingManager`/`PurchaseScreen`/`premiumUnlocked` 등 결제 관련 구현은 아직 없습니다. 아래는 그대로 유효한 작업 계획입니다.
>
> 참고: 그 사이 별 재화 기반의 인앱 잠금 해제(스킨 별 구매, `data/StarWallet.kt` + `data/BallSkin.kt`의 `priceStars`)가 별도로 도입되었습니다. 실제 현금 결제(IAP)를 붙일 때 이 별 경제와의 관계(별 충전 상품 vs 기능 잠금 해제)를 먼저 정해야 합니다.

---

## 1. 사전 결정 사항 (구현 전 정해야 함)

- [ ] **잠금 대상 기능** — 후보:
  - 전체 게임플레이 ("놀러 가기")
  - "나만의 게임 만들기" (에디터)
  - 일부 스테이지 (예: 5단계 이후 잠금) — 부분 무료/유료 모델
  - "내 도감" 배지/꾸미기 요소
- [ ] **결제 모델** — 옵션:
  - **일회성 (Non-consumable)** — 한 번 결제하면 영구 해제 (어린이 게임에 일반적이고 단순)
  - **구독 (Subscription)** — 월/연 구독
  - **혼합** — 일부는 무료, 일부는 일회성 또는 다른 상품
- [ ] **가격 책정** — 통화별, 국가별 가격대
- [ ] **무료 체험** 여부 — "처음 1단계만 무료" 같은 hook 단계 운영?
- [ ] **환불 정책** — Google Play 표준(48시간 자동) 외에 별도 안내 필요한지

> 권장: 어린이 대상 단일 앱에서 가장 단순한 모델은 **일회성 Non-consumable + "처음 1~2 단계 무료 체험"**. 구독은 보호자 동의/취소 흐름이 복잡해서 어린이 앱에 부담.

---

## 2. Google Play Console 작업

- [ ] Play Console에서 앱 등록 (또는 기존 앱에 인앱 상품 추가 권한 확인)
- [ ] **인앱 상품 등록**
  - 상품 ID: 예) `tororu_full_unlock` (소문자 + 밑줄, 영구 불변)
  - 상품 종류: **관리되는 상품(One-time)** 또는 **구독**
  - 가격/통화 설정
  - 제목/설명(언어별)
- [ ] **라이선스 테스터** 등록 — 실제 결제 없이 결제 흐름 테스트할 Google 계정 추가
- [ ] **내부 테스트 트랙** 빌드 업로드 (서명된 AAB) — 인앱 결제는 Play 스토어에 업로드된 빌드에서만 테스트 가능
- [ ] **Public Key** 복사해 두기 — 영수증 검증에 사용

---

## 3. Play Billing 라이브러리 추가

`gradle/libs.versions.toml`에 버전 항목 추가:

```toml
[versions]
billing = "7.1.1"

[libraries]
androidx-billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "billing" }
```

`app/build.gradle.kts`의 `dependencies`에 추가:

```kotlin
implementation(libs.androidx.billing)
```

`AndroidManifest.xml`에 권한 추가:

```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

---

## 4. BillingClient 통합 코드

### 4-1. `BillingManager` 신규 클래스 작성

위치: `app/src/main/java/com/rts/rys/ryy/wayfinding/data/BillingManager.kt`

책임:
- `BillingClient` 초기화 및 Play 서비스 연결
- 상품 정보 조회 (`queryProductDetails`)
- 결제 흐름 시작 (`launchBillingFlow`)
- 결제 결과 콜백(`PurchasesUpdatedListener`) 처리
- 결제 완료 후 **반드시** `acknowledgePurchase` 호출 (3일 내 acknowledge 안 하면 자동 환불)
- 앱 시작 시 `queryPurchasesAsync`로 기존 구매 복원
- 결과를 `MutableStateFlow<EntitlementState>`로 노출

핵심 API:
```kotlin
class BillingManager(context: Context) : PurchasesUpdatedListener {
    val entitlement: StateFlow<EntitlementState>
    fun connect()
    suspend fun queryProducts(): List<ProductDetails>
    fun launchPurchase(activity: Activity, productDetails: ProductDetails)
    suspend fun restorePurchases()
    fun disconnect()
}
```

### 4-2. Application 클래스 추가

`MainActivity`에서 직접 만들지 말고 `Application.onCreate()`에서 한 번만 초기화. AndroidManifest에 `android:name=".TororuApp"` 등록.

### 4-3. 영수증 검증

서버가 없으므로 로컬 검증:
- `BillingClient.queryPurchasesAsync()`가 반환하는 `Purchase` 객체의 `purchaseState == PURCHASED && isAcknowledged == true`만 신뢰
- **추가 권장**: `Purchase.originalJson`과 `Purchase.signature`를 Play Console의 Public Key로 검증 (간단한 RSA 서명 검증). 코드 예시는 Google 샘플 `TrivialDrive` 참조.
- 보안 우회 시도 대비가 중요하면 Play Integrity API 도입 검토(추가 작업).

---

## 5. 결제 상태 영구 저장

### 5-1. `AppSettings`에 잠금 해제 플래그 추가

`AppSettings.kt`에 다음 추가:

```kotlin
private const val KEY_PREMIUM_UNLOCKED = "premium_unlocked"

private val _premiumUnlocked = mutableStateOf(false)
val premiumUnlocked: MutableState<Boolean> get() = _premiumUnlocked

fun setPremiumUnlocked(value: Boolean) {
    _premiumUnlocked.value = value
    prefs?.edit()?.putBoolean(KEY_PREMIUM_UNLOCKED, value)?.apply()
}
```

`init()`에 로드 로직 추가:
```kotlin
_premiumUnlocked.value = p.getBoolean(KEY_PREMIUM_UNLOCKED, false)
```

### 5-2. BillingManager → AppSettings 연결

`BillingManager`의 entitlement state를 관찰하다가 결제 확인되면 `AppSettings.setPremiumUnlocked(true)` 호출.

> 주의: SharedPreferences만 의존하면 단순 우회 가능. 진짜 보안이 필요하면 매 앱 시작 시 `queryPurchasesAsync`로 Play와 직접 확인 후 캐시값을 갱신해야 함.

---

## 6. UI 잠금/잠금 해제 처리

### 6-1. HomeScreen 버튼 잠금

`HomeScreen.kt`의 각 `BigButton`을 `AppSettings.premiumUnlocked.value`로 분기:

```kotlin
val unlocked = AppSettings.premiumUnlocked.value
BigButton(
    label = if (unlocked) "놀러 가기" else "🔒 놀러 가기 (잠김)",
    bg = if (unlocked) CoralPink else InkSoft,
    onClick = if (unlocked) onStart else onShowPurchase
)
```

또는 잠금 아이콘 오버레이만 추가하고 onClick에서 결제 다이얼로그 띄우는 방식.

### 6-2. 결제 안내 화면/다이얼로그 신규 추가

위치: `app/src/main/java/com/rts/rys/ryy/wayfinding/ui/PurchaseScreen.kt`

내용:
- 잠금 해제된 기능 설명
- 가격 표시 (BillingManager가 가져온 `ProductDetails.oneTimePurchaseOfferDetails.formattedPrice`)
- "구매하기" 버튼 → `BillingManager.launchPurchase()` 호출
- "구매 복원" 버튼 → `BillingManager.restorePurchases()` 호출 (다른 디바이스/재설치 시)
- 결제 진행 중 로딩 상태
- 에러 처리 (네트워크 끊김, 결제 취소 등)

### 6-3. Navigation 통합

`MainActivity.kt`의 `Screen` sealed class에 `Purchase` 추가:
```kotlin
data object Purchase : Screen()
```

그리고 잠긴 버튼 클릭 시 `push(Screen.Purchase)`.

---

## 7. 결제 복원 / 다중 기기

- 사용자가 같은 Google 계정으로 다른 기기에서 로그인하거나 앱 재설치 시 자동 복원되려면:
  - 앱 시작 시 `queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(INAPP).build())` 호출
  - 반환된 `Purchase` 목록을 검사해 해당 상품 ID 발견 시 `AppSettings.setPremiumUnlocked(true)`
- 사용자가 명시적으로 "구매 복원" 버튼을 눌렀을 때 같은 흐름 실행

---

## 8. 보안 고려

- [ ] **결제 우회 방지** (기본):
  - 영수증 서명 검증 (Public Key 활용)
  - 매 앱 시작마다 Play와 재확인
- [ ] **ProGuard 설정** — release 빌드에서 BillingClient 클래스 난독화 예외 추가:
  ```
  -keep class com.android.vending.billing.**
  -keep class com.google.android.gms.** { *; }
  ```
- [ ] **루팅/탬퍼 탐지** — 어린이 게임 수준에서는 과도할 수 있음. 일반적으로 영수증 검증으로 충분.
- [ ] **Play Integrity API** — 진짜 우회 막을 거면 추가 도입(난이도/비용 ↑).

---

## 9. 어린이 앱 특이사항 (중요)

`또르르 미로`는 명확히 어린이 대상이므로 추가 정책 준수 필요:

- [ ] **Designed for Families 프로그램** 가입 여부 결정. 가입하면 광고/결제 가이드라인 강화.
- [ ] **결제 비밀번호/생체 인증** — Google Play 설정에서 "모든 구매에 인증 요구" 권장 안내를 앱 내에 표시
- [ ] **보호자 동의 UI** — 결제 직전 "어른에게 보여 주세요" 같은 친화적 경고
- [ ] **결제 후 환불 안내** — 실수 결제 시 24시간 내 Play 자체 환불 가능 안내
- [ ] **개인정보 처리방침** 갱신 — Play Console에 URL 등록 필수
- [ ] **연령 등급 재신청** — 결제 기능 추가 시 IARC 설문 다시 응답

---

## 10. 테스트 계획

- [ ] **라이선스 테스터 계정**으로 결제 흐름 검증 (실제 청구 없음)
- [ ] **네트워크 끊김** 상태에서 결제 시도 → 에러 처리
- [ ] **결제 도중 앱 강제 종료** → 다음 실행 시 acknowledge 누락 복구 흐름
- [ ] **재설치 후 구매 복원** 흐름
- [ ] **결제 취소** 후 잠금이 그대로인지
- [ ] **여러 Google 계정** 전환 시 entitlement 갱신
- [ ] **실 결제 1회** (출시 직전) — 라이선스 테스터 결제만으론 잡지 못하는 케이스 있음

---

## 11. 출시 체크리스트

- [ ] Play Console 인앱 상품 활성화 상태
- [ ] 새 versionCode/versionName으로 빌드
- [ ] 서명된 AAB 업로드
- [ ] 내부/비공개 테스트 → 공개 테스트 → 프로덕션 단계 진행
- [ ] 개인정보 처리방침/약관 URL 등록
- [ ] 어린이 대상 콘텐츠 정책 통과 확인
- [ ] 결제 안내 문구/스크린샷 추가

---

## 예상 작업 분량

| 작업 | 난이도 | 대략 시간 |
|---|---|---|
| Play Console 설정 + 상품 등록 | 낮음 | 1~2시간 |
| BillingClient 통합 + AppSettings 연결 | 중간 | 4~8시간 |
| UI 잠금 + 결제 안내 화면 | 낮음~중간 | 3~5시간 |
| 영수증 검증 + 보안 | 중간 | 2~4시간 |
| 테스트 (라이선스 + 실 결제) | 중간 | 2~4시간 |
| **합계** | | **약 12~23시간** |

> 실제 결제가 가능한 빌드를 만들려면 **반드시** 서명된 AAB로 Play Console 테스트 트랙에 업로드되어야 한다는 점이 가장 큰 진입장벽. 로컬 에뮬레이터/디버그 빌드만으로는 결제 흐름을 끝까지 검증할 수 없음.

---

## 다음 단계 권장

1. **1번 사전 결정 사항** 먼저 확정 (잠금 대상, 결제 모델, 가격)
2. Play Console에 앱/상품 등록 + 라이선스 테스터 추가
3. `BillingManager` 스켈레톤 + `AppSettings.premiumUnlocked` 추가
4. `HomeScreen` 버튼 잠금 분기 + 결제 안내 화면
5. 내부 테스트 트랙 업로드 후 라이선스 테스터로 전체 흐름 검증
