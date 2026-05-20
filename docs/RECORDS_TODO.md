# 기록 보기 기능 강화 TODO

현재 `RecordsScreen` / `RecordsRepository` / `GameRecord` 의 한계를 정리하고, 강화 작업과 추가 기능 아이디어를 모은 문서입니다.

---

## 1. 현재 상태 요약

- **데이터 모델 (`GameRecord`)**: `stageId`, `stageName`, `elapsedMs`, `timestamp` 4개 필드만 보관.
- **저장소 (`RecordsRepository`)**: `SharedPreferences` + `JSONArray` 기반. `load / add / clear` 만 존재. 정렬은 최신순(timestamp 내림차순).
- **화면 (`RecordsScreen`)**: 카드 형태로 스테이지번호 / 이름 / 날짜 / 걸린시간만 보여주는 단순 리스트. 빈 상태 메시지만 존재.
- **별점 (`MazePar.starsFor`)**: 결과 화면(`ResultScreen`)에서만 계산되며 **저장되지 않음**.
- **최고 기록**: `ResultScreen` 진입 시점에 같은 stageId의 최소 elapsedMs 만 한 번 비교.

---

## 2. 기록 보기 강화 — 해야 할 일

### 2.1 데이터 모델 확장
- [ ] `GameRecord`에 다음 필드 추가
  - `earnedStars: Int` — 현재 결과 화면에서 계산만 하고 버리는 별점을 영구 저장
  - `level: Int` — 난이도 필터링/통계용 (현재 `Stages.byId(stageId).level` 로 매번 조회)
  - `isCustom: Boolean` — 기본 스테이지 vs 사용자 제작 스테이지 구분 (현재 stageId 만으로는 식별 불가)
  - `mistakes: Int` 또는 `steps: Int` (선택) — 별점 평가가 시간 외 지표로 확장될 때 사용
- [ ] 기존 저장 데이터와의 호환성 처리 (없는 필드는 기본값으로 로드)

### 2.2 저장소(`RecordsRepository`) 확장
- [ ] `delete(record)` 또는 `deleteAt(index)` — 개별 기록 삭제
- [ ] `deleteByStage(stageId)` — 특정 스테이지의 기록만 일괄 삭제
- [ ] `bestByStage(stageId): GameRecord?` — 스테이지별 최고 기록 조회 (현재는 화면에서 매번 filter+min)
- [ ] `summary(): RecordsSummary` — 총 플레이 횟수, 누적 별, 평균 시간 등 집계
- [ ] **보관 한도 정책** — 기록이 무한히 쌓이지 않도록 stage별 최근 N개 또는 전체 최대치 (예: 500개) 제한
- [ ] (선택) SharedPreferences → DataStore 또는 Room 마이그레이션 검토
  - 현재 모든 기록을 통째로 JSON 문자열로 직렬화 → 기록이 늘어나면 add 비용 O(n)
  - Room으로 가면 정렬/필터/집계 쿼리가 쉬워짐

### 2.3 화면(`RecordsScreen`) UX 강화
- [ ] **요약 헤더 카드** — 화면 상단에 총 플레이 횟수 / 누적 별 / 최고 기록 스테이지 표시
- [ ] **정렬 선택** — 최신순 / 빠른시간순 / 별점순 / 스테이지순 (토글 또는 segmented control)
- [ ] **난이도 필터** — 전체 / 난이도1 / 난이도2 / 난이도3 / 난이도4 / 커스텀
- [ ] **스테이지별 그룹/접기** — 같은 stageId를 묶고 펼치면 시도 이력이 나오는 형태 (옵션)
- [ ] **카드에 별점 배지** 추가 — 현재 시간만 강조됨
- [ ] **"최고 기록" 뱃지** — 해당 스테이지의 베스트인 항목에 왕관/별 표시
- [ ] **개별 항목 스와이프 삭제** 또는 길게 눌러 삭제
- [ ] **전체 삭제 버튼** (이미 repo에 `clear()` 존재) — 상단 우측 + 확인 다이얼로그
- [ ] **빈 상태 CTA** — "게임 시작하기" 버튼을 빈 상태에 노출
- [ ] **화면 복귀 시 갱신** — 현재 `LaunchedEffect(Unit)`이라 게임 후 돌아와도 새 기록 반영 안 됨. `DisposableEffect` 또는 SnapshotState 기반으로 리로드 또는 `RecordsRepository`를 State 보유형으로 전환
- [ ] **스테이지 상세 화면** — 카드 탭 시 해당 스테이지의 모든 시도 리스트 + 시간 추이 그래프

### 2.4 결과 화면(`ResultScreen`) 연동
- [ ] 저장 시 `earnedStars`, `level`, `isCustom` 함께 기록
- [ ] "최고 기록" 비교를 `RecordsRepository.bestByStage()` 로 위임 (현재는 화면에서 filter+min)
- [ ] (선택) 직전 기록과의 차이 표시 — "이전보다 3초 빨라졌어요!"

---

## 3. 추가하면 좋을 기능 (Nice-to-have)

### 3.1 시각화 / 동기 부여
- **시간 추이 그래프** — 스테이지 상세 화면에서 시도별 elapsedMs 라인 차트, 자기 자신과의 경쟁
- **잔디(히트맵) 캘린더** — 날짜별 플레이 횟수를 GitHub 잔디 스타일로. 매일 하는 습관 만들기
- **이번 주 / 이번 달 요약** — 홈 화면 또는 기록 상단에 "이번 주 5번 플레이 했어요!" 노출
- **난이도별 진행률 막대** — 난이도1 6/6 클리어, 난이도2 2/3 별점 만점 같은 진척 표시

### 3.2 배지 / 도전 과제
- **배지 시스템** — 첫 클리어, 3별 5개 달성, 7일 연속 플레이, 모든 난이도 1단계 클리어 등
- **도전 과제 화면** — 진행 중/완료된 과제 목록. 기록 보기와 분리된 별도 탭

### 3.3 공유 / 백업
- **결과 공유** — 최고 기록 스크린샷을 카카오톡/사진첩으로 저장. 별점 + 시간 + 스테이지 이름 디자인된 이미지 생성
- **기록 내보내기/가져오기** — JSON 파일로 백업, 기기 변경 시 복원 (Storage Access Framework)
- **클라우드 동기화** — Google 계정 기반 (선택, 권한/개인정보 이슈로 신중하게)

### 3.4 부모/관찰자 시점
- **부모용 요약** — 자녀가 얼마나 자주, 얼마나 오래 플레이했는지 보기 좋은 주간/월간 리포트
- **PIN 잠금 옵션** — 기록 삭제나 설정 변경을 부모만 가능하게

### 3.5 게임 자체와의 연결
- **미로 미니맵 썸네일** — 카드에 그 스테이지의 미로 미니어처를 곁들이면 어떤 길이었는지 즉시 인지
- **"다시 도전" 버튼** — 기록 카드에서 바로 해당 스테이지로 진입
- **첫 시도 vs 최근 vs 최고 비교 카드** — 같은 스테이지에서 얼마나 늘었는지 한눈에

### 3.6 데이터/품질
- **분석 이벤트(선택)** — 어떤 스테이지에서 가장 많이 포기/재시도하는지 (로컬 집계만으로도 가능)
- **삭제 복원(Undo)** — 스낵바로 5초간 되돌리기 제공
- **단위 테스트** — `RecordsRepository`의 add/load/clear/migration 동작 테스트 (현재 테스트 없음)

---

## 4. 우선순위 제안

| 순위 | 항목 | 이유 |
|---|---|---|
| 1 | `GameRecord`에 `earnedStars`, `level`, `isCustom` 추가 | 이후 모든 강화의 전제. 호환성 처리만 잘 하면 작업량 작음 |
| 2 | 화면 복귀 시 자동 갱신 | 현재 버그성 동작에 가까움 (게임 후 돌아오면 새 기록이 안 보임) |
| 3 | 카드에 별점 + 최고 기록 뱃지 표시 | 즉각적 시각 개선, 데이터 모델만 확장되면 작업량 작음 |
| 4 | 요약 헤더 카드 (총 플레이 / 누적 별 / 최고) | 동기 부여 효과 큼 |
| 5 | 개별 삭제 + 전체 삭제 UI | 이미 repo에 기능 있음, UI만 붙이면 됨 |
| 6 | 정렬 / 난이도 필터 | 기록이 쌓일수록 가치 큼 |
| 7 | 스테이지 상세 화면 (시도 이력 + 그래프) | 별도 화면 신규 → 작업량 큼, 위의 1~3 끝난 뒤 |
| 8 | 잔디 캘린더 / 배지 / 공유 | 만족도 큰 기능이지만 별도 스프린트 |

---

## 5. 참고 파일 경로

- `app/src/main/java/com/rts/rys/ryy/wayfinding/data/GameRecord.kt`
- `app/src/main/java/com/rts/rys/ryy/wayfinding/data/RecordsRepository.kt`
- `app/src/main/java/com/rts/rys/ryy/wayfinding/ui/RecordsScreen.kt`
- `app/src/main/java/com/rts/rys/ryy/wayfinding/ui/ResultScreen.kt` (저장 호출 지점, 별점 계산 위치)
- `app/src/main/java/com/rts/rys/ryy/wayfinding/game/MazePar.kt` (별점 산정 로직)
- `app/src/main/java/com/rts/rys/ryy/wayfinding/game/Stages.kt` (level, isCustom 출처)
