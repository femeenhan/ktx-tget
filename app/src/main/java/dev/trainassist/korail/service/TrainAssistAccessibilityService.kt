package dev.trainassist.korail.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import dev.trainassist.korail.AutomationPreferences
import dev.trainassist.korail.R

class TrainAssistAccessibilityService : AccessibilityService() {
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private var automationPhase: AutomationPhase = AutomationPhase.MONITORING
    private var lastArmedState: Boolean = false
    private var seenSoldOutSinceArmed: Boolean = false
    private val loopRunnable: Runnable = object : Runnable {
        override fun run() {
            executeAutomationLoop()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        scheduleLoop(0L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!AutomationPreferences.isAutomationArmed(this)) {
            return
        }
        if (automationPhase == AutomationPhase.COMPLETED) {
            return
        }
        scheduleLoop(0L)
    }

    override fun onInterrupt() {
    }

    private fun executeAutomationLoop() {
        mainHandler.removeCallbacks(loopRunnable)
        val armedNow: Boolean = AutomationPreferences.isAutomationArmed(this)
        if (armedNow != lastArmedState) {
            lastArmedState = armedNow
            if (armedNow) {
                resetAutomationState()
            }
        }
        if (!armedNow) {
            return
        }
        if (automationPhase == AutomationPhase.COMPLETED) {
            return
        }
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            scheduleLoop(500L)
            return
        }
        try {
            if (root.packageName == null || root.packageName.toString() != TARGET_PACKAGE) {
                scheduleLoop(1000L)
                return
            }
            when (automationPhase) {
                AutomationPhase.MONITORING -> handleMonitoring(root)
                AutomationPhase.AFTER_PRICE_INFO -> scheduleLoop(400L)
                AutomationPhase.COMPLETED -> return
            }
        } finally {
            root.recycle()
        }
    }

    private fun resetAutomationState() {
        automationPhase = AutomationPhase.MONITORING
        seenSoldOutSinceArmed = false
    }

    private fun handleMonitoring(root: AccessibilityNodeInfo) {
        if (isTextPresentWithQuickFind(root, TEXT_SOLD_OUT)) {
            seenSoldOutSinceArmed = true
        }
        if (seenSoldOutSinceArmed && tryClickLabel(root, TEXT_PRICE_INFO)) {
            automationPhase = AutomationPhase.AFTER_PRICE_INFO
            mainHandler.postDelayed({ tryClickReserveEntry() }, AFTER_CLICK_DELAY_MS)
            scheduleLoop(1200L)
            return
        }
        performRefresh(root)
        scheduleLoop(REFRESH_INTERVAL_MS)
    }

    private fun tryClickReserveEntry() {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            scheduleLoop(500L)
            return
        }
        try {
            if (root.packageName == null || root.packageName.toString() != TARGET_PACKAGE) {
                scheduleLoop(600L)
                return
            }
            if (tryClickLabel(root, TEXT_RESERVE)) {
                completeFlow()
            } else {
                scheduleLoop(600L)
            }
        } finally {
            root.recycle()
        }
    }

    private fun completeFlow() {
        automationPhase = AutomationPhase.COMPLETED
        AutomationPreferences.setAutomationArmed(this, false)
        mainHandler.post {
            Toast.makeText(applicationContext, getString(R.string.automation_completed), Toast.LENGTH_LONG).show()
        }
    }

    private fun scheduleLoop(delayMs: Long) {
        mainHandler.removeCallbacks(loopRunnable)
        mainHandler.postDelayed(loopRunnable, delayMs)
    }

    private fun performRefresh(root: AccessibilityNodeInfo) {
        if (tryClickLabel(root, TEXT_REFRESH)) {
            return
        }
        if (tryClickContaining(root, "새로")) {
            return
        }
        clickByViewIdSubstring(root, "refresh")
    }

    private fun tryClickContaining(root: AccessibilityNodeInfo, substring: String): Boolean {
        val matches: List<AccessibilityNodeInfo> = root.findAccessibilityNodeInfosByText(substring)
        for (node in matches) {
            try {
                val host: AccessibilityNodeInfo = findClickableHost(node) ?: continue
                if (host.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            } finally {
                node.recycle()
            }
        }
        return false
    }

    private fun tryClickLabel(root: AccessibilityNodeInfo, label: String): Boolean {
        return depthFirstClickExactLabel(root, label)
    }

    private fun depthFirstClickExactLabel(node: AccessibilityNodeInfo, label: String): Boolean {
        if (nodeMatchesExactLabel(node, label)) {
            val host: AccessibilityNodeInfo? = findClickableHost(node)
            if (host != null && host.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        val childCount: Int = node.childCount
        var index: Int = 0
        while (index < childCount) {
            val child: AccessibilityNodeInfo? = node.getChild(index)
            if (child != null) {
                try {
                    if (depthFirstClickExactLabel(child, label)) {
                        return true
                    }
                } finally {
                    child.recycle()
                }
            }
            index++
        }
        return false
    }

    private fun clickByViewIdSubstring(node: AccessibilityNodeInfo, needle: String): Boolean {
        val viewId: String? = node.viewIdResourceName
        if (viewId != null && viewId.contains(needle, ignoreCase = true)) {
            val host: AccessibilityNodeInfo? = findClickableHost(node)
            if (host != null && host.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        val count: Int = node.childCount
        var i: Int = 0
        while (i < count) {
            val child: AccessibilityNodeInfo? = node.getChild(i)
            if (child != null) {
                try {
                    if (clickByViewIdSubstring(child, needle)) {
                        return true
                    }
                } finally {
                    child.recycle()
                }
            }
            i++
        }
        return false
    }

    private fun isTextPresentWithQuickFind(root: AccessibilityNodeInfo, needle: String): Boolean {
        val matches: List<AccessibilityNodeInfo> = root.findAccessibilityNodeInfosByText(needle)
        val found: Boolean = matches.isNotEmpty()
        for (node in matches) {
            node.recycle()
        }
        return found
    }

    private fun nodeMatchesExactLabel(node: AccessibilityNodeInfo, label: String): Boolean {
        val text: CharSequence? = node.text
        if (text != null && label == text.toString()) {
            return true
        }
        val description: CharSequence? = node.contentDescription
        return description != null && label == description.toString()
    }

    private fun findClickableHost(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = start
        var hops: Int = 0
        while (current != null && hops < 14) {
            if (current.isClickable) {
                return current
            }
            val parent: AccessibilityNodeInfo? = current.parent
            current = parent
            hops++
        }
        return null
    }

    private enum class AutomationPhase {
        MONITORING,
        AFTER_PRICE_INFO,
        COMPLETED,
    }

    companion object {
        private const val TARGET_PACKAGE: String = "com.korail.talk"
        private const val REFRESH_INTERVAL_MS: Long = 1900L
        private const val AFTER_CLICK_DELAY_MS: Long = 750L
        private const val TEXT_SOLD_OUT: String = "매진"
        private const val TEXT_PRICE_INFO: String = "가격정보"
        private const val TEXT_RESERVE: String = "예매"
        private const val TEXT_REFRESH: String = "새로고침"
    }
}
