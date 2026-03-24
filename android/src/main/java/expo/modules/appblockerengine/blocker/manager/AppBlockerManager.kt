package expo.modules.appblockerengine.blocker.manager

import android.content.Context
import expo.modules.appblockerengine.blocker.model.BlockerState
import expo.modules.appblockerengine.blocker.monitor.AppMonitor
import expo.modules.appblockerengine.blocker.service.BlockerService
import expo.modules.appblockerengine.blocker.storage.PreferencesManager
import expo.modules.appblockerengine.blocker.util.TimeUtils

class AppBlockerManager private constructor(private val context: Context) {
    
    private val preferencesManager = PreferencesManager.getInstance(context)
    private val appMonitor = AppMonitor(context)
    
    companion object {
        @Volatile
        private var instance: AppBlockerManager? = null
        
        fun getInstance(context: Context): AppBlockerManager {
            return instance ?: synchronized(this) {
                instance ?: AppBlockerManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun block(apps: List<String>?) {
        val currentState = preferencesManager.loadState()
        
        val state = BlockerState(
            isBlocking = true,
            blockedApps = apps ?: emptyList(),
            blockAll = apps == null || apps.isEmpty(),
            scheduledTime = currentState.scheduledTime,
            scheduleActivated = currentState.scheduleActivated
        )
        
        preferencesManager.saveState(state)
        startServiceIfNeeded()
    }
    
    fun clear() {
        val clearedState = BlockerState(
            isBlocking = false,
            blockedApps = emptyList(),
            blockAll = false,
            scheduledTime = null,
            scheduleActivated = false
        )
        
        preferencesManager.saveState(clearedState)
        stopServiceIfNotNeeded()
    }
    
    fun schedule(time: String): Boolean {
        val parsed = TimeUtils.parseTime(time)
        if (parsed == null) {
            return false
        }
        
        val currentState = preferencesManager.loadState()
        
        val scheduleActivated = TimeUtils.isTimeReached(time)
        
        val state = BlockerState(
            isBlocking = currentState.isBlocking || scheduleActivated,
            blockedApps = currentState.blockedApps,
            blockAll = currentState.blockAll,
            scheduledTime = time,
            scheduleActivated = scheduleActivated
        )
        
        preferencesManager.saveState(state)
        startServiceIfNeeded()
        
        return true
    }
    
    fun getState(): BlockerState {
        val state = preferencesManager.loadState()
        
        if (state.scheduleActivated && state.scheduledTime != null) {
            if (!TimeUtils.isTimeReached(state.scheduledTime)) {
                return state.copy(isBlocking = false)
            }
        }
        
        return state
    }
    
    fun isBlocking(): Boolean {
        val state = getState()
        return state.isBlocking || state.scheduleActivated
    }
    
    fun hasUsageStatsPermission(): Boolean {
        return appMonitor.hasUsageStatsPermission()
    }
    
    fun hasOverlayPermission(): Boolean {
        return appMonitor.isOverlayPermissionGranted()
    }
    
    fun getInstalledApps(): List<String> {
        return appMonitor.getInstalledApps(includeSystemApps = false)
    }
    
    private fun startServiceIfNeeded() {
        val state = preferencesManager.loadState()
        
        if (state.isBlocking || state.scheduledTime != null) {
            BlockerService.startService(context)
        }
    }
    
    private fun stopServiceIfNotNeeded() {
        val state = preferencesManager.loadState()
        
        if (!state.isBlocking && state.scheduledTime == null) {
            BlockerService.stopService(context)
        }
    }
}
