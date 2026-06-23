package com.counter.app

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object NodeFinder {

    fun findFirst(root: AccessibilityNodeInfo, spec: String): AccessibilityNodeInfo? {
        val parsed = parseSelector(spec)
        return findFirst(root, parsed)
    }

    fun exists(root: AccessibilityNodeInfo, spec: String): Boolean {
        val node = findFirst(root, spec) ?: return false
        node.recycle()
        return true
    }

    fun isOnScreen(node: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.left < screenWidth && rect.right > 0 &&
                rect.top < screenHeight && rect.bottom > 0
    }

    fun findFirst(root: AccessibilityNodeInfo, selectors: Map<String, String>): AccessibilityNodeInfo? {
        val id = selectors["id"]
        if (id != null) {
            val candidates = root.findAccessibilityNodeInfosByViewId(id)
            if (candidates != null) {
                for (c in candidates) {
                    if (matchesAll(c, selectors)) return obtain(c)
                    c.recycle()
                }
            }
        }

        val text = selectors["text"] ?: selectors["textContains"]
        if (text != null) {
            val candidates = root.findAccessibilityNodeInfosByText(text)
            if (candidates != null) {
                for (c in candidates) {
                    if (matchesAll(c, selectors)) return obtain(c)
                    c.recycle()
                }
            }
        }

        return dfsFirst(root, selectors)
    }

    private fun matchesAll(node: AccessibilityNodeInfo, selectors: Map<String, String>): Boolean {
        for ((key, value) in selectors) {
            when (key) {
                "id" -> {
                    if (node.viewIdResourceName != value) return false
                }
                "text" -> {
                    if (node.text?.toString() != value) return false
                }
                "textContains" -> {
                    if (node.text?.toString()?.contains(value, true) != true) return false
                }
                "desc" -> {
                    if (node.contentDescription?.toString() != value) return false
                }
                "descContains" -> {
                    if (node.contentDescription?.toString()?.contains(value, true) != true) return false
                }
            }
        }
        return true
    }

    private fun dfsFirst(node: AccessibilityNodeInfo, selectors: Map<String, String>): AccessibilityNodeInfo? {
        if (matchesAll(node, selectors)) return obtain(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = dfsFirst(child, selectors)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun obtain(node: AccessibilityNodeInfo): AccessibilityNodeInfo =
        AccessibilityNodeInfo.obtain(node)

    fun parseSelector(spec: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (part in spec.split(";")) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            val sep = trimmed.indexOf(':')
            if (sep < 0) {
                map["id"] = trimmed
                continue
            }
            val key = trimmed.substring(0, sep).trim().lowercase()
            val value = trimmed.substring(sep + 1).trim()
            map[key] = value
        }
        return map
    }
}
