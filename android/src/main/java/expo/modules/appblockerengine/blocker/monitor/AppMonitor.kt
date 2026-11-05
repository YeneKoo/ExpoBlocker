package expo.modules.appblockerengine.blocker.monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Base64
import expo.modules.appblockerengine.blocker.model.AppUsageStats
import expo.modules.appblockerengine.blocker.model.ScreenSession
import java.io.ByteArrayOutputStream

class AppMonitor(private val context: Context) {
    
    companion object {
        // Margin on the time since the last query, so no switch falls between two.
        private const val LOOKBACK_MARGIN_MS = 5_000L
        // Once per process: far enough back to find an app held in front across a
        // service restart. Later polls only cover the gap since the last one.
        private const val INITIAL_LOOKBACK_MS = 24 * 60 * 60 * 1000L
        // ACTIVITY_RESUMED (API 29+) under its pre-29 name.
        @Suppress("DEPRECATION")
        private const val RESUMED_EVENT = UsageEvents.Event.MOVE_TO_FOREGROUND
        // Spelled out because TelephonyManager.ACTION_EMERGENCY_ASSISTANCE is
        // @SystemApi and absent from the public SDK. Matches the <queries> entry
        // app.plugin.js adds, without which this would never resolve on API 30+.
        private const val ACTION_EMERGENCY_ASSISTANCE =
            "android.telephony.action.EMERGENCY_ASSISTANCE"
    }
    
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    
    private val appOpsManager: AppOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }
    
    private val packageManager: PackageManager by lazy {
        context.packageManager
    }
    
    fun hasUsageStatsPermission(): Boolean {
        val appOps = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return appOps == AppOpsManager.MODE_ALLOWED
    }
    
    // Last app seen resuming. Events only arrive on a switch, so staying in one
    // app produces none; the last known app carries over between polls.
    private var lastForegroundPackage: String? = null
    private var lastQueryMillis: Long? = null
    
    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            return null
        }
        
        val endTime = System.currentTimeMillis()
        // Spans the whole gap since the previous query, which is 30s while paused.
        val startTime = lastQueryMillis
            ?.let { it - LOOKBACK_MARGIN_MS }
            ?: (endTime - INITIAL_LOOKBACK_MS)
        lastQueryMillis = endTime
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == RESUMED_EVENT) {
                lastForegroundPackage = event.packageName
            }
        }
        
        return lastForegroundPackage
    }
    
    // Launchable apps only, visible through the plugin's MAIN/LAUNCHER <queries>
    // entry. Preinstalled apps such as YouTube carry FLAG_SYSTEM on many
    // devices, so filtering on it would hide the apps people most want to lock.
    fun getInstalledApps(): List<String> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val protectedPackages = protectedPackages()
        return activities
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName && it !in protectedPackages }
            .distinct()
    }
    
    fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }
    
    fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    fun getAppIconBase64(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val icon = packageManager.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(icon)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
            bitmapToBase64(resizedBitmap)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    fun shouldBlockPackage(packageName: String, blockedApps: List<String>, blockAll: Boolean, excludeApps: List<String>): Boolean {
        // Always skip excluded apps
        if (excludeApps.contains(packageName)) {
            return false
        }
        
        // Skip our own app
        if (packageName == context.packageName) {
            return false
        }
        
        if (blockedApps.contains(packageName)) {
            return !isProtectedPackage(packageName)
        }
        
        if (blockAll) {
            return !isSystemAppByName(packageName) && !isProtectedPackage(packageName)
        }
        
        return false
    }
    
    // Resolved only for an app that would otherwise be blocked, so the
    // once-a-second poll rarely pays for it.
    private fun isProtectedPackage(packageName: String): Boolean =
        packageName in protectedPackages()

    // Locking any of these would strand the user: no calls, no way to revoke the
    // permissions, no home screen, no emergency help. They are left out of
    // getInstalledApps too, so a picker never offers a lock that cannot hold.
    private fun protectedPackages(): Set<String> {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val protectedIntents = listOf(
            Intent(Settings.ACTION_SETTINGS),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            Intent(ACTION_EMERGENCY_ASSISTANCE),
        )
        val resolved = protectedIntents.mapNotNull { intent ->
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            info?.activityInfo?.packageName
        }
        return (resolved + listOfNotNull(telecomManager?.defaultDialerPackage)).toSet()
    }
    
    private fun isSystemAppByName(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            isSystemApp(appInfo)
        } catch (e: Exception) {
            false
        }
    }
    
    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    fun getAppUsageStats(startTime: Long, endTime: Long): List<AppUsageStats> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }
        
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        return usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                AppUsageStats(
                    packageName = stats.packageName,
                    appName = getAppName(stats.packageName),
                    iconBase64 = getAppIconBase64(stats.packageName),
                    usageTime = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed
                )
            }
            .sortedByDescending { it.usageTime }
    }
    
    fun getTodayUsageStats(): List<AppUsageStats> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getAppUsageStats(startTime, endTime)
    }
    
    fun getUsageTimeForPackage(packageName: String): Long {
        if (!hasUsageStatsPermission()) {
            return 0
        }
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        return usageStatsList.find { it.packageName == packageName }?.totalTimeInForeground ?: 0
    }

    // Periods the screen was on, from the screen on/off events (API 28+; null
    // below). The system keeps roughly ten days of events.
    fun getScreenSessions(startMillis: Long, endMillis: Long): List<ScreenSession>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val sessions = mutableListOf<ScreenSession>()
        var onSince: Long? = null
        val events = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                // A second "on" without an "off" means the phone lost power
                // with the screen on; restart rather than count the outage.
                UsageEvents.Event.SCREEN_INTERACTIVE -> onSince = event.timeStamp
                // An "off" with no "on" before it is dropped: near the end of
                // the kept history, its start may be days earlier. Shutdown
                // (Android 10+) ends a session the same way.
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    val start = onSince
                    if (start != null && event.timeStamp > start) {
                        sessions += ScreenSession(start, event.timeStamp)
                    }
                    onSince = null
                }
            }
        }

        val stillOn = onSince
        val end = minOf(endMillis, System.currentTimeMillis())
        if (stillOn != null && end > stillOn) {
            sessions += ScreenSession(stillOn, end)
        }
        return sessions
    }
}
