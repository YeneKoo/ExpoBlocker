package expo.modules.appblockerengine.blocker.manager

import android.content.Context
import expo.modules.appblockerengine.blocker.model.AppInfo
import expo.modules.appblockerengine.blocker.model.AppUsageStats
import expo.modules.appblockerengine.blocker.model.BlockerState
import expo.modules.appblockerengine.blocker.model.OverlayConfig
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
    
    fun block(apps: List<String>?, excludeApps: List<String> = emptyList()) {
        val currentState = preferencesManager.loadState()
        
        val mergedExcludeApps = (excludeApps + currentState.excludeApps + context.packageName).distinct()
        
        val state = BlockerState(
            isBlocking = true,
            blockedApps = apps ?: emptyList(),
            blockAll = apps == null || apps.isEmpty(),
            scheduledTime = currentState.scheduledTime,
            scheduledAtMillis = currentState.scheduledAtMillis,
            scheduleActivated = currentState.scheduleActivated,
            excludeApps = mergedExcludeApps
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
            scheduledAtMillis = null,
            scheduleActivated = false,
            excludeApps = emptyList()
        )
        
        preferencesManager.saveState(clearedState)
        stopServiceIfNotNeeded()
    }
    
    fun schedule(time: String, excludeApps: List<String> = emptyList()): Boolean {
        val dateTime = TimeUtils.getNextDateTimeForTime(time) ?: return false
        return scheduleAt(dateTime, excludeApps)
    }
    
    fun scheduleAt(dateTime: String, excludeApps: List<String> = emptyList()): Boolean {
        val scheduledAtMillis = TimeUtils.parseDateTimeToMillis(dateTime) ?: return false
        
        val currentState = preferencesManager.loadState()
        
        val scheduleActivated = System.currentTimeMillis() >= scheduledAtMillis
        
        val mergedExcludeApps = (excludeApps + currentState.excludeApps + context.packageName).distinct()
        
        val state = BlockerState(
            isBlocking = currentState.isBlocking || scheduleActivated,
            blockedApps = currentState.blockedApps,
            blockAll = currentState.blockAll,
            scheduledTime = dateTime,
            scheduledAtMillis = scheduledAtMillis,
            scheduleActivated = scheduleActivated,
            excludeApps = mergedExcludeApps
        )
        
        preferencesManager.saveState(state)
        startServiceIfNeeded()
        
        return true
    }
    
    fun setExcludeApps(apps: List<String>) {
        val currentState = preferencesManager.loadState()
        val mergedExcludeApps = (apps + context.packageName).distinct()
        
        val newState = currentState.copy(excludeApps = mergedExcludeApps)
        preferencesManager.saveState(newState)
    }
    
    fun updateOverlayConfig(config: OverlayConfig) {
        preferencesManager.saveOverlayConfig(config)
    }
    
    fun getOverlayConfig(): OverlayConfig {
        return preferencesManager.loadOverlayConfig()
    }
    
    fun getState(): BlockerState {
        val state = preferencesManager.loadState()
        
        if (state.scheduleActivated) {
            val scheduledAt = state.scheduledAtMillis
            if (scheduledAt == null || System.currentTimeMillis() < scheduledAt) {
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
    
    fun getAppName(packageName: String): String {
        return appMonitor.getAppName(packageName)
    }
    
    fun getAppIconBase64(packageName: String): String? {
        return appMonitor.getAppIconBase64(packageName)
    }
    
    fun getUsageStats(): List<AppUsageStats> {
        return appMonitor.getTodayUsageStats()
    }
    
    fun getUsageTimeForPackage(packageName: String): Long {
        return appMonitor.getUsageTimeForPackage(packageName)
    }
    
    private fun startServiceIfNeeded() {
        val state = preferencesManager.loadState()
        
        if (state.isBlocking || state.scheduledAtMillis != null) {
            BlockerService.startService(context)
        }
    }
    
    private fun stopServiceIfNotNeeded() {
        val state = preferencesManager.loadState()
        
        if (!state.isBlocking && state.scheduledAtMillis == null) {
            BlockerService.stopService(context)
        }
    }
}