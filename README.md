# expo-blocker

A production-ready app blocker for React Native / Expo with scheduling, app usage tracking, and customizable UI.

## Features

- **Block specific apps** or all non-system apps
- **Schedule blocking** at specific times (HH:mm format)
- **Exclude apps** from being blocked
- **App usage statistics** - track how much time you spend on apps
- **Customizable overlay UI** - change title, message, colors, and what info is shown
- **Foreground service** for reliable background operation
- **Boot persistence** - auto-restart after device reboot
- **Get app icons and names** for nice UI display

## Installation

```bash
npx expo install expo-blocker
```

Or with npm:
```bash
npm install expo-blocker
```

## Required Permissions

This module requires two special permissions that users must grant manually:

### 1. Usage Access (PACKAGE_USAGE_STATS)
Required to detect which app is currently in the foreground.

```typescript
// Check permission
const hasUsagePermission = await AppBlocker.hasUsageStatsPermission();

// Request permission (opens system settings)
await AppBlocker.requestUsageStatsPermission();
```

### 2. Overlay Permission (SYSTEM_ALERT_WINDOW)
Required to display the blocking overlay on top of other apps.

```typescript
// Check permission
const hasOverlayPermission = await AppBlocker.hasOverlayPermission();

// Request permission (opens system settings)
await AppBlocker.requestOverlayPermission();
```

## Usage

### Check Permissions

```typescript
import AppBlocker from 'expo-blocker';

const permissions = await AppBlocker.checkPermissions();
console.log(permissions);
// { usageStats: true, overlay: false }
```

### Block Specific Apps

```typescript
// Block Instagram and Facebook
await AppBlocker.block(['com.instagram.android', 'com.facebook.katana']);
```

### Block All Non-System Apps

```typescript
await AppBlocker.block();
```

### Exclude Apps (Whitelist)

```typescript
// Apps in this list will never be blocked
await AppBlocker.block(null, ['com.yourapp.package', 'com.android.settings']);
```

### Schedule Blocking

```typescript
// Block apps starting at 9 PM
await AppBlocker.schedule('21:00');

// With excluded apps
await AppBlocker.schedule('21:00', ['com.yourapp.package']);
```

### Clear Blocking

```typescript
await AppBlocker.clear();
```

### Get App Usage Statistics

```typescript
// Get today's usage for all apps
const stats = await AppBlocker.getUsageStats();
console.log(stats);
// [
//   { packageName: 'com.instagram.android', appName: 'Instagram', usageTime: 3600000, usageTimeFormatted: '1h 0m', ... },
//   { packageName: 'com.facebook.katana', appName: 'Facebook', usageTime: 1800000, usageTimeFormatted: '30m', ... },
// ]

// Get usage for specific app
const usage = await AppBlocker.getAppUsageTime('com.instagram.android');
console.log(usage);
// { usageTime: 3600000, usageTimeFormatted: '1h 0m' }
```

### Get App Info

```typescript
// Get app name
const name = await AppBlocker.getAppName('com.instagram.android');
// 'Instagram'

// Get app icon as base64
const icon = await AppBlocker.getAppIcon('com.instagram.android');
// 'iVBORw0KGgoAAAANSUhEUgAAAAEAAA...'
```

### Customize Overlay UI

```typescript
await AppBlocker.updateOverlayConfig({
  title: 'App Blocked 🔒',
  message: 'Time to focus on something else!',
  backgroundColor: 0xFF1A1A1A,  // Dark background
  textColor: 0xFFFFFFFF,        // White text
  showAppIcon: true,            // Show app icon on overlay
  showAppName: true,            // Show app name on overlay
  showUsageStats: true          // Show today's usage time
});
```

### Get/Set Excluded Apps

```typescript
// Get current excluded apps
const excluded = await AppBlocker.getExcludeApps();

// Set excluded apps
await AppBlocker.setExcludeApps(['com.yourapp.package']);
```

### Get Blocking State

```typescript
const state = await AppBlocker.getState();
console.log(state);
// {
//   isBlocking: true,
//   blockedApps: ['com.instagram.android'],
//   blockAll: false,
//   scheduledTime: null,
//   scheduleActivated: false,
//   excludeApps: ['com.yourapp.package']
// }
```

## Complete Example

```typescript
import React, { useEffect, useState } from 'react';
import { Button, Text, View } from 'react-native';
import AppBlocker from 'expo-blocker';

export default function App() {
  const [permissions, setPermissions] = useState(null);

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    const perms = await AppBlocker.checkPermissions();
    setPermissions(perms);
  };

  const startBlocking = async () => {
    // Customize the overlay
    await AppBlocker.updateOverlayConfig({
      title: 'Blocked 🔒',
      message: 'Stay focused!',
      showAppIcon: true,
      showAppName: true,
      showUsageStats: true
    });

    // Block Instagram with your app excluded
    await AppBlocker.block(['com.instagram.android'], ['com.yourapp.package']);
  };

  if (!permissions?.usageStats || !permissions?.overlay) {
    return (
      <View>
        <Text>Please grant permissions first!</Text>
        <Button title="Grant Usage Stats" onPress={() => AppBlocker.requestUsageStatsPermission()} />
        <Button title="Grant Overlay" onPress={() => AppBlocker.requestOverlayPermission()} />
      </View>
    );
  }

  return (
    <View>
      <Button title="Start Blocking" onPress={startBlocking} />
      <Button title="Clear" onPress={() => AppBlocker.clear()} />
    </View>
  );
}
```

## API Reference

| Method | Parameters | Description |
|--------|------------|-------------|
| `block(apps?, excludeApps?)` | `string[]`, `string[]` | Block apps. Pass `null` for all non-system apps |
| `blockAll(excludeApps?)` | `string[]` | Block all non-system apps |
| `clear()` | - | Stop all blocking |
| `schedule(time, excludeApps?)` | `string` (HH:mm), `string[]` | Schedule blocking at time |
| `getState()` | - | Get current blocking state |
| `isBlocking()` | - | Check if blocking is active |
| `checkPermissions()` | - | Check all permissions |
| `hasUsageStatsPermission()` | - | Check usage access permission |
| `hasOverlayPermission()` | - | Check overlay permission |
| `requestUsageStatsPermission()` | - | Open usage access settings |
| `requestOverlayPermission()` | - | Open overlay settings |
| `getInstalledApps()` | - | Get list of non-system apps |
| `getAppName(package)` | `string` | Get app name from package |
| `getAppIcon(package)` | `string` | Get app icon as base64 |
| `getUsageStats()` | - | Get today's app usage stats |
| `getAppUsageTime(package)` | `string` | Get usage for specific app |
| `setExcludeApps(apps)` | `string[]` | Set apps to exclude |
| `getExcludeApps()` | - | Get excluded apps list |
| `updateOverlayConfig(config)` | `OverlayConfig` | Customize overlay UI |
| `getOverlayConfig()` | - | Get current overlay config |

## OverlayConfig Options

```typescript
{
  title?: string,           // Default: "App Blocked"
  message?: string,          // Default: "This app has been blocked"
  backgroundColor?: number,   // Default: 0xFF1A1A1A
  textColor?: number,        // Default: 0xFFFFFFFF
  titleTextSize?: number,    // Default: 32
  messageTextSize?: number,  // Default: 18
  showAppIcon?: boolean,     // Default: true
  showAppName?: boolean,     // Default: true
  showUsageStats?: boolean  // Default: true
}
```

## AppUsageStat Object

```typescript
{
  packageName: string,           // Package name (e.g., 'com.instagram.android')
  appName: string,                // Display name (e.g., 'Instagram')
  iconBase64: string | null,      // Base64 encoded PNG icon
  usageTime: number,              // Time in milliseconds
  usageTimeFormatted: string,      // Formatted (e.g., '1h 30m')
  lastTimeUsed: number            // Timestamp of last use
}
```

## Platform Support

- **Android**: Fully supported
- **iOS**: Coming soon (contact us if you need it)

## Permissions Required

| Permission | Purpose | How to Grant |
|------------|---------|--------------|
| `PACKAGE_USAGE_STATS` | Detect foreground apps | Settings → Apps → Special access → Usage access |
| `SYSTEM_ALERT_WINDOW` | Show blocking overlay | Settings → Apps → Special access → Display over other apps |

## Quick Testing with ADB

If you're testing on an emulator or device connected via ADB, you can grant permissions programmatically:

```bash
# Get your app's package name (default: expo.modules.appblockerengine.example)
adb shell pm list packages | grep blocker

# Grant Usage Access permission
adb shell appops set expo.modules.appblockerengine.example PACKAGE_USAGE_STATS allow

# Grant Overlay permission
adb shell appops set expo.modules.appblockerengine.example SYSTEM_ALERT_WINDOW allow

# Restart the app to apply permissions
adb shell am force-stop expo.modules.appblockerengine.example
adb shell am start -n expo.modules.appblockerengine.example/.MainActivity
```

For your own app, replace `expo.modules.appblockerengine.example` with your package name.

## License

MIT
