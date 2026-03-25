package expo.modules.appblockerengine.blocker.storage

import android.content.Context
import android.content.SharedPreferences
import expo.modules.appblockerengine.blocker.model.BlockerState
import expo.modules.appblockerengine.blocker.model.OverlayConfig
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
        private const val KEY_BUTTON_CALLBACK = "button_callback"
        
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
            put("message", config.message ?: "")
            put("description", config.description ?: "")
            put("backgroundColor", config.backgroundColor)
            put("textColor", config.textColor)
            put("titleTextSize", config.titleTextSize.toDouble())
            put("messageTextSize", config.messageTextSize.toDouble())
            put("descriptionTextSize", config.descriptionTextSize.toDouble())
            put("showAppIcon", config.showAppIcon)
            put("showAppName", config.showAppName)
            put("showUsageStats", config.showUsageStats)
            put("showTodayUsage", config.showTodayUsage)
            put("blockerAppName", config.blockerAppName ?: "")
            put("buttonText", config.buttonText ?: "")
            put("buttonLink", config.buttonLink ?: "")
            put("buttonColor", config.buttonColor)
            put("buttonTextColor", config.buttonTextColor)
            put("buttonBorderRadius", config.buttonBorderRadius.toDouble())
            put("buttonWidth", config.buttonWidth.toDouble())
            put("buttonHeight", config.buttonHeight.toDouble())
            put("buttonMarginTop", config.buttonMarginTop.toDouble())
            put("showCloseButton", config.showCloseButton)
            put("closeButtonColor", config.closeButtonColor)
        }
        prefs.edit().putString(KEY_OVERLAY_CONFIG, json.toString()).apply()
    }
    
    fun loadOverlayConfig(): OverlayConfig {
        val jsonStr = prefs.getString(KEY_OVERLAY_CONFIG, null) ?: return OverlayConfig()
        
        return try {
            val json = JSONObject(jsonStr)
            OverlayConfig(
                title = json.optString("title", "App Blocked"),
                message = json.optString("message", null).takeIf { it.isNotEmpty() },
                description = json.optString("description", null).takeIf { it.isNotEmpty() },
                backgroundColor = json.optString("backgroundColor", "#1A1A1A"),
                textColor = json.optString("textColor", "#FFFFFF"),
                titleTextSize = json.optDouble("titleTextSize", 32.0).toFloat(),
                messageTextSize = json.optDouble("messageTextSize", 18.0).toFloat(),
                descriptionTextSize = json.optDouble("descriptionTextSize", 16.0).toFloat(),
                showAppIcon = json.optBoolean("showAppIcon", true),
                showAppName = json.optBoolean("showAppName", true),
                showUsageStats = json.optBoolean("showUsageStats", false),
                showTodayUsage = json.optBoolean("showTodayUsage", false),
                blockerAppName = json.optString("blockerAppName", null).takeIf { it.isNotEmpty() },
                buttonText = json.optString("buttonText", null).takeIf { it.isNotEmpty() },
                buttonLink = json.optString("buttonLink", null).takeIf { it.isNotEmpty() },
                buttonColor = json.optString("buttonColor", "#4CAF50"),
                buttonTextColor = json.optString("buttonTextColor", "#FFFFFF"),
                buttonBorderRadius = json.optDouble("buttonBorderRadius", 50.0).toFloat(),
                buttonWidth = json.optDouble("buttonWidth", 280.0).toFloat(),
                buttonHeight = json.optDouble("buttonHeight", 60.0).toFloat(),
                buttonMarginTop = json.optDouble("buttonMarginTop", 40.0).toFloat(),
                showCloseButton = json.optBoolean("showCloseButton", false),
                closeButtonColor = json.optString("closeButtonColor", "#666666")
            )
        } catch (e: Exception) {
            OverlayConfig()
        }
    }
    
    fun saveButtonCallback(callback: String?) {
        prefs.edit().putString(KEY_BUTTON_CALLBACK, callback).apply()
    }
    
    fun loadButtonCallback(): String? {
        return prefs.getString(KEY_BUTTON_CALLBACK, null)
    }
    
    fun setExcludeApps(apps: List<String>) {
        prefs.edit().putStringSet(KEY_EXCLUDE_APPS, apps.toSet()).apply()
    }
    
    fun isBlocking(): Boolean = prefs.getBoolean(KEY_IS_BLOCKING, false)
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
