package expo.modules.appblockerengine.blocker.controller

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class OverlayController(private val context: Context) {
    
    private var overlayView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private val layoutParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }
    
    fun showOverlay(packageName: String) {
        handler.post {
            if (overlayView != null) {
                return@post
            }
            
            try {
                overlayView = FrameLayout(context).apply {
                    setBackgroundColor(0xFF1A1A1A.toInt())
                    
                    val textView = TextView(context).apply {
                        text = "App Blocked"
                        textSize = 32f
                        setTextColor(0xFFFFFFFF.toInt())
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    
                    addView(textView)
                }
                
                windowManager.addView(overlayView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun hideOverlay() {
        handler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }
        }
    }
    
    fun isOverlayShowing(): Boolean {
        return overlayView != null
    }
}
