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
    val backgroundColor: String = "#1A1A1A",
    val textColor: String = "#FFFFFF",
    val titleTextSize: Float = 32f,
    val messageTextSize: Float = 18f,
    val descriptionTextSize: Float = 16f,
    val showAppIcon: Boolean = true,
    val showAppName: Boolean = true,
    val showUsageStats: Boolean = false,
    val showTodayUsage: Boolean = false,
    val blockerAppName: String? = null,
    val buttonText: String? = null,
    val buttonLink: String? = null,
    val buttonColor: String = "#4CAF50",
    val buttonTextColor: String = "#FFFFFF",
    val buttonBorderRadius: Float = 50f,
    val buttonWidth: Float = 280f,
    val buttonHeight: Float = 60f,
    val buttonMarginTop: Float = 40f,
    val showCloseButton: Boolean = false,
    val closeButtonColor: String = "#666666"
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

fun String.toColorInt(): Int {
    val hex = this.removePrefix("#")
    return android.graphics.Color.parseColor("#$hex")
}
