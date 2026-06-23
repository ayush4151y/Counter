package com.counter.app

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
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
    private var lastCount = -1

    private val params: WindowManager.LayoutParams by lazy {
        val dm = context.resources.displayMetrics
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels * 0.85f).toInt()
            y = (dm.heightPixels * 0.3f).toInt()
        }
    }

    fun hasPermission(): Boolean =
        Settings.canDrawOverlays(context)

    fun show(count: Int) {
        if (!hasPermission()) return
        if (overlayView != null && isVisible) {
            if (count != lastCount) {
                binding?.reelCounterText?.text = count.toString()
                lastCount = count
            }
            return
        }

        binding = OverlayReelCounterBinding.inflate(LayoutInflater.from(context))
        overlayView = binding?.root
        binding?.reelCounterText?.text = count.toString()
        lastCount = count

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    offsetX = event.rawX
                    offsetY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - offsetX).toInt()
                    params.y = initialY + (event.rawY - offsetY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(overlayView, params)
        isVisible = true
    }

    fun updateCount(count: Int) {
        if (count == lastCount) return
        lastCount = count
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
        isVisible = false
        lastCount = -1
    }
}
