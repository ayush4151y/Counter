package com.counter.app

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.counter.app.databinding.OverlayReelCounterBinding

class ReelOverlayManager(private val context: Context) {

    private var overlayView: View? = null
    private var binding: OverlayReelCounterBinding? = null
    private var windowManager: WindowManager? = null
    var isVisible = false
        private set

    private var initialX = 0
    private var initialY = 0
    private var offsetX = 0f
    private var offsetY = 0f

    private var params: WindowManager.LayoutParams? = null

    fun show(count: Int) {
        if (overlayView != null) {
            binding?.reelCounterText?.text = count.toString()
            return
        }

        binding = OverlayReelCounterBinding.inflate(LayoutInflater.from(context))
        overlayView = binding?.root
        binding?.reelCounterText?.text = count.toString()

        val dm = context.resources.displayMetrics

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels * 0.85f).toInt()
            y = (dm.heightPixels * 0.3f).toInt()
        }

        overlayView?.setOnTouchListener { _, event ->
            params?.let { p ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = p.x
                        initialY = p.y
                        offsetX = event.rawX
                        offsetY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = initialX + (event.rawX - offsetX).toInt()
                        p.y = initialY + (event.rawY - offsetY).toInt()
                        windowManager?.updateViewLayout(overlayView, p)
                        true
                    }
                    MotionEvent.ACTION_UP -> true
                    else -> false
                }
            } ?: false
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(overlayView, params)
        isVisible = true
    }

    fun updateCount(count: Int) {
        binding?.reelCounterText?.text = count.toString()
    }

    fun hide() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
        }
        overlayView = null
        binding = null
        params = null
        isVisible = false
    }
}
