# App Blocker Engine - Android Native Module

A production-ready Android Kotlin module that acts as an **app blocker engine**, designed to block selected apps or all non-system apps, with scheduling support and persistent background operation.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AppBlockerManager                        │
│                  (Main API Interface)                       │
└─────────────────────────────┬───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │ Preferences │  │   AppMonitor │  │BlockerService│
    │  Manager    │  │ (Detection) │  │(Foreground)  │
    └─────────────┘  └─────────────┘  └──────┬──────┘
                                              │
                                      ┌───────┴───────┐
                                      ▼               ▼
                              ┌───────────┐   ┌─────────────┐
                              │  Overlay  │   │  Boot       │
                              │ Controller│   │  Receiver   │
                              └───────────┘   └─────────────┘
```

## Module Structure

### Core Components

| File | Purpose |
|------|---------|
| `AppBlockerManager.kt` | Main API interface, coordinates all operations |
| `BlockerService.kt` | Foreground service for continuous monitoring |
| `AppMonitor.kt` | App detection using UsageStatsManager |
| `OverlayController.kt` | Full-screen overlay for blocking |
| `PreferencesManager.kt` | SharedPreferences-based state persistence |
| `BootReceiver.kt` | BOOT_COMPLETED broadcast receiver |
| `TimeUtils.kt` | Time parsing and schedule validation |
| `BlockerState.kt` | Data model for blocking state |

## API Reference

### AppBlockerManager

```kotlin
class AppBlockerManager {
    fun block(apps: List<String>?)
    fun clear()
    fun schedule(time: String): Boolean
    fun getState(): BlockerState
    fun isBlocking(): Boolean
}
```

### Methods

#### `block(apps: List<String>?)`
- **Parameter**: `apps` - List of package names to block, or null to block all non-system apps
- **Behavior**: 
  - If `apps` is provided → block only those packages
  - If `apps` is null/empty → block ALL non-system apps
  - Triggers immediately upon call
  - Persists until `clear()` or new schedule

#### `clear()`
- Stops all blocking immediately
- Cancels any active schedule
- Removes overlay instantly
- Updates persistent state

#### `schedule(time: String): Boolean`
- **Parameter**: `time` - Time in "HH:mm" (24h) format
- **Returns**: `true` if valid, `false` if invalid format
- **Behavior**:
  - Before scheduled time → no blocking
  - Once current time >= scheduled time → activates blocking
  - Blocking continues until `clear()` or new schedule
  - Survives app/service restarts

## Permissions

### Required Permissions

| Permission | Purpose | Grant Method |
|------------|---------|--------------|
| `PACKAGE_USAGE_STATS` | Detect foreground apps | Settings → Usage Access |
| `SYSTEM_ALERT_WINDOW` | Display blocking overlay | Settings → Display over other apps |
| `FOREGROUND_SERVICE` | Run persistent background service | Automatically granted |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ foreground service type | Automatically granted |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot | Automatically granted |
| `POST_NOTIFICATIONS` | Show foreground notification (Android 13+) | Runtime permission |
| `QUERY_ALL_PACKAGES` | Query installed packages | Automatically granted |

### Permission Flow

```typescript
// Check current permission status
const permissions = await AppBlocker.checkPermissions();

// Request permissions
if (!permissions.usageStats) {
    await AppBlocker.requestUsageStatsPermission();
    // User must manually grant in Settings
}

if (!permissions.overlay) {
    await AppBlocker.requestOverlayPermission();
    // User must manually grant in Settings
}
```

## Blocking Mechanism

### Detection Method
- **Primary**: `UsageStatsManager` - Queries usage statistics every 500ms
- **Polling Interval**: 500ms (configurable in `BlockerService.kt`)

### Blocking Method
- **Overlay**: Full-screen, non-dismissible overlay using `SYSTEM_ALERT_WINDOW`
- **UI**: Simple "App Blocked" message (customizable)

### System App Detection
```kotlin
fun isSystemApp(appInfo: ApplicationInfo): Boolean {
    return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
           (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}
```

## State Persistence

### SharedPreferences Keys
| Key | Type | Description |
|-----|------|-------------|
| `is_blocking` | Boolean | Whether blocking is active |
| `blocked_apps` | Set<String> | List of blocked package names |
| `block_all` | Boolean | Whether blocking all non-system apps |
| `scheduled_time` | String | Scheduled activation time (HH:mm) |
| `schedule_activated` | Boolean | Whether schedule has been triggered |

## Service Lifecycle

### Start Conditions
1. `block()` called
2. `schedule()` called with valid time
3. Device boot (if state was active)

### Stop Conditions
1. `clear()` called
2. No active blocking and no schedule

### Auto-Restart
- Service uses `START_STICKY` for crash recovery
- `onTaskRemoved()` checks state and restarts if needed

## Usage Examples

### Block Specific Apps
```typescript
await AppBlocker.block(['com.instagram.android', 'com.facebook.katana']);
```

### Block All Non-System Apps
```typescript
await AppBlocker.block(); // or await AppBlocker.blockAll();
```

### Schedule Blocking
```typescript
await AppBlocker.schedule('21:00'); // Blocks at 9 PM
```

### Clear Blocking
```typescript
await AppBlocker.clear();
```

### Check State
```typescript
const state = await AppBlocker.getState();
console.log(state.isBlocking);
console.log(state.blockedApps);
```

## Manual Testing Instructions

### 1. Build the Module
```bash
cd android
./gradlew assembleDebug
```

### 2. Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Permissions
```bash
# Grant overlay permission
adb shell appops set expo.modules.appblockerengine SYSTEM_ALERT_WINDOW allow

# Grant usage stats permission (requires manual intervention)
adb shell am start -a android.settings.USAGE_ACCESS_SETTINGS
```

### 4. Test Blocking
```bash
# Start blocking a specific app
adb shell am start -a expo.modules.appblockerengine.action.BLOCK --es apps "com.android.chrome"

# Start blocking all apps
adb shell am start -a expo.modules.appblockerengine.action.BLOCK_ALL

# Schedule blocking
adb shell am start -a expo.modules.appblockerengine.action.SCHEDULE --es time "19:00"

# Clear blocking
adb shell am start -a expo.modules.appblockerengine.action.CLEAR
```

### 5. Verify Service
```bash
# Check if service is running
adb shell dumpsys activity services expo.modules.appblockerengine

# View logs
adb logcat -s BlockerService AppMonitor OverlayController
```

## React Native Integration

### Basic Usage
```typescript
import AppBlocker from 'expo-blocker';

// Check permissions first
const permissions = await AppBlocker.checkPermissions();
if (!permissions.usageStats || !permissions.overlay) {
    // Show permission request UI
}

// Block specific apps
await AppBlocker.block(['com.instagram.android']);

// Schedule blocking
await AppBlocker.schedule('22:00');

// Clear when done
await AppBlocker.clear();
```

### React Native Bridge Structure
```typescript
// ExpoBlockerModule.ts
declare class ExpoBlockerModule extends NativeModule {
  block(apps: string[] | null): Promise<{ success: boolean }>;
  clear(): Promise<{ success: boolean }>;
  schedule(time: string): Promise<{ success: boolean }>;
  // ... other methods
}
```

## Accessibility Service (Future Enhancement)

For improved blocking reliability, consider implementing an AccessibilityService:

### Advantages
- Real-time app switching detection (no polling)
- Works even when app is in background
- More responsive blocking

### Implementation Notes
```kotlin
class BlockerAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            // Check and block if needed
        }
    }
}
```

## Edge Cases Handled

| Edge Case | Handling |
|-----------|----------|
| User revokes overlay permission | Service detects and shows notification to re-grant |
| Usage access not granted | Returns null from getCurrentForegroundApp(), no blocking |
| Device kills service | START_STICKY + onTaskRemoved restarts service |
| Invalid time format | Returns false, does not update state |
| No apps to block | Treated as block-all mode |
| Service destroyed during blocking | State persists, auto-restart on next check |

## File Locations

```
android/src/main/java/expo/modules/appblockerengine/
├── AndroidManifest.xml
├── ExpoBlockerModule.kt
└── blocker/
    ├── controller/
    │   └── OverlayController.kt
    ├── manager/
    │   └── AppBlockerManager.kt
    ├── model/
    │   └── Models.kt (OverlayConfig, BlockerState, AppInfo, etc.)
    ├── monitor/
    │   └── AppMonitor.kt
    ├── receiver/
    │   └── BootReceiver.kt
    ├── service/
    │   └── BlockerService.kt
    ├── storage/
    │   └── PreferencesManager.kt
    └── util/
        └── TimeUtils.kt
```

## Overlay Configuration

The `OverlayConfig` data class in `Models.kt` defines the appearance of the blocking overlay:

```kotlin
data class OverlayConfig(
    val title: String = "App Blocked",
    val message: String? = null,
    val description: String? = null,
    val backgroundColor: String = "#1A1A1A",    // Hex color string
    val textColor: String = "#FFFFFF",           // Hex color string
    val titleTextSize: Float = 32f,
    val messageTextSize: Float = 18f,
    val descriptionTextSize: Float = 16f,
    val showAppIcon: Boolean = true,
    val showAppName: Boolean = true,
    val showUsageStats: Boolean = false,
    val showTodayUsage: Boolean = false,
    val blockerAppName: String? = null,
    val buttonText: String? = null,
    val buttonLink: String? = null,              // URL/scheme to navigate to
    val buttonColor: String = "#4CAF50",         // Hex color string
    val buttonTextColor: String = "#FFFFFF",    // Hex color string
    val buttonBorderRadius: Float = 50f,
    val buttonWidth: Float = 280f,
    val buttonHeight: Float = 60f,
    val buttonMarginTop: Float = 40f,
    val showCloseButton: Boolean = false,
    val closeButtonColor: String = "#666666"     // Hex color string
)
```

### Hex Color Support

Colors are now specified as hex strings (e.g., `"#FF4CAF50"` or `"#4CAF50"`). A helper function converts these to Android color integers:

```kotlin
fun String.toColorInt(): Int {
    val hex = this.removePrefix("#")
    return android.graphics.Color.parseColor("#$hex")
}
```

### Button Link Navigation

When `buttonLink` is set, clicking the button will:
1. Navigate to the specified URL/scheme using an Intent
2. Fire the `onButtonClicked` event (for callback handling)

Supported link formats:
- `expo://home` - Expo deep links
- `myapp://settings` - Custom app schemes
- `https://example.com` - Web URLs

### Button Click Event

The button click triggers two actions:
1. **Navigation**: If `buttonLink` is set, opens the URL/scheme
2. **Event**: Fires `onButtonClicked` event for JS callback handling

```kotlin
button.setOnClickListener {
    val link = config.buttonLink
    if (!link.isNullOrEmpty()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Fire event for JS callback
    val intent = Intent(ACTION_BUTTON_CLICKED)
    intent.putExtra("packageName", appInfo.packageName)
    context.sendBroadcast(intent)
}
```

## Limitations

1. **Battery Impact**: Continuous foreground service with polling may impact battery
2. **Permission Dependency**: Requires user to grant special permissions
3. **Android Version**: Optimized for Android 8+ (API 26+)
4. **Overlay**: Some devices have aggressive battery optimization that may kill the service
