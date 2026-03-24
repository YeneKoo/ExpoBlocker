package expo.modules.appblockerengine.blocker.controller

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import expo.modules.appblockerengine.blocker.model.AppInfo
import expo.modules.appblockerengine.blocker.model.OverlayConfig

class OverlayController(private val context: Context) {
    
    private var overlayView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var currentConfig: OverlayConfig = OverlayConfig()
    private var currentAppInfo: AppInfo? = null
    
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
    
    fun updateConfig(config: OverlayConfig) {
        currentConfig = config
        // If overlay is showing, recreate it with new config
        if (overlayView != null) {
            val appInfo = currentAppInfo
            hideOverlay()
            if (appInfo != null) {
                showOverlayWithInfo(appInfo)
            }
        }
    }
    
    fun showOverlay(packageName: String, appName: String, usageTime: Long) {
        val appInfo = AppInfo(
            packageName = packageName,
            appName = appName,
            iconBase64 = null,
            usageTime = usageTime
        )
        showOverlayWithInfo(appInfo)
    }
    
    fun showOverlayWithInfo(appInfo: AppInfo) {
        currentAppInfo = appInfo
        handler.post {
            if (overlayView != null) {
                return@post
            }
            
            try {
                overlayView = createOverlayView(appInfo)
                windowManager.addView(overlayView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun createOverlayView(appInfo: AppInfo): FrameLayout {
        val config = currentConfig
        
        val container = FrameLayout(context).apply {
            setBackgroundColor(config.backgroundColor)
        }
        
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        
        // App Icon
        if (config.showAppIcon && appInfo.iconBase64 != null) {
            val iconImageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                    bottomMargin = 40
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            
            try {
                val decodedBytes = Base64.decode(appInfo.iconBase64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                iconImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Icon loading failed
            }
            
            contentLayout.addView(iconImageView)
        }
        
        // App Name
        if (config.showAppName) {
            val appNameText = TextView(context).apply {
                text = appInfo.appName
                textSize = 24f
                setTextColor(config.textColor)
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 20
                }
            }
            contentLayout.addView(appNameText)
        }
        
        // Title
        val titleText = TextView(context).apply {
            text = config.title
            textSize = config.titleTextSize
            setTextColor(config.textColor)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        contentLayout.addView(titleText)
        
        // Message
        val messageText = TextView(context).apply {
            text = config.message
            textSize = config.messageTextSize
            setTextColor(config.textColor)
            gravity = Gravity.CENTER
            alpha = 0.7f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }
        contentLayout.addView(messageText)
        
        // Usage Stats
        if (config.showUsageStats && appInfo.usageTime > 0) {
            val usageText = TextView(context).apply {
                text = "Today's usage: ${formatUsageTime(appInfo.usageTime)}"
                textSize = 16f
                setTextColor(config.textColor)
                gravity = Gravity.CENTER
                alpha = 0.6f
            }
            contentLayout.addView(usageText)
        }
        
        container.addView(contentLayout)
        return container
    }
    
    private fun formatUsageTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
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
            currentAppInfo = null
        }
    }
    
    fun isOverlayShowing(): Boolean {
        return overlayView != null
    }
}
