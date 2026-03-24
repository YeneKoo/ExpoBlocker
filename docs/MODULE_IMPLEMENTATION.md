# App Blocker Engine - Production-Ready Android Module

## Build Status
✅ **Build Successful** - AAR generated at `android/appblocker/build/outputs/aar/appblocker-debug.aar`

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

```
android/appblocker/
├── build.gradle
├── src/main/
│   ├── AndroidManifest.xml
│   ├── kotlin/com/appblocker/
│   │   ├── controller/
│   │   │   └── OverlayController.kt
│   │   ├── manager/
│   │   │   └── AppBlockerManager.kt
│   │   ├── model/
│   │   │   └── BlockerState.kt
│   │   ├── monitor/
│   │   │   └── AppMonitor.kt
│   │   ├── receiver/
│   │   │   └── BootReceiver.kt
│   │   ├── service/
│   │   │   └── BlockerService.kt
│   │   ├── storage/
│   │   │   └── PreferencesManager.kt
│   │   └── util/
│   │       └── TimeUtils.kt
│   └── res/values/
│       └── strings.xml
```

## API Reference

### AppBlockerManager

```kotlin
class AppBlockerManager {
    fun block(apps: List<String>?)
    fun clear()
    fun schedule(time: String): Boolean
    fun getState(): BlockerState
    fun isBlocking(): Boolean
    fun hasUsageStatsPermission(): Boolean
    fun hasOverlayPermission(): Boolean
    fun getInstalledApps(): List<String>
}
```

## Blocking Behavior

### `block(apps: List<String>?)`
- **With apps list**: Blocks only specified package names
- **Without apps list**: Blocks ALL non-system apps
- **Immediate activation**: Blocking starts right away
- **Persistence**: Remains active until `clear()` or new schedule

### `clear()`
- Stops all blocking immediately
- Cancels any active schedule
- Removes overlay instantly
- Updates persistent state

### `schedule(time: String): Boolean`
- **Format**: "HH:mm" (24-hour, e.g., "19:00")
- **Returns**: `true` if valid, `false` if invalid
- **Behavior**:
  - Before scheduled time → no blocking
  - After scheduled time → activates blocking
- **Persistence**: Survives app/service restarts

## Permissions

| Permission | Purpose | Grant Method |
|-----------|---------|--------------|
| `PACKAGE_USAGE_STATS` | Detect foreground apps | Settings → Usage Access |
| `SYSTEM_ALERT_WINDOW` | Display blocking overlay | Settings → Overlay |
| `FOREGROUND_SERVICE` | Background monitoring | Auto-granted |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ FGS type | Auto-granted |
| `RECEIVE_BOOT_COMPLETED` | Auto-restart after reboot | Auto-granted |
| `QUERY_ALL_PACKAGES` | List installed apps | Auto-granted |
| `POST_NOTIFICATIONS` | Show notifications | Runtime (Android 13+) |

## Permissions Check Flow

```kotlin
val manager = AppBlockerManager.getInstance(context)

// Check permissions
if (!manager.hasUsageStatsPermission()) {
    // Open Settings for user to grant
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    startActivity(intent)
}

if (!manager.hasOverlayPermission()) {
    // Open Settings for user to grant
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    startActivity(intent)
}
```

## React Native Integration

### Step 1: Create Native Module Class

```kotlin
// AppBlockerModule.kt (in your Expo/React Native module)
class AppBlockerModule(reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext) {
    
    private val manager = AppBlockerManager.getInstance(reactContext)
    
    override fun getName() = "AppBlocker"
    
    @ReactMethod
    fun block(apps: ReadableArray?, promise: Promise) {
        try {
            val appsList = apps?.toArrayList()?.filterIsInstance<String>()
            manager.block(appsList)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("BLOCK_ERROR", e.message)
        }
    }
    
    @ReactMethod
    fun clear(promise: Promise) {
        try {
            manager.clear()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CLEAR_ERROR", e.message)
        }
    }
    
    @ReactMethod
    fun schedule(time: String, promise: Promise) {
        try {
            val success = manager.schedule(time)
            promise.resolve(success)
        } catch (e: Exception) {
            promise.reject("SCHEDULE_ERROR", e.message)
        }
    }
    
    // ... other methods
}
```

### Step 2: Create React Package

```kotlin
class AppBlockerPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): 
        List<NativeModule> {
        return listOf(AppBlockerModule(reactContext))
    }
}
```

### Step 3: Register in MainApplication

```kotlin
override fun getPackages(): List<ReactPackage> =
    PackageList(this).packages.apply {
        add(AppBlockerPackage())
    }
```

### Step 4: TypeScript Wrapper

```typescript
// AppBlocker.ts
import { NativeModules } from 'react-native';

const { AppBlocker } = NativeModules;

export const block = (apps?: string[]) => AppBlocker.block(apps);
export const clear = () => AppBlocker.clear();
export const schedule = (time: string) => AppBlocker.schedule(time);
export const getState = () => AppBlocker.getState();
export const isBlocking = () => AppBlocker.isBlocking();
export const getInstalledApps = () => AppBlocker.getInstalledApps();
export const checkPermissions = () => AppBlocker.checkPermissions();
export const requestUsageStatsPermission = () => 
    AppBlocker.requestUsageStatsPermission();
export const requestOverlayPermission = () => 
    AppBlocker.requestOverlayPermission();

export default { block, clear, schedule, getState, isBlocking, 
                 getInstalledApps, checkPermissions, 
                 requestUsageStatsPermission, requestOverlayPermission };
```

## Manual Testing Instructions

### 1. Build the Module
```bash
cd android
./gradlew assembleDebug
```

### 2. Build Outputs
```bash
# AAR location
android/appblocker/build/outputs/aar/appblocker-debug.aar

# Classes JAR for verification
unzip -l android/appblocker/build/outputs/aar/appblocker-debug.aar classes.jar
```

### 3. Test App Integration

Create a test app that includes the AAR:

```groovy
// app/build.gradle
dependencies {
    implementation project(':appblocker')
}
```

### 4. Test Commands (via ADB)

```bash
# Block specific apps
adb shell am start -a com.appblocker.action.BLOCK \
  --es apps "com.android.chrome,com.instagram.android"

# Block all apps
adb shell am start -a com.appblocker.action.BLOCK_ALL

# Schedule blocking
adb shell am start -a com.appblocker.action.SCHEDULE \
  --es time "21:00"

# Clear blocking
adb shell am start -a com.appblocker.action.CLEAR
```

### 5. Logcat for Debugging

```bash
adb logcat -s BlockerService AppMonitor OverlayController
```

## Accessibility Service Upgrade (Future Enhancement)

For improved blocking reliability, consider implementing AccessibilityService:

### Advantages
- Real-time app switching detection (no polling)
- Works even when app is in background
- More responsive blocking

### Implementation

```kotlin
class BlockerAccessibilityService : AccessibilityService() {
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            val state = preferencesManager.loadState()
            if (!state.isBlocking && !state.scheduleActivated) return
            
            val shouldBlock = appMonitor.shouldBlockPackage(
                packageName,
                state.blockedApps,
                state.blockAll
            )
            
            if (shouldBlock) {
                overlayController.showOverlay(packageName)
            } else {
                overlayController.hideOverlay()
            }
        }
    }
    
    override fun onInterrupt() {
        // Handle service interruption
    }
}
```

### Manifest Addition

```xml
<service
    android:name=".accessibility.BlockerAccessibilityService"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### accessibility_service_config.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="false"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity="com.example.app.MainActivity" />
```

## Edge Cases Handled

| Edge Case | Handling |
|-----------|----------|
| User revokes overlay permission | Service continues; blocking inactive until re-granted |
| Usage access not granted | getCurrentForegroundApp() returns null; no blocking |
| Device kills service | START_STICKY + onTaskRemoved auto-restarts |
| Invalid time format | Returns false; state unchanged |
| No apps to block | Treated as block-all mode |
| Service destroyed during blocking | State persists; auto-restart on next check |
| Schedule time passed on startup | scheduleActivated flag handles immediate activation |

## Compiled Classes

All Kotlin classes successfully compiled:

```
✓ com.appblocker.controller.OverlayController
✓ com.appblocker.manager.AppBlockerManager
✓ com.appblocker.model.BlockerState
✓ com.appblocker.monitor.AppMonitor
✓ com.appblocker.receiver.BootReceiver
✓ com.appblocker.service.BlockerService
✓ com.appblocker.storage.PreferencesManager
✓ com.appblocker.util.TimeUtils
```

## Future React Native Bridge

The module is designed for easy React Native integration:

1. **Create Native Module**: Wrap AppBlockerManager with ReactContextBaseJavaModule
2. **Create React Package**: Register the module in getPackages()
3. **TypeScript Types**: Already defined in `src/ExpoBlocker.types.ts`
4. **Native Module File**: Already exists at `src/ExpoBlockerModule.ts`

## Usage Example

```typescript
import AppBlocker from './src/AppBlocker';

// Request permissions
const perms = await AppBlocker.checkPermissions();
if (!perms.usageStats || !perms.overlay) {
    await AppBlocker.requestUsageStatsPermission();
    await AppBlocker.requestOverlayPermission();
}

// Block specific apps
await AppBlocker.block(['com.instagram.android', 'com.facebook.katana']);

// Or block all non-system apps
await AppBlocker.blockAll();

// Schedule blocking at 9 PM
await AppBlocker.schedule('21:00');

// Check state
const state = await AppBlocker.getState();
console.log(state.isBlocking);

// Clear when done
await AppBlocker.clear();
```

## Build Information

- **Gradle**: 8.11.1
- **Kotlin**: 2.0.21
- **AGP**: 8.3.0
- **Compile SDK**: 34
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Java**: 23
