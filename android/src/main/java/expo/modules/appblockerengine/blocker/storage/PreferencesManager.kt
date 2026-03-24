package expo.modules.appblockerengine.blocker.storage

import android.content.Context
import android.content.SharedPreferences
import expo.modules.appblockerengine.blocker.model.BlockerState

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
            apply()
        }
    }
    
    fun loadState(): BlockerState {
        return BlockerState(
            isBlocking = prefs.getBoolean(KEY_IS_BLOCKING, false),
            blockedApps = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toList() ?: emptyList(),
            blockAll = prefs.getBoolean(KEY_BLOCK_ALL, false),
            scheduledTime = prefs.getString(KEY_SCHEDULED_TIME, null),
            scheduleActivated = prefs.getBoolean(KEY_SCHEDULE_ACTIVATED, false)
        )
    }
    
    fun setBlocking(blocking: Boolean) {
        prefs.edit().putBoolean(KEY_IS_BLOCKING, blocking).apply()
    }
    
    fun setBlockedApps(apps: List<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, apps.toSet()).apply()
    }
    
    fun setBlockAll(blockAll: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_ALL, blockAll).apply()
    }
    
    fun setScheduledTime(time: String?) {
        prefs.edit().putString(KEY_SCHEDULED_TIME, time).apply()
    }
    
    fun setScheduleActivated(activated: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULE_ACTIVATED, activated).apply()
    }
    
    fun isBlocking(): Boolean = prefs.getBoolean(KEY_IS_BLOCKING, false)
    
    fun hasActiveSchedule(): Boolean {
        return prefs.getString(KEY_SCHEDULED_TIME, null) != null
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
