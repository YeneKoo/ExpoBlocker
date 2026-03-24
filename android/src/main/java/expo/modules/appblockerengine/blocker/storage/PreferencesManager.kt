package expo.modules.appblockerengine.blocker.storage

import android.content.Context
import android.content.SharedPreferences
import expo.modules.appblockerengine.blocker.model.BlockerState
import expo.modules.appblockerengine.blocker.model.OverlayConfig
import expo.modules.appblockerengine.blocker.model.AppUsageStats
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "app_blocker_prefs"
        private const val KEY_IS_BLOCKING = "is_blocking"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_BLOCK_ALL = "block_all"
        private const val KEY_SCHEDULED_TIME = "scheduled_time"
        private const val KEY_SCHEDULE_ACTIVATED = "schedule_activated"
        private const val KEY_EXCLUDE_APPS = "exclude_apps"
        private const val KEY_OVERLAY_CONFIG = "overlay_config"
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun saveState(state: BlockerState) {
        prefs.edit().apply {
            putBoolean(KEY_IS_BLOCKING, state.isBlocking)
            putStringSet(KEY_BLOCKED_APPS, state.blockedApps.toSet())
            putBoolean(KEY_BLOCK_ALL, state.blockAll)
            putString(KEY_SCHEDULED_TIME, state.scheduledTime)
            putBoolean(KEY_SCHEDULE_ACTIVATED, state.scheduleActivated)
            putStringSet(KEY_EXCLUDE_APPS, state.excludeApps.toSet())
            apply()
        }
    }
    
    fun loadState(): BlockerState {
        return BlockerState(
            isBlocking = prefs.getBoolean(KEY_IS_BLOCKING, false),
            blockedApps = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toList() ?: emptyList(),
            blockAll = prefs.getBoolean(KEY_BLOCK_ALL, false),
            scheduledTime = prefs.getString(KEY_SCHEDULED_TIME, null),
            scheduleActivated = prefs.getBoolean(KEY_SCHEDULE_ACTIVATED, false),
            excludeApps = prefs.getStringSet(KEY_EXCLUDE_APPS, emptySet())?.toList() ?: emptyList()
        )
    }
    
    fun saveOverlayConfig(config: OverlayConfig) {
        val json = JSONObject().apply {
            put("title", config.title)
            put("message", config.message)
            put("backgroundColor", config.backgroundColor)
            put("textColor", config.textColor)
            put("titleTextSize", config.titleTextSize.toDouble())
            put("messageTextSize", config.messageTextSize.toDouble())
            put("showAppIcon", config.showAppIcon)
            put("showAppName", config.showAppName)
            put("showUsageStats", config.showUsageStats)
        }
        prefs.edit().putString(KEY_OVERLAY_CONFIG, json.toString()).apply()
    }
    
    fun loadOverlayConfig(): OverlayConfig {
        val jsonStr = prefs.getString(KEY_OVERLAY_CONFIG, null) ?: return OverlayConfig()
        
        return try {
            val json = JSONObject(jsonStr)
            OverlayConfig(
                title = json.optString("title", "App Blocked"),
                message = json.optString("message", "This app has been blocked"),
                backgroundColor = json.optInt("backgroundColor", 0xFF1A1A1A.toInt()),
                textColor = json.optInt("textColor", 0xFFFFFFFF.toInt()),
                titleTextSize = json.optDouble("titleTextSize", 32.0).toFloat(),
                messageTextSize = json.optDouble("messageTextSize", 18.0).toFloat(),
                showAppIcon = json.optBoolean("showAppIcon", true),
                showAppName = json.optBoolean("showAppName", true),
                showUsageStats = json.optBoolean("showUsageStats", true)
            )
        } catch (e: Exception) {
            OverlayConfig()
        }
    }
    
    fun setExcludeApps(apps: List<String>) {
        prefs.edit().putStringSet(KEY_EXCLUDE_APPS, apps.toSet()).apply()
    }
    
    fun isBlocking(): Boolean = prefs.getBoolean(KEY_IS_BLOCKING, false)
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
