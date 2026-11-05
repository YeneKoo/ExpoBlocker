package expo.modules.appblockerengine.blocker.manager

import android.content.Context
import expo.modules.appblockerengine.blocker.model.AppInfo
import expo.modules.appblockerengine.blocker.model.AppUsageStats
import expo.modules.appblockerengine.blocker.model.BlockerState
import expo.modules.appblockerengine.blocker.model.OverlayConfig
import expo.modules.appblockerengine.blocker.model.ScreenSession
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
        // An emptied selection blocks nothing, so it ends any schedule too. Only an
        // explicit null blocks everything.
        if (apps != null && apps.isEmpty()) {
            clear()
            return
        }

        val currentState = preferencesManager.loadState()
        
        val mergedExcludeApps = (excludeApps + currentState.excludeApps + context.packageName).distinct()
        
        val state = BlockerState(
            isBlocking = true,
            blockedApps = apps ?: emptyList(),
            blockAll = apps == null,
            scheduledTime = currentState.scheduledTime,
            scheduledAtMillis = currentState.scheduledAtMillis,
            scheduleActivated = currentState.scheduleActivated,
            excludeApps = mergedExcludeApps,
            pausedUntilMillis = null
        )
        
        preferencesManager.saveState(state)
        startServiceIfNeeded()
    }
    
    // Keeps the list and the running service; the service resumes blocking on its
    // own at untilMillis. Writes state only and never starts the service, so it is
    // safe to call while the app is in the background.
    fun pauseUntil(untilMillis: Long) {
        val currentState = preferencesManager.loadState()
        preferencesManager.saveState(currentState.copy(pausedUntilMillis = untilMillis))
    }
    
    fun resume() {
        val currentState = preferencesManager.loadState()
        preferencesManager.saveState(currentState.copy(pausedUntilMillis = null))
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
        if (state.isPausedAt(System.currentTimeMillis())) return false
        return state.isBlocking || state.scheduleActivated
    }
    
    fun hasUsageStatsPermission(): Boolean {
        return appMonitor.hasUsageStatsPermission()
    }
    
    fun hasOverlayPermission(): Boolean {
        return appMonitor.isOverlayPermissionGranted()
    }
    
    fun getInstalledApps(): List<String> {
        return appMonitor.getInstalledApps()
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

    fun getScreenSessions(startMillis: Long, endMillis: Long): List<ScreenSession>? {
        return appMonitor.getScreenSessions(startMillis, endMillis)
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