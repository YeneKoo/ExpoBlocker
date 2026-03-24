package expo.modules.appblockerengine.blocker.model

data class BlockerState(
    val isBlocking: Boolean = false,
    val blockedApps: List<String> = emptyList(),
    val blockAll: Boolean = false,
    val scheduledTime: String? = null,
    val scheduleActivated: Boolean = false
)
