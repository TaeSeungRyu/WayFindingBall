# 개발 환경 셋업 (새 PC / 새 클론)

클론 후 바로 빌드하려면 **git에 없는 설정 파일**(SDK 경로·키스토어)을 손으로 만들어야 한다. 이 문서만 따라 하면 새 PC에서도 처음부터 빌드·릴리스까지 갈 수 있다.

---

## 1. 필수 준비물

| 항목 | 버전 | 비고 |
| --- | --- | --- |
| JDK | **17 또는 21** | Gradle 8.7이 JDK 22까지만 지원. JDK 25면 빌드 실패 |
| Android SDK | **API 36 플랫폼** (`compileSdk = 36`) | Android Studio SDK Manager에서 "Android 16 (API 36)" 설치 |
| Android Studio | Ladybug 이상 권장 | CLI(gradlew)만으로도 빌드는 가능 |
| Git | - | - |

Gradle 자체는 설치할 필요 없다 — `gradlew` 래퍼가 8.7을 자동으로 받는다 (`gradle/wrapper/gradle-wrapper.properties`).

빌드 구성: **AGP 8.6.1 / Gradle 8.7 / Kotlin 1.9.0 / Compose BOM 2024.04.01**, minSdk 26 · target/compileSdk 36. 의존성은 전부 `gradle/libs.versions.toml`(버전 카탈로그)에서 관리한다.

---

## 2. 클론 직후 만들어야 하는 파일

```powershell
git clone <repo-url> ChildrenWayfinding
cd ChildrenWayfinding
```

아래 3개는 **git에 없다**. `.gitignore`에 `local.properties` / `upload-keystore.jks` / `keystore.properties`가 들어 있기 때문이다. (`gradle.properties`는 비공개 저장소라 커밋해 두었으므로 만들 필요 없다 — 대신 2-1의 한 줄만 확인할 것.)

### 2-1. `gradle.properties` — 커밋돼 있음, JDK 경로 한 줄만 확인

파일 자체는 저장소에 있다. 다만 그 안의 **`org.gradle.java.home` 은 머신마다 다르다.** 새 PC의 실제 JDK 17/21 경로로 바꾸거나, 시스템 `JAVA_HOME`이 이미 17/21이면 그 줄을 지운다. 존재하지 않는 경로면 Gradle이 즉시 실패한다.

- Windows: `C:/Program Files/Java/jdk-17.0.19` (역슬래시 대신 슬래시 `/`)
- macOS: `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`
- Linux: `/usr/lib/jvm/temurin-17-jdk-amd64`

내 PC 경로가 남에게 강요되지 않게 하려면, 그 줄을 지우고 각자 `JAVA_HOME`을 17/21로 맞추는 방식도 가능하다. 다만 이 줄을 지우면 시스템 JDK가 22+인 환경에서는 매번 `JAVA_HOME`을 세션마다 지정해야 한다(3장 참고).

> 커밋된 파일이라 **경로를 바꿨다면 커밋하지 말 것.** 로컬만 바꿔 쓰려면 `git update-index --skip-worktree gradle.properties`로 변경을 무시시킬 수 있다.

### 2-2. `local.properties` (필수 — Android SDK 위치)

Android Studio로 프로젝트를 한 번 열면 자동 생성된다. CLI만 쓸 거면 직접 만든다.

```properties
# Windows — 역슬래시와 콜론을 이스케이프한다
sdk.dir=D\:\\dev\\androidsdk
```

```properties
# macOS / Linux
sdk.dir=/Users/<user>/Library/Android/sdk
```

`ANDROID_HOME` 환경변수가 잡혀 있으면 이 파일 없이도 빌드되지만, 명시하는 쪽이 안전하다.

### 2-3. `keystore.properties` + `upload-keystore.jks` (릴리스 서명용 — 디버그 빌드엔 불필요)

없어도 `assembleDebug`는 정상 동작하고, `assembleRelease`는 **미서명 APK**로 나온다 (`app/build.gradle.kts`가 파일 존재 여부를 검사해 분기).

Play Console에 올리려면 **기존 업로드 키스토어가 반드시 필요하다** — 새로 만들면 기존 앱 업데이트가 거부된다. 기존 PC에서 `upload-keystore.jks`를 안전한 경로(암호화 USB·비밀번호 관리자 첨부 등)로 옮겨 루트에 두고, 아래 파일을 만든다.

```properties
storeFile=upload-keystore.jks
storePassword=<비밀번호>
keyPassword=<비밀번호>
keyAlias=<별칭>
```

`storeFile`은 **루트 기준 상대 경로**로 해석된다 (`rootProject.file(...)`).

> 🔑 키스토어를 분실하면 같은 패키지명(`com.rts.rys.ryy.wayfinding`)으로 업데이트를 못 올린다. Play App Signing에 등록돼 있다면 Play Console에서 업로드 키 재설정을 요청할 수 있다. 클론 환경에는 **복사해 오는 것이 원칙**이고, 절대 git에 커밋하지 않는다.

---

## 3. 빌드 확인

```powershell
# 시스템 기본 JAVA_HOME이 17/21이 아니면 세션에서 덮어쓴다
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.19'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

.\gradlew.bat assembleDebug           # 디버그 APK
.\gradlew.bat assembleRelease         # 릴리스 APK (keystore.properties 있으면 서명)
.\gradlew.bat bundleRelease           # Play 업로드용 AAB
.\gradlew.bat test                    # JVM 단위 테스트
.\gradlew.bat connectedAndroidTest    # 계측 테스트 (기기/에뮬 필요)
```

macOS / Linux:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./gradlew assembleDebug
```

산출물:
- 디버그 APK — `app/build/outputs/apk/debug/app-debug.apk`
- 릴리스 APK — `app/build/outputs/apk/release/app-release.apk`
- 릴리스 AAB — `app/build/outputs/bundle/release/app-release.aab`

기기 설치: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

테스트는 현재 스캐폴드(`ExampleUnitTest`, `ExampleInstrumentedTest`)만 있어 실질 커버리지가 없다. `test` 통과가 곧 동작 보증은 아니니 실기기/에뮬 확인이 필요하다.

---

## 4. 자주 겪는 오류

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `Unsupported class file major version` / `Unsupported Java` | JDK 22+ 로 빌드 | JDK 17 또는 21 지정 (`JAVA_HOME` 또는 `org.gradle.java.home`) |
| `Configuration ... AndroidX ... android.useAndroidX` 에러 | `gradle.properties` 유실 | 저장소에 커밋돼 있다 — `git checkout gradle.properties`로 복구 |
| `SDK location not found` | `local.properties` 없음 / `ANDROID_HOME` 미설정 | 2-2 대로 파일 생성 |
| `Failed to find target with hash string 'android-36'` | API 36 플랫폼 미설치 | SDK Manager에서 Android 16 (API 36) 설치 |
| `Value 'C:\...' ... invalid` (java.home) | 경로에 이스케이프 안 된 역슬래시 | `/` 슬래시로 표기 |
| compileSdk 36 미지원 경고 | AGP 8.6.1 지원 목록 미포함 | 정상 — `android.suppressUnsupportedCompileSdk=36`으로 억제됨 |
| 릴리스 APK가 미서명 | `keystore.properties` 없음 | 2-3 대로 키스토어 배치 |
| 빌드가 계속 이상함 | 캐시 꼬임 | `.\gradlew.bat clean` 후 재빌드, 그래도 안 되면 `.gradle/` 삭제 |

---

## 5. 릴리스 절차

1. `app/build.gradle.kts`에서 `versionCode`(정수 +1)와 `versionName`을 **같이** 올린다.
2. `docs/RELEASE_NOTES.md` 맨 위에 새 버전 항목 추가 — **사용자 대상 문구**(Play Console "새로운 기능", 언어별 500자 제한)와 **기술 요약**을 나눠 작성.
3. `.\gradlew.bat bundleRelease` (또는 `assembleRelease`)로 서명 산출물 생성.
4. Play Console 업로드. 데이터 안전 양식은 `docs/DATA_SAFETY.md`, 스토어 문구는 `STORE_LISTING.md`, 개인정보 처리방침은 `PRIVACY_POLICY.md` 참조.
5. 스토어 그래픽이 필요하면 `scripts/gen_play_icon.py`, `scripts/gen_play_feature_graphic.py` (산출물은 `market/`).

현재 출시 버전은 `app/build.gradle.kts`의 `versionCode`/`versionName`이 기준 (작성 시점 2.3.0 / code 12).

---

## 6. 새 환경으로 옮길 때 체크리스트

- [ ] JDK 17 또는 21 설치, 경로 확인
- [ ] Android SDK API 36 플랫폼 설치
- [ ] `gradle.properties`의 `org.gradle.java.home`을 새 PC 경로로 수정 (커밋하지 말 것)
- [ ] `local.properties` 생성 (`sdk.dir`)
- [ ] `upload-keystore.jks` 복사 + `keystore.properties` 생성 (릴리스가 필요할 때만)
- [ ] `.\gradlew.bat assembleDebug` 성공 확인
- [ ] 실기기 설치 후 홈 화면 진입·게임 1판 확인 (센서/사운드는 에뮬에서 확인 어려움)

> 💡 `gradle.properties`는 비밀정보가 없어 **비공개 저장소 전제로 커밋**해 두었다 (2.3.0 이후). 따라서 새 클론에서 손댈 설정은 `local.properties`와 (릴리스 시) 키스토어뿐이다.
