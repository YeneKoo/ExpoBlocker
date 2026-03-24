package expo.modules.appblockerengine.blocker.monitor

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.Settings
import android.util.Base64
import expo.modules.appblockerengine.blocker.model.AppUsageStats
import java.io.ByteArrayOutputStream

class AppMonitor(private val context: Context) {
    
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
    
    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            return null
        }
        
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 10
        
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        if (usageStatsList.isEmpty()) {
            return null
        }
        
        var recentStats: UsageStats? = null
        for (usageStats in usageStatsList) {
            if (recentStats == null || usageStats.lastTimeUsed > recentStats.lastTimeUsed) {
                recentStats = usageStats
            }
        }
        
        return recentStats?.packageName
    }
    
    fun getInstalledApps(includeSystemApps: Boolean = false): List<String> {
        val installedApps: List<ApplicationInfo> = packageManager.getInstalledApplications(PackageManager.GET_META_DATA).toList()
        
        return installedApps
            .filter { appInfo: ApplicationInfo ->
                if (includeSystemApps) {
                    true
                } else {
                    !isSystemApp(appInfo)
                }
            }
            .map { appInfo: ApplicationInfo -> appInfo.packageName }
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
            return true
        }
        
        if (blockAll) {
            return !isSystemAppByName(packageName)
        }
        
        return false
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
}
