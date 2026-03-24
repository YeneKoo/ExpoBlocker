package expo.modules.appblockerengine.blocker.monitor

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

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
    
    fun shouldBlockPackage(packageName: String, blockedApps: List<String>, blockAll: Boolean): Boolean {
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
}
