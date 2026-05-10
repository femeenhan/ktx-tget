# KTX 취소표 자동 매크로 앱 — 기술 스택 문서

## 1. 프로젝트 개요

코레일 앱의 열차 조회 화면을 **자동 새로고침**하다가 `매진` 버튼이 **가격 정보**(예: `23,700원`)로 전환되는 순간을 감지하여 자동 탭 → `예매` 버튼 탭까지 진행하는 Android 매크로 앱.

- 사용자는 코레일에서 **출발·도착·시간·인원·열차종별(KTX 등) 필터**까지 직접 설정한 뒤 열차 조회 화면에 둔 상태에서 매크로를 시작한다.
- 매크로 앱은 코레일의 필터를 바꾸지 않고, **현재 화면에 그려진 열차 행**에 대해 **추가 2차 필터**(제외 열차번호·자유석 등)를 적용한 뒤, 조건을 통과한 행에서만 가격 버튼을 누른다.

> ⚠️ **중요**: 이 앱은 Android **접근성 서비스(AccessibilityService)** 를 핵심 기반으로 동작합니다. 별도 루팅이나 ADB 없이 일반 사용자도 설치·사용 가능합니다.

---

## 2. 핵심 동작 흐름

```
사용자가 코레일 앱에서 열차 조회 화면 진입 (KTX 등 원하는 조건 반영)
        ↓
[매크로 앱] AccessibilityService 활성화
        ↓
일정 주기(예: 1~3초)로 새로고침 버튼 자동 탭
        ↓
화면 UI 트리에서 '현재 보이는' 열차 행 단위로 수집
        ↓
DataStore에 저장한 2차 필터 적용 (제외 열차·자유석/입석 등)
        ↓
필터를 통과한 행에서만 '매진' → '가격(원)' 패턴으로 변경 감지
        ↓
해당 행의 가격 버튼 자동 탭
        ↓
'예매' 버튼 자동 탭
        ↓
'승차권 정보 확인' 화면 전환 감지 (typeWindowStateChanged)
        ↓
Push 알림 + 진동 + 알람음 동시 발송
        ↓
매크로 즉시 중단 → 사용자 직접 결제 조작
```

---

## 3. 스코프 및 2차 필터링

### 3.1 처리 범위: 화면에 보이는 열차 목록만

다음은 **의도적으로 범위 밖**으로 둔다.

- 목록 전체를 스크롤하며 모든 행을 스캔하지 않음.
- **지금 Accessibility 트리에 노출된(화면에 보이는 구간의) 행만** 필터링·자동 탭 대상으로 삼음.

`RecyclerView` 가상화로 화면 밖 행은 트리에 없을 수 있으므로, 사용자가 **보고 싶은 구간을 스크롤해 둔 상태**에서 매크로를 돌리는 사용 방식을 전제로 한다.

### 3.2 사용자 옵션 (매크로 앱 설정)

DataStore 등에 저장하여 적용한다.

| 옵션 (예시) | 설명 |
|---------------|------|
| 제외 열차번호 목록 | 지정한 번호 행은 가격이 떠도 클릭하지 않음 |
| 자유석 행 제외 | 해당 문구가 보이는 행은 클릭 후보에서 제외 |
| 입석+좌석 예약대기 등 제외 | 부분 문자열 매칭으로 지정 상태 행 제외 |
| 일반실만 / 특·우등만 (선택) | 클릭할 열(칸)을 제한 |

### 3.3 도메인 처리 순서 (권장)

1. **행 식별**: 트리에서 열차 한 줄(또는 동일 행에 묶이는 노드들)을 그룹으로 식별한다.  
2. **열차번호·좌석 칸 텍스트**를 행 단위로 읽는다.  
3. **제외 열차번호·자유석/입석 등** 옵션과 비교해 행을 걸러낸다.  
4. 통과한 행에서만 `매진` → 가격 정규식(`\d{1,3}(,\d{3})*원`)에 해당하는 클릭 가능 노드를 탭한다.  
5. **`예매`** 노드를 찾아 탭한다.

순수 Kotlin 함수로 “행 단위 메타데이터 + 필터 결과”를 분리해 두면 단위 테스트로 규칙만 검증하기 쉽다.

### 3.4 한계 (명시)

| 항목 | 설명 |
|------|------|
| 가상화 목록 | 보이지 않는 행은 접근 불가 — 사용자 스크롤 위치에 의존 |
| 코레일 앱 변경 | 뷰 계층·문구 변경 시 행 묶기·텍스트 파싱 로직 수정 필요 |
| 스토어·약관 | Play 정책·코레일 이용약관상 자동화 제한 가능 — 배포·법적 검토 별도 |

---

## 4. 기술 스택

### 4.1 언어 및 플랫폼

| 항목 | 선택 | 비고 |
|------|------|------|
| 언어 | **Kotlin** | 100% Kotlin, Java 혼용 불필요 |
| 플랫폼 | Android (minSdk 26 / targetSdk 35) | Android 8.0+ 지원 |
| 빌드 시스템 | **Gradle (Kotlin DSL)** | `build.gradle.kts` |

---

### 4.2 핵심 Android API

#### ① AccessibilityService (필수)
- `android.accessibilityservice.AccessibilityService`
- 화면의 UI 요소(버튼, 텍스트 등) 를 **루팅 없이 읽고 탭** 가능
- `AccessibilityNodeInfo`로 특정 텍스트("매진", "원") 노드 탐색
- **가격 클릭은 반드시 행 단위 필터 적용 후** 해당 행의 노드에 대해 `performAction(ACTION_CLICK)` 수행
- 화면 캡처·OCR 위주 방식보다 트리 기반이 안정적 (접근성으로 읽을 수 있을 때 우선)

```kotlin
// 핵심 사용 예시
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    val root = rootInActiveWindow ?: return
    val soldOutNodes = root.findAccessibilityNodeInfosByText("매진")
    // 행 그룹화·필터·가격 탐지 로직 수행
}
```

#### ② UiAutomator2 (보조 — 개발/테스트용)
- `androidx.test.uiautomator:uiautomator`
- 개발 중 UI 구조 분석 및 자동화 테스트에 활용
- 실제 배포 앱에는 포함하지 않음 (본 로직은 Accessibility에 둔다)

---

### 4.3 UI 프레임워크

| 항목 | 선택 |
|------|------|
| UI | **Jetpack Compose** |
| 테마 | Material 3 (MaterialTheme) |
| 내비게이션 | Compose Navigation |

매크로 앱 자체 UI는 최소화:

- 매크로 ON/OFF 토글
- 새로고침 주기 설정 슬라이더 (1초 ~ 5초)
- **2차 필터**: 제외 열차번호, 자유석 제외, 입석+좌석 등 제외, (선택) 일반실/특실 클릭 대상
- 알림·진동 ON/OFF (승차권 확인 이후 알림용)
- 실시간 상태 로그 표시

**선택**: 코레일 위에서 시작/중지만 할 **플로팅 오버레이**(`TYPE_APPLICATION_OVERLAY`, `WindowManager` 또는 Bubble) — 이 경우 `SYSTEM_ALERT_WINDOW` 권한 안내 필요.

---

### 4.4 백그라운드 처리

| 항목 | 선택 | 이유 |
|------|------|------|
| 포그라운드 서비스 | **ForegroundService** (권장) | 앱이 백그라운드로 가도 매크로 유지·시스템에 안정적으로 살아 있음 |
| 단순 구성 | 접근성 서비스만 + 코레일 포그라운드 | 장시간 백그라운드 매크로가 아니면 최소 구성 가능 (요구사항에 맞게 선택) |
| 비동기 처리 | **Kotlin Coroutines + Flow** | 주기적 새로고침 타이머 구현 |
| 스케줄러 | `delay()` + `while(isActive)` 루프 | 단순·안정적인 폴링 구조 |
| 취소 | `SupervisorJob` / 전용 `CoroutineScope` | 서비스 종료 시 코루틴 정리 |

```kotlin
// 새로고침 루프 예시
CoroutineScope(Dispatchers.Default).launch {
    while (isActive) {
        performRefresh()
        delay(refreshIntervalMs)
    }
}
```

---

### 4.5 데이터 저장

| 항목 | 선택 |
|------|------|
| 설정 저장 | **DataStore (Preferences)** |
| 저장 항목 | 새로고침 주기, **2차 필터 전체**, 마지막 검색 조건(선택), 알림 여부 |

---

### 4.6 알림

| 항목 | 선택 |
|------|------|
| 알림 API | **NotificationManager + NotificationChannel** |
| 용도 1 | 포그라운드 서비스 상태 알림 ("매크로 실행 중") |
| 용도 2 | 취소표 감지·승차권 확인 단계에서 진동 + 소리 알림 |
| 진동 | `VibrationEffect` (API 26+) |

---

### 4.7 권한

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<!-- 오버레이 UI 사용 시 -->
<!-- <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" /> -->
```

---

### 4.8 주요 라이브러리

| 라이브러리 | 버전 | 용도 |
|------------|------|------|
| Jetpack Compose BOM | 2024.xx | UI |
| Kotlin Coroutines | 1.8.x | 비동기 처리 |
| DataStore Preferences | 1.1.x | 설정 저장 |
| Hilt (선택) | 2.51.x | 의존성 주입 (서비스 규모 시) |
| Timber | 5.x | 디버그 로깅 |

---

## 5. 프로젝트 구조

```
app/
├── src/main/
│   ├── kotlin/com.ktxmacro/
│   │   ├── service/
│   │   │   ├── KtxAccessibilityService.kt   # 핵심: UI 감지 & 탭 자동화
│   │   │   └── MacroForegroundService.kt     # 백그라운드 유지 (도입 시)
│   │   ├── domain/
│   │   │   ├── TrainRowParser.kt             # 행 단위 텍스트 수집
│   │   │   └── MacroFilterEngine.kt          # 2차 필터·가격 후보 판정
│   │   ├── ui/
│   │   │   ├── MainScreen.kt                # Compose 메인 화면
│   │   │   └── LogScreen.kt                 # 실시간 상태 로그
│   │   ├── data/
│   │   │   └── SettingsRepository.kt        # DataStore 설정
│   │   └── util/
│   │       ├── NodeFinder.kt                # AccessibilityNode 탐색 유틸
│   │       └── NotificationHelper.kt        # 알림 유틸
│   └── res/
│       └── xml/
│           └── accessibility_service_config.xml  # 접근성 서비스 설정
```

---

## 6. AccessibilityService 설정 파일

```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:packageNames="kr.co.korail.mobile"
    android:notificationTimeout="100"
    android:description="@string/accessibility_service_description" />
```

> `packageNames`에 코레일 앱 패키지명(`kr.co.korail.mobile`)을 명시하여 해당 앱 이벤트만 수신

---

## 7. '승차권 정보 확인' 화면 감지 및 알림

`예매` 버튼 탭 이후 코레일 앱이 **승차권 정보 확인** 화면으로 전환되는 순간을 `AccessibilityService`의 `typeWindowStateChanged` 이벤트로 감지하여 즉시 사용자에게 알립니다.

### 감지 방법

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        val root = rootInActiveWindow ?: return
        // '승차권 정보 확인' 텍스트 노드 존재 여부로 화면 전환 판단
        val confirmed = root.findAccessibilityNodeInfosByText("승차권 정보 확인")
        if (confirmed.isNotEmpty()) {
            triggerUserAlert()
            stopMacro()  // 매크로 즉시 중단, 이후는 사용자 직접 조작
        }
    }
}
```

### 알림 전략 (다중 채널 동시 발송)

사용자가 화면을 보지 않고 있을 수 있으므로 **3가지 알림을 동시에** 발송합니다.

| 알림 수단 | 구현 | 비고 |
|-----------|------|------|
| 상단 알림(Push) | `NotificationManager` — 높은 우선순위(`IMPORTANCE_HIGH`) | 헤드업 알림으로 즉시 표시 |
| 진동 | `VibrationEffect.createWaveform()` — 강한 패턴 반복 | 무음 모드에서도 인지 가능 |
| 소리 | `RingtoneManager.TYPE_ALARM` | 알람음으로 확실한 인지 |

```kotlin
fun triggerUserAlert() {
    // 1. 헤드업 Push 알림
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("🚆 KTX 예매 진행 중!")
        .setContentText("승차권 정보 확인 화면입니다. 지금 바로 결제를 완료하세요!")
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(ALERT_NOTIFICATION_ID, notification)

    // 2. 진동 (0.5초 간격 3회 반복)
    val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))

    // 3. 알람 소리
    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    RingtoneManager.getRingtone(applicationContext, alarmUri).play()
}
```

---

## 8. 취소표 감지 로직 핵심

트리 전체에서 **첫 번째 가격 노드만** 찾으면, 필터에서 제외해야 할 행의 가격을 잘못 누를 수 있다. **반드시 행 단위로 묶은 뒤 `MacroFilterEngine` 등으로 필터 통과 행의 가격 노드만** 클릭한다.

아래는 **개념용** 단순 순회 예시이며, 실제 구현은 `TrainRowParser` + 필터 결과와 결합한다.

```kotlin
fun findPriceNodeInRow(rowRoot: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    val pricePattern = Regex("\\d{1,3}(,\\d{3})*원")
    // rowRoot 하위만 순회하여 클릭 가능한 가격 텍스트 노드 반환
    fun walk(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        if (pricePattern.containsMatchIn(text) && node.isClickable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            walk(child)?.let { return it }
        }
        return null
    }
    return walk(rowRoot)
}
```

---

## 9. 사용자 설치 및 설정 흐름

1. APK 설치 (또는 Play Store 배포)
2. 앱 실행 → **접근성 권한 허용** 안내 화면
3. 설정 → 접근성 → KTX 매크로 → 활성화
4. 앱으로 돌아와 새로고침 주기·**2차 필터** 설정
5. 코레일 앱에서 열차 조회 화면 진입 (원하는 열차가 **화면에 보이도록** 스크롤)
6. 매크로 앱에서 **START** 버튼 탭
7. 취소표 감지 시 (필터 통과 행만) 자동 탭 + 진동 알림
8. `예매` 버튼 자동 탭
9. **승차권 정보 확인** 화면 전환 감지 → Push 알림 + 진동 + 알람음 발송
10. 사용자가 알림 확인 후 직접 결제 진행

---

## 10. 주의사항 및 제약

| 항목 | 내용 |
|------|------|
| 코레일 앱 업데이트 | UI 구조 변경 시 NodeFinder·행 파서 로직 재조정 필요 |
| 앱 배터리 최적화 | 사용자가 배터리 최적화 예외 설정 필요 |
| 네트워크 속도 | 새로고침 주기는 네트워크 응답 시간보다 길게 설정 권장 |
| PlayStore 정책 | 접근성 서비스 앱은 심사 기준이 엄격함 — 직접 배포(APK) 권장 |
| 법적 이슈 | 코레일 앱 이용약관 검토 필요 (자동화 도구 사용 제한 여부 확인) |

---

## 11. 개발 로드맵 (단계별)

| 단계 | 작업 | 예상 기간 |
|------|------|-----------|
| Phase 1 | AccessibilityService 기본 구조 + UI 노드 탐색 | 1주 |
| Phase 2 | 새로고침 자동화 + **행 단위 파싱** + 매진/가격 감지 + **2차 필터** | 1주 |
| Phase 3 | 포그라운드 서비스 + 알림 | 3일 |
| Phase 4 | Compose UI (설정·필터 화면, 로그) | 3일 |
| Phase 5 | 테스트 및 안정화 | 1주 |
