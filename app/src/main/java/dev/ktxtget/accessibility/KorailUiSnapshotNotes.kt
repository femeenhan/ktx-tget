package dev.ktxtget.accessibility

/**
 * Single reference for Korail UI structure: product strings and Layout Inspector outputs.
 * Update [VIEW_ID_] constants after capturing resource-ids on a device (Phase 1 / iteration).
 */
object KorailUiSnapshotNotes {
    /** Legacy 코레일 모바일 (일반 조회 앱). */
    const val PACKAGE_KORAIL_MOBILE: String = "kr.co.korail.mobile"
    /** 코레일톡 (주 사용처). */
    const val PACKAGE_KORAIL_TALK: String = "com.korail.talk"
    /**
     * Seat cells (매진/원) whose vertical centers differ by at most this many px are one visual row.
     * 코레일톡·Compose 등 RecyclerView 직계 자식이 없을 때 좌표 클러스터링에 사용.
     */
    const val SEAT_ROW_CLUSTER_THRESHOLD_PX: Int = 56

    val KORAIL_TARGET_PACKAGES: Set<String> = setOf(
        PACKAGE_KORAIL_MOBILE,
        PACKAGE_KORAIL_TALK,
    )

    fun isTargetPackage(packageName: CharSequence?): Boolean {
        val name: String = packageName?.toString() ?: return false
        return name in KORAIL_TARGET_PACKAGES
    }
    /** Shown on the flow after tapping **예매** (accessibility tree title or text probe). */
    const val TICKET_CONFIRMATION_TITLE: String = "승차권 정보 확인"
    /**
     * Intermediate-stop notice body often contains one of these (e.g. "OOO을 정차하는 열차입니다").
     * Tune after capturing accessibility trees if 코레일 wording changes.
     */
    val INTERMEDIATE_STOP_DIALOG_TEXT_MARKERS: List<String> = listOf(
        "정차하는",
    )
    /** Primary action on the intermediate-stop notice dialog. */
    const val INTERMEDIATE_STOP_CONFIRM_LABEL: String = "확인"
    /** Bottom sheet primary action label (Phase 2 tap target). */
    const val RESERVE_BUTTON_LABEL: String = "예매"
    /** Cell label before a seat opens (sold out). */
    const val SOLD_OUT_LABEL: String = "매진"
    /**
     * Price pattern for Phase 2 — document regex in domain layer later; kept here for discovery alignment.
     */
    const val PRICE_SAMPLE_PATTERN: String = "\\d{1,3}(,\\d{3})*원"
    /** Recycler row or row wrapper: set from Layout Inspector (train list). */
    const val VIEW_ID_TRAIN_ROW_ROOT: String = ""
    /** Tap target when view-id lookup fails (Korail often uses this label). */
    const val REFRESH_TEXT_HINT: String = "새로고침"
    /** List screen refresh control. */
    const val VIEW_ID_REFRESH: String = ""
    /** General / first-class price cell (if distinct ids). */
    const val VIEW_ID_PRICE_GENERAL: String = ""
    const val VIEW_ID_PRICE_FIRST: String = ""
    /** Bottom sheet root or **예매** button when stable ids exist. */
    const val VIEW_ID_BOTTOM_SHEET_ROOT: String = ""
    const val VIEW_ID_RESERVE_BUTTON: String = ""
}
