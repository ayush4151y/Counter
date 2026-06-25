package com.counter.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.counter.app.ReelAppConfig.ReelAppData
import com.counter.app.data.CounterDatabase
import com.counter.app.data.ReelCount
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class ReelCountingService : AccessibilityService() {

    companion object {
        const val TAG = "ReelCounter"
        const val ACTION_COUNT_UPDATED = "com.counter.app.COUNT_UPDATED"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_COUNT = "extra_count"
        private const val TARGET_EVENTS = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        private const val TEXT_CHANGE_THRESHOLD = 0.85f
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: CounterDatabase
    private lateinit var overlayManager: ReelOverlayManager

    private var screenWidth = 0
    private var screenHeight = 0
    private var totalToday = 0

    private var currentReelPkg: String? = null
    private var hideJob: Job? = null

    private val lastDynamicText = mutableMapOf<String, String>()
    private val recentCaptions = mutableMapOf<String, MutableSet<String>>()
    private val perAppCounts = mutableMapOf<String, Int>()

    override fun onCreate() {
        super.onCreate()
        database = CounterDatabase.getInstance(this)
        overlayManager = ReelOverlayManager(this)
        val displayMetrics = DisplayMetrics()
        val display = (getSystemService(DISPLAY_SERVICE) as? android.hardware.display.DisplayManager)
            ?.getDisplay(0)
        display?.getRealMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        scope.launch {
            val today = getTodayDate()
            for ((pkg, _) in ReelAppConfig.reelData) {
                val saved = database.reelCountDao().getCount(today, pkg)
                perAppCounts[pkg] = saved?.count ?: 0
            }
            totalToday = ReelAppConfig.reelData.keys.sumOf { perAppCounts[it] ?: 0 }
            broadcastUpdate()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || (event.eventType and TARGET_EVENTS) == 0) return

        val pkg = event.packageName?.toString() ?: return
        val config = ReelAppConfig.reelData[pkg] ?: run {
            if (currentReelPkg != null) {
                currentReelPkg = null
                hideJob?.cancel()
                hideJob = scope.launch {
                    delay(1000)
                    overlayManager.hide()
                }
            }
            return
        }
        if ((event.eventType and config.eventType) == 0) return

        val root = rootInActiveWindow ?: return

        try {
            var reelContainer: AccessibilityNodeInfo? = null
            for (vid in config.viewIds) {
                reelContainer = NodeFinder.findFirst(root, vid)
                if (reelContainer != null) break
            }
            if (reelContainer == null) {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideIfInReel()
                return
            }

            if (!NodeFinder.isOnScreen(reelContainer, screenWidth, screenHeight)) {
                reelContainer.recycle()
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideIfInReel()
                return
            }

            for (req in config.requiresPresent) {
                if (!NodeFinder.exists(root, req)) {
                    reelContainer.recycle()
                    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideIfInReel()
                    return
                }
            }

            for (req in config.requiresAbsent) {
                if (NodeFinder.exists(root, req)) {
                    reelContainer.recycle()
                    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideIfInReel()
                    return
                }
            }

            reelContainer.recycle()

            var currentText = ""
            for (compId in config.dynamicComparatorIds) {
                val comp = NodeFinder.findFirst(root, compId)
                if (comp != null) {
                    currentText += cleanText(comp, config.cleanser)
                    comp.recycle()
                }
            }

            if (currentText.isBlank()) {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideIfInReel()
                return
            }

            hideJob?.cancel()
            if (currentReelPkg == null) {
                overlayManager.show(totalToday)
            }
            currentReelPkg = pkg

            detectNewReel(pkg, currentText)

        } catch (e: Exception) {
            Log.w(TAG, "Error processing event", e)
        } finally {
            root.recycle()
        }
    }

    private fun hideIfInReel() {
        if (currentReelPkg != null) {
            currentReelPkg = null
            hideJob?.cancel()
            hideJob = scope.launch {
                delay(1000)
                overlayManager.hide()
            }
        }
    }

    private fun detectNewReel(pkg: String, currentText: String) {
        val previous = lastDynamicText[pkg] ?: ""
        if (currentText == previous) return

        if (previous.isNotEmpty()) {
            val overlap = wordOverlapRatio(currentText, previous)
            if (overlap >= TEXT_CHANGE_THRESHOLD) return
        }

        val caption = extractFirstLine(currentText)
        val seen = recentCaptions.getOrPut(pkg) { mutableSetOf() }
        if (caption in seen) return
        seen.add(caption)
        if (seen.size > 100) seen.clear()

        lastDynamicText[pkg] = currentText
        val newCount = (perAppCounts[pkg] ?: 0) + 1
        perAppCounts[pkg] = newCount
        totalToday++

        overlayManager.updateCount(totalToday)

        scope.launch {
            val today = getTodayDate()
            val existing = database.reelCountDao().getCount(today, pkg)
            if (existing != null) {
                database.reelCountDao().upsert(existing.copy(count = newCount))
            } else {
                database.reelCountDao().upsert(ReelCount(date = today, packageName = pkg, count = newCount))
            }
            broadcastUpdate()
        }
    }

    private fun cleanText(node: AccessibilityNodeInfo, cleanser: ((String) -> String)?): String {
        val text = buildString {
            append(node.text?.toString() ?: "")
            append(node.contentDescription?.toString() ?: "")
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                append(cleanText(child, null))
                child.recycle()
            }
        }
        return if (cleanser != null) cleanser(text) else text
    }

    private fun wordOverlapRatio(a: String, b: String): Float {
        val wordsA = a.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val wordsB = b.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0f
        val intersection = wordsA.intersect(wordsB).size
        return intersection.toFloat() / minOf(wordsA.size, wordsB.size)
    }

    private fun extractFirstLine(text: String): String {
        val idx = text.indexOf('\n')
        return if (idx >= 0) text.substring(0, idx) else text
    }

    private fun broadcastUpdate() {
        for ((pkg, count) in perAppCounts) {
            val intent = Intent(ACTION_COUNT_UPDATED).apply {
                putExtra(EXTRA_PACKAGE, pkg)
                putExtra(EXTRA_COUNT, count)
            }
            sendBroadcast(intent)
        }
    }

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hide()
        scope.cancel()
    }
}
