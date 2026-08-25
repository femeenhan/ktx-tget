package dev.ktxtget.accessibility

/**
 * 코레일 앱 화면에 대한 사실 기록. 실기(SM-F731N / 코레일톡 7.0.2, 2026-08) 접근성 덤프 기준이다.
 *
 * ## 리뉴얼로 달라진 전제
 * 코레일톡 7.x는 Jetpack Compose로 재작성되면서 두 가지가 바뀌었다.
 *
 * 1. **`resource-id`가 트리 전체에 하나도 없다.** 예전에 두었던 `VIEW_ID_*` 상수는 채울 방법이
 *    없어져 전부 제거했다. 노드는 라벨(text/contentDescription)과 좌표로만 찾는다.
 * 2. **열차 목록 행이 텍스트를 전혀 노출하지 않는다.** 각 행은
 *    `Button text='' content-desc='' NAF=true` 인 빈 리프다. TalkBack을 켜고
 *    touch exploration까지 활성화해도 동일했다(코레일톡 쪽 접근성 회귀로 보인다).
 *    그래서 좌석 상태는 화면 캡처 픽셀로 읽고([dev.ktxtget.domain.SoldOutPixelClassifier]),
 *    정확한 판단은 행을 탭해 연 바텀시트 텍스트로 한다([dev.ktxtget.domain.BookingSheet]).
 *
 * 헤더·바텀시트·홈 화면은 정상적으로 텍스트를 노출한다.
 * 문구 상수는 [dev.ktxtget.domain.KorailLabels]에 모아 두었다.
 */
object KorailUiSnapshotNotes {
    /** 코레일톡 (주 사용처). */
    const val PACKAGE_KORAIL_TALK: String = "com.korail.talk"

    /** 구 코레일 모바일. 아직 남아 있는 사용자를 위해 대상에 둔다. */
    const val PACKAGE_KORAIL_MOBILE: String = "kr.co.korail.mobile"

    val KORAIL_TARGET_PACKAGES: Set<String> = setOf(
        PACKAGE_KORAIL_TALK,
        PACKAGE_KORAIL_MOBILE,
    )

    fun isTargetPackage(packageName: CharSequence?): Boolean {
        val name: String = packageName?.toString() ?: return false
        return name in KORAIL_TARGET_PACKAGES
    }

    /** 예매 완료 직전 화면. 여기 닿으면 알림을 띄우고 매크로를 멈춘다. */
    const val TICKET_CONFIRMATION_TITLE: String = "승차권 정보 확인"

    /** 중간 정차역 안내 팝업 본문에 들어가는 문구. */
    val INTERMEDIATE_STOP_DIALOG_TEXT_MARKERS: List<String> = listOf(
        "정차하는",
    )

    /** 안내 팝업의 기본 동작 버튼. */
    const val CONFIRM_LABEL: String = "확인"

    /**
     * 새로고침을 누르면 목록 전체가 이 자리표시자로 잠깐 교체된다. 이때는 행이 하나도 없으므로
     * 고정 시간을 기다리지 말고 행이 돌아올 때까지 폴링해야 한다 (실기에서 확인한 실패 원인).
     */
    const val LIST_LOADING_MARKER: String = "목록을 불러오고 있어요"

    /**
     * 목록 행으로 인정할 최소 높이(px). 스크롤 경계에서 잘린 행은 픽셀 판정이 어긋나므로 버린다.
     * 실기 기준 정상 행 높이는 282px이다.
     */
    const val MIN_ROW_HEIGHT_PX: Int = 160
}
