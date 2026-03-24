package expo.modules.appblockerengine.blocker.model

data class BlockerState(
    val isBlocking: Boolean = false,
    val blockedApps: List<String> = emptyList(),
    val blockAll: Boolean = false,
    val scheduledTime: String? = null,
    val scheduleActivated: Boolean = false,
    val excludeApps: List<String> = emptyList()
)

data class OverlayConfig(
    val title: String = "App Blocked",
    val message: String = "This app has been blocked",
    val backgroundColor: Int = 0xFF1A1A1A.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val titleTextSize: Float = 32f,
    val messageTextSize: Float = 18f,
    val showAppIcon: Boolean = true,
    val showAppName: Boolean = true,
    val showUsageStats: Boolean = true
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconBase64: String?,
    val usageTime: Long // in milliseconds
)

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val iconBase64: String?,
    val usageTime: Long,
    val lastTimeUsed: Long
)
