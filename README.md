# App Blocker Engine - Android Module

A production-ready Android Kotlin module for blocking apps, designed for React Native integration.

## Quick Start

### Build
```bash
cd android
./gradlew assembleDebug
```

### Output
```
android/appblocker/build/outputs/aar/appblocker-debug.aar
```

### Test with ADB
```bash
# Grant permissions
adb shell appops set com.example.app PACKAGE_USAGE_STATS allow
adb shell appops set com.example.app SYSTEM_ALERT_WINDOW allow

# Start blocking
adb shell am startservice -n com.appblocker/.service.BlockerService

# View logs
adb logcat -s BlockerService
```

## Architecture

- **AppBlockerManager**: Main API
- **BlockerService**: Foreground service
- **AppMonitor**: UsageStatsManager integration
- **OverlayController**: SYSTEM_ALERT_WINDOW blocking
- **PreferencesManager**: SharedPreferences persistence
- **BootReceiver**: BOOT_COMPLETED restart

## API

```kotlin
manager.block(apps)     // Block specific apps or all
manager.clear()         // Stop blocking
manager.schedule("21:00") // Schedule at HH:mm
```

## React Native

See `docs/MODULE_IMPLEMENTATION.md` for full integration guide.

TypeScript wrapper: `src/AppBlocker.ts`

## Permissions

- PACKAGE_USAGE_STATS
- SYSTEM_ALERT_WINDOW
- FOREGROUND_SERVICE
- RECEIVE_BOOT_COMPLETED

## Files

```
android/appblocker/src/main/kotlin/com/appblocker/
├── manager/AppBlockerManager.kt
├── service/BlockerService.kt
├── monitor/AppMonitor.kt
├── controller/OverlayController.kt
├── storage/PreferencesManager.kt
├── receiver/BootReceiver.kt
├── model/BlockerState.kt
└── util/TimeUtils.kt
```
