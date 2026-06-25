package com.counter.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.counter.app.databinding.OverlayReelCounterBinding

class ReelOverlayManager(private val context: Context) {
    companion object {
        const val TAG = "ReelOverlayManager"
    }

    private var overlayView: View? = null
    private var binding: OverlayReelCounterBinding? = null
    private var windowManager: WindowManager? = null
    var isVisible = false
        private set

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

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun show(count: Int) {
        if (overlayView != null || isVisible) return
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted, cannot show overlay")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        binding = OverlayReelCounterBinding.inflate(LayoutInflater.from(context))
        overlayView = binding?.root
        isVisible = true
        lastCount = count
        binding?.reelLabel?.visibility = View.VISIBLE
        binding?.reelCounterText?.text = count.toString()
        binding?.reelUnit?.visibility = View.VISIBLE

        var initX = 0
        var initY = 0
        var offsetX = 0f
        var offsetY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x
                    initY = params.y
                    offsetX = event.rawX
                    offsetY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (event.rawX - offsetX).toInt()
                    params.y = initY + (event.rawY - offsetY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(overlayView, params)
    }

    fun updateCount(count: Int) {
        if (count == lastCount) return
        lastCount = count
        binding?.reelLabel?.visibility = View.VISIBLE
        binding?.reelCounterText?.text = count.toString()
        binding?.reelUnit?.visibility = View.VISIBLE
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