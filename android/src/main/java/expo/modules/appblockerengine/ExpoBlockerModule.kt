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
        
        AsyncFunction("getState") { promise: Promise ->
            val state = appBlockerManager.getState()
            promise.resolve(mapOf(
                "isBlocking" to state.isBlocking,
                "blockedApps" to state.blockedApps,
                "blockAll" to state.blockAll,
                "scheduledTime" to state.scheduledTime,
                "scheduleActivated" to state.scheduleActivated
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
        
        AsyncFunction("checkPermissions") { promise: Promise ->
            promise.resolve(mapOf(
                "usageStats" to appBlockerManager.hasUsageStatsPermission(),
                "overlay" to appBlockerManager.hasOverlayPermission()
            ))
        }
    }
}
