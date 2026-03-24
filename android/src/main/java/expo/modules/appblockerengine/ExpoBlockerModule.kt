package expo.modules.appblockerengine

import expo.modules.kotlin.AppContext
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoBlockerModule : Module() {
    
    private val appBlockerManager by lazy {
        expo.modules.appblockerengine.blocker.manager.AppBlockerManager.getInstance(appContext.reactContext!!)
    }
    
    override fun definition() = ModuleDefinition {
        Name("ExpoBlocker")
        
        AsyncFunction("block") { apps: List<String>?, promise: Promise ->
            appBlockerManager.block(apps)
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("blockWithExclude") { apps: List<String>?, excludeApps: List<String>, promise: Promise ->
            appBlockerManager.block(apps, excludeApps)
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("clear") { promise: Promise ->
            appBlockerManager.clear()
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("schedule") { time: String, promise: Promise ->
            val success = appBlockerManager.schedule(time)
            if (success) {
                promise.resolve(mapOf("success" to true))
            } else {
                promise.reject("SCHEDULE_ERROR", "Invalid time format. Use HH:mm (24-hour format)", null)
            }
        }
        
        AsyncFunction("scheduleWithExclude") { time: String, excludeApps: List<String>, promise: Promise ->
            val success = appBlockerManager.schedule(time, excludeApps)
            if (success) {
                promise.resolve(mapOf("success" to true))
            } else {
                promise.reject("SCHEDULE_ERROR", "Invalid time format. Use HH:mm (24-hour format)", null)
            }
        }
        
        AsyncFunction("getState") { promise: Promise ->
            val state = appBlockerManager.getState()
            promise.resolve(mapOf(
                "isBlocking" to state.isBlocking,
                "blockedApps" to state.blockedApps,
                "blockAll" to state.blockAll,
                "scheduledTime" to state.scheduledTime,
                "scheduleActivated" to state.scheduleActivated,
                "excludeApps" to state.excludeApps
            ))
        }
        
        AsyncFunction("isBlocking") { promise: Promise ->
            promise.resolve(appBlockerManager.isBlocking())
        }
        
        AsyncFunction("hasUsageStatsPermission") { promise: Promise ->
            promise.resolve(appBlockerManager.hasUsageStatsPermission())
        }
        
        AsyncFunction("hasOverlayPermission") { promise: Promise ->
            promise.resolve(appBlockerManager.hasOverlayPermission())
        }
        
        AsyncFunction("requestUsageStatsPermission") { promise: Promise ->
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.reactContext?.startActivity(intent)
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("requestOverlayPermission") { promise: Promise ->
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${appContext.reactContext?.packageName}")
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.reactContext?.startActivity(intent)
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("getInstalledApps") { promise: Promise ->
            val apps = appBlockerManager.getInstalledApps()
            promise.resolve(apps)
        }
        
        AsyncFunction("getAppName") { packageName: String, promise: Promise ->
            val appName = appBlockerManager.getAppName(packageName)
            promise.resolve(appName)
        }
        
        AsyncFunction("getAppIcon") { packageName: String, promise: Promise ->
            val iconBase64 = appBlockerManager.getAppIconBase64(packageName)
            promise.resolve(iconBase64)
        }
        
        AsyncFunction("getUsageStats") { promise: Promise ->
            val stats = appBlockerManager.getUsageStats()
            val result = stats.map { stat ->
                mapOf(
                    "packageName" to stat.packageName,
                    "appName" to stat.appName,
                    "iconBase64" to stat.iconBase64,
                    "usageTime" to stat.usageTime,
                    "lastTimeUsed" to stat.lastTimeUsed,
                    "usageTimeFormatted" to formatUsageTime(stat.usageTime)
                )
            }
            promise.resolve(result)
        }
        
        AsyncFunction("getAppUsageTime") { packageName: String, promise: Promise ->
            val usageTime = appBlockerManager.getUsageTimeForPackage(packageName)
            promise.resolve(mapOf(
                "usageTime" to usageTime,
                "usageTimeFormatted" to formatUsageTime(usageTime)
            ))
        }
        
        AsyncFunction("checkPermissions") { promise: Promise ->
            promise.resolve(mapOf(
                "usageStats" to appBlockerManager.hasUsageStatsPermission(),
                "overlay" to appBlockerManager.hasOverlayPermission()
            ))
        }
        
        AsyncFunction("setExcludeApps") { apps: List<String>, promise: Promise ->
            appBlockerManager.setExcludeApps(apps)
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("getExcludeApps") { promise: Promise ->
            val state = appBlockerManager.getState()
            promise.resolve(state.excludeApps)
        }
        
        AsyncFunction("updateOverlayConfig") { config: Map<String, Any?>, promise: Promise ->
            val overlayConfig = expo.modules.appblockerengine.blocker.model.OverlayConfig(
                title = config["title"] as? String ?: "App Blocked",
                message = config["message"] as? String ?: "This app has been blocked",
                backgroundColor = (config["backgroundColor"] as? Number)?.toInt() ?: 0xFF1A1A1A.toInt(),
                textColor = (config["textColor"] as? Number)?.toInt() ?: 0xFFFFFFFF.toInt(),
                titleTextSize = (config["titleTextSize"] as? Number)?.toFloat() ?: 32f,
                messageTextSize = (config["messageTextSize"] as? Number)?.toFloat() ?: 18f,
                showAppIcon = config["showAppIcon"] as? Boolean ?: true,
                showAppName = config["showAppName"] as? Boolean ?: true,
                showUsageStats = config["showUsageStats"] as? Boolean ?: true
            )
            appBlockerManager.updateOverlayConfig(overlayConfig)
            promise.resolve(mapOf("success" to true))
        }
        
        AsyncFunction("getOverlayConfig") { promise: Promise ->
            val config = appBlockerManager.getOverlayConfig()
            promise.resolve(mapOf(
                "title" to config.title,
                "message" to config.message,
                "backgroundColor" to config.backgroundColor,
                "textColor" to config.textColor,
                "titleTextSize" to config.titleTextSize,
                "messageTextSize" to config.messageTextSize,
                "showAppIcon" to config.showAppIcon,
                "showAppName" to config.showAppName,
                "showUsageStats" to config.showUsageStats
            ))
        }
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
}
