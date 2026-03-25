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
    val message: String? = null,
    val description: String? = null,
    val backgroundColor: Int = 0xFF1A1A1A.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val titleTextSize: Float = 32f,
    val messageTextSize: Float = 18f,
    val descriptionTextSize: Float = 16f,
    val showAppIcon: Boolean = true,
    val showAppName: Boolean = true,
    val showUsageStats: Boolean = false,
    val showTodayUsage: Boolean = false,
    val blockerAppName: String? = null,
    val buttonText: String? = null,
    val buttonColor: Int = 0xFF4CAF50.toInt(),
    val buttonTextColor: Int = 0xFFFFFFFF.toInt(),
    val buttonBorderRadius: Float = 50f,
    val buttonWidth: Float = 280f,
    val buttonHeight: Float = 60f,
    val buttonMarginTop: Float = 40f,
    val showCloseButton: Boolean = false,
    val closeButtonColor: Int = 0xFF666666.toInt()
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconBase64: String?,
    val usageTime: Long
)

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val iconBase64: String?,
    val usageTime: Long,
    val lastTimeUsed: Long
)
