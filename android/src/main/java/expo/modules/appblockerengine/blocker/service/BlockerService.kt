package expo.modules.appblockerengine.blocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import expo.modules.appblockerengine.blocker.controller.OverlayController
import expo.modules.appblockerengine.blocker.monitor.AppMonitor
import expo.modules.appblockerengine.blocker.model.AppInfo
import expo.modules.appblockerengine.blocker.storage.PreferencesManager
import expo.modules.appblockerengine.blocker.util.TimeUtils

class BlockerService : Service() {
    
    private lateinit var appMonitor: AppMonitor
    private lateinit var overlayController: OverlayController
    private lateinit var preferencesManager: PreferencesManager
    
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    
    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring) {
                checkAndBlock()
                handler.postDelayed(this, POLLING_INTERVAL)
            }
        }
    }
    
    companion object {
        const val CHANNEL_ID = "app_blocker_channel"
        const val NOTIFICATION_ID = 1001
        const val POLLING_INTERVAL = 500L
        
        const val ACTION_START = "expo.modules.appblockerengine.action.START"
        const val ACTION_STOP = "expo.modules.appblockerengine.action.STOP"
        
        fun startService(context: Context) {
            val intent = Intent(context, BlockerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, BlockerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        appMonitor = AppMonitor(this)
        overlayController = OverlayController(this)
        preferencesManager = PreferencesManager.getInstance(this)
        
        // Update overlay config from preferences
        val config = preferencesManager.loadOverlayConfig()
        overlayController.updateConfig(config)
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startMonitoring()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        overlayController.hideOverlay()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when app blocking is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocker Active")
            .setContentText("Monitoring apps in background")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        handler.post(monitorRunnable)
    }
    
    private fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
    }
    
    private fun checkAndBlock() {
        val state = preferencesManager.loadState()
        
        if (!state.isBlocking && !state.scheduleActivated) {
            overlayController.hideOverlay()
            return
        }
        
        if (state.scheduleActivated && state.scheduledTime != null) {
            if (!TimeUtils.isTimeReached(state.scheduledTime)) {
                overlayController.hideOverlay()
                return
            }
        }
        
        val currentApp = appMonitor.getCurrentForegroundApp() ?: return
        
        val shouldBlock = appMonitor.shouldBlockPackage(
            currentApp,
            state.blockedApps,
            state.blockAll,
            state.excludeApps
        )
        
        if (shouldBlock) {
            val appName = appMonitor.getAppName(currentApp)
            val usageTime = appMonitor.getUsageTimeForPackage(currentApp)
            
            val appInfo = AppInfo(
                packageName = currentApp,
                appName = appName,
                iconBase64 = appMonitor.getAppIconBase64(currentApp),
                usageTime = usageTime
            )
            
            overlayController.showOverlayWithInfo(appInfo)
        } else {
            overlayController.hideOverlay()
        }
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        val state = preferencesManager.loadState()
        if (state.isBlocking || state.scheduledTime != null) {
            val restartIntent = Intent(this, BlockerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
    }
}
