package expo.modules.appblockerengine.blocker.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
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
    private var buttonClickReceiver: BroadcastReceiver? = null
    
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
        
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            setPadding(40, 80, 40, 40)
        }
        
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
            }
        }
        
        if (config.showAppIcon && appInfo.iconBase64 != null) {
            val iconImageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(180, 180).apply {
                    bottomMargin = 24
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            
            try {
                val decodedBytes = Base64.decode(appInfo.iconBase64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                iconImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
            }
            
            contentLayout.addView(iconImageView)
        }
        
        if (config.showAppName) {
            val appNameText = TextView(context).apply {
                text = "${appInfo.appName} is blocked"
                textSize = 28f
                setTextColor(config.textColor)
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            contentLayout.addView(appNameText)
        }
        
        if (!config.message.isNullOrEmpty()) {
            val messageText = TextView(context).apply {
                text = config.message
                textSize = config.messageTextSize
                setTextColor(config.textColor)
                gravity = Gravity.CENTER
                alpha = 0.85f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            contentLayout.addView(messageText)
        }
        
        if (!config.description.isNullOrEmpty()) {
            val descriptionText = TextView(context).apply {
                text = config.description
                textSize = config.descriptionTextSize
                setTextColor(config.textColor)
                gravity = Gravity.CENTER
                alpha = 0.65f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            contentLayout.addView(descriptionText)
        }
        
        if (config.showTodayUsage && appInfo.usageTime > 0) {
            val usageText = TextView(context).apply {
                text = "Today's usage: ${formatUsageTime(appInfo.usageTime)}"
                textSize = 15f
                setTextColor(config.textColor)
                gravity = Gravity.CENTER
                alpha = 0.7f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            contentLayout.addView(usageText)
        }
        
        mainLayout.addView(contentLayout)
        
        if (!config.buttonText.isNullOrEmpty()) {
            val buttonWrapper = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
            }
            
            val button = Button(context).apply {
                text = config.buttonText
                setTextColor(config.buttonTextColor)
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                
                val drawable = GradientDrawable().apply {
                    setColor(config.buttonColor)
                    cornerRadius = config.buttonBorderRadius
                }
                background = drawable
                
                layoutParams = LinearLayout.LayoutParams(
                    config.buttonWidth.toInt(),
                    config.buttonHeight.toInt()
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = config.buttonMarginTop.toInt()
                    bottomMargin = 60
                }
                setPadding(30, 0, 30, 0)
            }
            
            button.setOnClickListener {
                val intent = Intent(ACTION_BUTTON_CLICKED)
                intent.putExtra("packageName", appInfo.packageName)
                context.sendBroadcast(intent)
            }
            
            buttonWrapper.addView(button)
            mainLayout.addView(buttonWrapper)
        }
        
        container.addView(mainLayout)
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
    
    companion object {
        const val ACTION_BUTTON_CLICKED = "expo.modules.appblockerengine.action.BUTTON_CLICKED"
    }
}
