# iOS Implementation Guide

## Overview

This document provides a detailed technical guide for implementing iOS support for `expo-blocker`. The goal is to maintain API compatibility with the existing Android implementation while leveraging iOS-specific APIs.

## Architecture Comparison: Android vs iOS

### Android Implementation Summary

The Android implementation consists of these key components:

```
┌─────────────────────────────────────────────────────────────────┐
│                        JavaScript Layer                         │
│  (AppBlocker.ts, ExpoBlockerModule.ts, ExpoBlocker.types.ts)  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ExpoBlockerModule.kt                        │
│  - Exposes all JS functions as AsyncFunction                   │
│  - Manages event emission (onButtonClicked)                   │
│  - Delegates to AppBlockerManager                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AppBlockerManager.kt                        │
│  - Core business logic                                         │
│  - State management (BlockerState)                             │
│  - Coordinates between services                               │
└─────────────────────────────────────────────────────────────────┘
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐
│  BlockerService │  │  AppMonitor    │  │ OverlayController   │
│  - Foreground  │  │  - UsageStats  │  │  - WindowManager    │
│    Service      │  │  - getCurrent │  │  - UI Rendering    │
│  - Polling loop │  │    Foreground │  │  - Button clicks   │
└─────────────────┘  └─────────────────┘  └─────────────────────┘
          │
          ▼
┌─────────────────┐  ┌─────────────────┐
│  Preferences   │  │   BootReceiver  │
│    Manager     │  │  - Auto-restart │
│  - State persist│ │                 │
└─────────────────┘  └─────────────────┘
```

### iOS Fundamental Differences

| Feature | Android | iOS |
|---------|---------|-----|
| **App Usage Tracking** | `UsageStatsManager` | `DeviceActivityMonitor` (Family Controls) |
| **Blocking Mechanism** | Overlay window | `DeviceActivity` authorization + ManagedSettings |
| **Permissions** | `PACKAGE_USAGE_STATS` + `SYSTEM_ALERT_WINDOW` | `Family Controls` entitlement |
| **Background Processing** | Foreground Service | `DeviceActivityMonitor` extension |
| **Overlay UI** | `WindowManager` (TYPE_APPLICATION_OVERLAY) | Not possible - use parental controls |

## Key Technical Challenges

### 1. iOS App Blocking is Restricted

iOS does **not** allow arbitrary app blocking like Android:
- Cannot overlay on top of other apps
- Cannot programmatically force-close other apps
- Must use Apple's **Family Controls** framework (requires special entitlement)
- Family Controls are designed for parental supervision, not general app blocking

**Implication**: The iOS implementation must:
- Request `Family Controls` entitlement from Apple
- Use DeviceActivity framework for monitoring
- Use ManagedSettings to apply restrictions
- Show in-app blocking UI instead of system overlay

### 2. API Compatibility Strategy

The same TypeScript API should work for both platforms. The native module will detect the platform and use appropriate implementations:

```typescript
// This API stays the same for both platforms
await AppBlocker.block(['com.instagram.android']);
await AppBlocker.schedule('21:00');
await AppBlocker.getUsageStats();
```

## Implementation Steps

### Step 1: Create iOS Native Module Structure

```
ios/
├── ExpoBlockerModule.swift      # Main module (replace existing stub)
├── AppBlocker/
│   ├── AppBlockerManager.swift  # Core logic (similar to Android)
│   ├── AppMonitor.swift          # Usage tracking (iOS-specific)
│   ├── DeviceActivityHandler.swift  # Screen Time API integration
│   ├── StateManager.swift       # Persistence (UserDefaults)
│   └── OverlayPresenter.swift   # In-app blocking UI
├── DeviceActivityMonitorExtension/
│   └── DeviceActivityMonitorExtension.swift  # Background monitor
└── ManagedSettingsStoreExtension/
    └── ManagedSettingsStoreExtension.swift    # App restrictions
```

### Step 2: Implement ExpoBlockerModule.swift

The iOS module must implement the same functions as the Android module:

```swift
import ExpoModulesCore

public class ExpoBlockerModule: Module {
    
    private let appBlockerManager = AppBlockerManager.shared
    
    public func definition() -> ModuleDefinition {
        Name("ExpoBlocker")
        
        Events("onButtonClicked")
        
        // Blocking functions
        AsyncFunction("block") { (apps: [String]?, promise: Promise) in
            appBlockerManager.block(apps: apps)
            promise.resolve(["success": true])
        }
        
        AsyncFunction("blockWithExclude") { (apps: [String]?, excludeApps: [String], promise: Promise) in
            appBlockerManager.block(apps: apps, excludeApps: excludeApps)
            promise.resolve(["success": true])
        }
        
        AsyncFunction("clear") { (promise: Promise) in
            appBlockerManager.clear()
            promise.resolve(["success": true])
        }
        
        // Scheduling
        AsyncFunction("schedule") { (time: String, promise: Promise) in
            let success = appBlockerManager.schedule(time: time)
            promise.resolve(["success": success])
        }
        
        AsyncFunction("scheduleWithExclude") { (time: String, excludeApps: [String], promise: Promise) in
            let success = appBlockerManager.schedule(time: time, excludeApps: excludeApps)
            promise.resolve(["success": success])
        }
        
        // State queries
        AsyncFunction("getState") { (promise: Promise) in
            let state = appBlockerManager.getState()
            promise.resolve([
                "isBlocking": state.isBlocking,
                "blockedApps": state.blockedApps,
                "blockAll": state.blockAll,
                "scheduledTime": state.scheduledTime,
                "scheduleActivated": state.scheduleActivated,
                "excludeApps": state.excludeApps
            ])
        }
        
        AsyncFunction("isBlocking") { (promise: Promise) in
            promise.resolve(appBlockerManager.isBlocking())
        }
        
        // Permissions (different iOS approach)
        AsyncFunction("hasUsageStatsPermission") { (promise: Promise) in
            promise.resolve(appBlockerManager.hasScreenTimePermission())
        }
        
        AsyncFunction("hasOverlayPermission") { (promise: Promise) in
            // Not applicable on iOS - always return true or handle differently
            promise.resolve(true)
        }
        
        AsyncFunction("requestUsageStatsPermission") { (promise: Promise) in
            appBlockerManager.requestScreenTimeAuthorization()
            promise.resolve(["success": true])
        }
        
        AsyncFunction("requestOverlayPermission") { (promise: Promise) in
            // No-op on iOS
            promise.resolve(["success": true])
        }
        
        // App info
        AsyncFunction("getInstalledApps") { (promise: Promise) in
            promise.resolve(appBlockerManager.getInstalledApps())
        }
        
        AsyncFunction("getAppName") { (bundleId: String, promise: Promise) in
            promise.resolve(appBlockerManager.getAppName(bundleId: bundleId))
        }
        
        AsyncFunction("getAppIcon") { (bundleId: String, promise: Promise) in
            promise.resolve(appBlockerManager.getAppIconBase64(bundleId: bundleId))
        }
        
        // Usage stats
        AsyncFunction("getUsageStats") { (promise: Promise) in
            let stats = appBlockerManager.getUsageStats()
            let result = stats.map { stat in
                [
                    "packageName": stat.bundleId,
                    "appName": stat.appName,
                    "iconBase64": stat.iconBase64 ?? NSNull(),
                    "usageTime": stat.usageTime,
                    "lastTimeUsed": stat.lastTimeUsed,
                    "usageTimeFormatted": stat.usageTimeFormatted
                ]
            }
            promise.resolve(result)
        }
        
        AsyncFunction("getAppUsageTime") { (bundleId: String, promise: Promise) in
            let usageTime = appBlockerManager.getUsageTimeForBundle(bundleId: bundleId)
            promise.resolve([
                "usageTime": usageTime,
                "usageTimeFormatted": formatUsageTime(usageTime)
            ])
        }
        
        AsyncFunction("checkPermissions") { (promise: Promise) in
            promise.resolve([
                "usageStats": appBlockerManager.hasScreenTimePermission(),
                "overlay": true  // Not applicable
            ])
        }
        
        // Exclude apps
        AsyncFunction("setExcludeApps") { (apps: [String], promise: Promise) in
            appBlockerManager.setExcludeApps(apps)
            promise.resolve(["success": true])
        }
        
        AsyncFunction("getExcludeApps") { (promise: Promise) in
            promise.resolve(appBlockerManager.getExcludeApps())
        }
        
        // Overlay config
        AsyncFunction("updateOverlayConfig") { (config: [String: Any?], promise: Promise) in
            let overlayConfig = OverlayConfig(
                title: config["title"] as? String ?? "App Blocked",
                message: config["message"] as? String,
                // ... map all other config options
            )
            appBlockerManager.updateOverlayConfig(overlayConfig)
            promise.resolve(["success": true])
        }
        
        AsyncFunction("getOverlayConfig") { (promise: Promise) in
            let config = appBlockerManager.getOverlayConfig()
            promise.resolve(config.toDictionary())
        }
    }
    
    private func formatUsageTime(_ milliseconds: Int) -> String {
        let seconds = milliseconds / 1000
        let minutes = seconds / 60
        let hours = minutes / 60
        
        if hours > 0 {
            return "\(hours)h \(minutes % 60)m"
        } else if minutes > 0 {
            return "\(minutes)m \(seconds % 60)s"
        } else {
            return "\(seconds)s"
        }
    }
}
```

### Step 3: Implement AppBlockerManager.swift

The iOS manager handles platform-specific logic:

```swift
import Foundation
import FamilyControls
import DeviceActivity
import ManagedSettings

class AppBlockerManager {
    static let shared = AppBlockerManager()
    
    private let stateManager = StateManager()
    private let appMonitor = AppMonitor()
    private var deviceActivityHandler: DeviceActivityHandler?
    private var authorizationStatus: AuthorizationStatus = .notDetermined
    
    private init() {}
    
    // MARK: - Blocking
    
    func block(apps: [String]?, excludeApps: [String] = []) {
        var mergedExclude = excludeApps + stateManager.getExcludeApps()
        mergedExclude.append(Bundle.main.bundleIdentifier!)
        
        let state = BlockerState(
            isBlocking: true,
            blockedApps: apps ?? [],
            blockAll: apps == nil || apps!.isEmpty,
            scheduledTime: stateManager.getState().scheduledTime,
            scheduleActivated: stateManager.getState().scheduleActivated,
            excludeApps: mergedExclude
        )
        
        stateManager.saveState(state)
        startMonitoring()
    }
    
    func clear() {
        let clearedState = BlockerState(
            isBlocking: false,
            blockedApps: [],
            blockAll: false,
            scheduledTime: nil,
            scheduleActivated: false,
            excludeApps: []
        )
        
        stateManager.saveState(clearedState)
        stopMonitoring()
    }
    
    func schedule(time: String, excludeApps: [String] = []) -> Bool {
        guard validateTimeFormat(time) else { return false }
        
        let scheduleActivated = isScheduleTimeReached(time)
        
        var mergedExclude = excludeApps + stateManager.getExcludeApps()
        mergedExclude.append(Bundle.main.bundleIdentifier!)
        
        let currentState = stateManager.getState()
        
        let newState = BlockerState(
            isBlocking: currentState.isBlocking || scheduleActivated,
            blockedApps: currentState.blockedApps,
            blockAll: currentState.blockAll,
            scheduledTime: time,
            scheduleActivated: scheduleActivated,
            excludeApps: mergedExclude
        )
        
        stateManager.saveState(newState)
        
        if scheduleActivated {
            startMonitoring()
        }
        
        return true
    }
    
    // MARK: - Permissions
    
    func hasScreenTimePermission() -> Bool {
        return authorizationStatus == .approved
    }
    
    func requestScreenTimeAuthorization() {
        AuthorizationCenter.shared.requestAuthorization { status in
            self.authorizationStatus = status
        }
    }
    
    // MARK: - State
    
    func getState() -> BlockerState {
        return stateManager.getState()
    }
    
    func isBlocking() -> Bool {
        let state = getState()
        return state.isBlocking || state.scheduleActivated
    }
    
    // MARK: - App Info
    
    func getInstalledApps() -> [String] {
        return appMonitor.getInstalledApps()
    }
    
    func getAppName(bundleId: String) -> String {
        return appMonitor.getAppName(bundleId: bundleId)
    }
    
    func getAppIconBase64(bundleId: String) -> String? {
        return appMonitor.getAppIconBase64(bundleId: bundleId)
    }
    
    // MARK: - Usage Stats
    
    func getUsageStats() -> [AppUsageStat] {
        return appMonitor.getTodayUsageStats()
    }
    
    func getUsageTimeForBundle(bundleId: String) -> Int {
        return appMonitor.getUsageTimeForBundle(bundleId: bundleId)
    }
    
    // MARK: - Private
    
    private func startMonitoring() {
        deviceActivityHandler = DeviceActivityHandler()
        deviceActivityHandler?.startMonitoring()
    }
    
    private func stopMonitoring() {
        deviceActivityHandler?.stopMonitoring()
    }
    
    private func validateTimeFormat(_ time: String) -> Bool {
        let regex = try! NSRegularExpression(pattern: "^\\d{2}:\\d{2}$")
        return regex.firstMatch(in: time, range: NSRange(time.startIndex..., in: time)) != nil
    }
    
    private func isScheduleTimeReached(_ time: String) -> Bool {
        let components = time.split(separator: ":")
        guard components.count == 2,
              let hour = Int(components[0]),
              let minute = Int(components[1]) else {
            return false
        }
        
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        let currentMinute = calendar.component(.minute, from: now)
        
        return currentHour > hour || (currentHour == hour && currentMinute >= minute)
    }
}
```

### Step 4: Implement AppMonitor.swift (iOS Usage Tracking)

iOS uses a different approach for usage tracking - the DeviceActivity framework:

```swift
import Foundation
import DeviceActivity
import FamilyControls

class AppMonitor {
    
    func hasScreenTimePermission() -> Bool {
        return AuthorizationCenter.shared.authorizationStatus == .approved
    }
    
    func getInstalledApps() -> [String] {
        // Get apps from FamilyActivityPicker authorization
        let center = AuthorizationCenter.shared
        guard center.authorizationStatus == .approved else {
            return []
        }
        
        var apps: [String] = []
        
        // This requires user to select apps in FamilyActivityPicker
        // Store selections and return their bundle identifiers
        // Implementation depends on how you store selections
        
        return apps
    }
    
    func getAppName(bundleId: String) -> String {
        if let name = Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String {
            return name
        }
        
        // Try to get from installed apps
        let workspace = NSWorkspace.shared
        if let app = workspace.urlForApplication(withBundleIdentifier: bundleId) {
            return FileManager.default.displayName(atPath: app.path)
        }
        
        return bundleId
    }
    
    func getAppIconBase64(bundleId: String) -> String? {
        let workspace = NSWorkspace.shared
        guard let appURL = workspace.urlForApplication(withBundleIdentifier: bundleId),
              let icon = workspace.icon(forFile: appURL.path) as NSImage? else {
            return nil
        }
        
        // Convert to PNG data and then base64
        guard let tiffData = icon.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiffData),
              let pngData = bitmap.representation(using: .png, properties: [:]) else {
            return nil
        }
        
        return pngData.base64EncodedString()
    }
    
    func getTodayUsageStats() -> [AppUsageStat] {
        // iOS doesn't provide direct usage stats API to third parties
        // You would need to use:
        // 1. DeviceActivityReport (requires Family Controls)
        // 2. Or track usage within your own app
        // This is limited on iOS compared to Android
        
        return []
    }
    
    func getUsageTimeForBundle(bundleId: String) -> Int {
        // Not directly available without Family Controls
        return 0
    }
}
```

### Step 5: Implement DeviceActivityHandler.swift

The DeviceActivity framework requires an extension:

```swift
import Foundation
import DeviceActivity
import ManagedSettings
import FamilyControls

class DeviceActivityHandler {
    private let center = AuthorizationCenter.shared
    private var activityMonitor: DeviceActivityMonitor?
    
    func startMonitoring() {
        guard center.authorizationStatus == .approved else { return }
        
        let schedule = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: 0, minute: 0),
            intervalEnd: DateComponents(hour: 23, minute: 59)
        )
        
        do {
            try DeviceActivityCenter().startMonitoring(
                during: schedule
            )
        } catch {
            print("Failed to start monitoring: \(error)")
        }
    }
    
    func stopMonitoring() {
        DeviceActivityCenter().stopMonitoring()
    }
}
```

### Step 6: Implement StateManager.swift (Persistence)

Use UserDefaults for state persistence (similar to Android's SharedPreferences):

```swift
import Foundation

class StateManager {
    private let defaults = UserDefaults.standard
    
    private enum Keys {
        static let blockerState = "blockerState"
        static let overlayConfig = "overlayConfig"
        static let excludedApps = "excludedApps"
    }
    
    func saveState(_ state: BlockerState) {
        let data: [String: Any] = [
            "isBlocking": state.isBlocking,
            "blockedApps": state.blockedApps,
            "blockAll": state.blockAll,
            "scheduledTime": state.scheduledTime ?? NSNull(),
            "scheduleActivated": state.scheduleActivated,
            "excludeApps": state.excludeApps
        ]
        
        defaults.set(data, forKey: Keys.blockerState)
    }
    
    func getState() -> BlockerState {
        guard let data = defaults.dictionary(forKey: Keys.blockerState) else {
            return BlockerState()
        }
        
        return BlockerState(
            isBlocking: data["isBlocking"] as? Bool ?? false,
            blockedApps: data["blockedApps"] as? [String] ?? [],
            blockAll: data["blockAll"] as? Bool ?? false,
            scheduledTime: data["scheduledTime"] as? String,
            scheduleActivated: data["scheduleActivated"] as? Bool ?? false,
            excludeApps: data["excludeApps"] as? [String] ?? []
        )
    }
    
    func saveOverlayConfig(_ config: OverlayConfig) {
        defaults.set(config.toDictionary(), forKey: Keys.overlayConfig)
    }
    
    func getOverlayConfig() -> OverlayConfig {
        guard let data = defaults.dictionary(forKey: Keys.overlayConfig) else {
            return OverlayConfig()
        }
        return OverlayConfig(from: data)
    }
}
```

### Step 7: Create Data Models

```swift
import Foundation

struct BlockerState {
    var isBlocking: Bool = false
    var blockedApps: [String] = []
    var blockAll: Bool = false
    var scheduledTime: String? = nil
    var scheduleActivated: Bool = false
    var excludeApps: [String] = []
}

struct OverlayConfig {
    var title: String = "App Blocked"
    var message: String? = nil
    var description: String? = nil
    var backgroundColor: String = "#1A1A1A"
    var textColor: String = "#FFFFFF"
    var titleTextSize: Float = 32
    var messageTextSize: Float = 18
    var descriptionTextSize: Float = 16
    var showAppIcon: Bool = true
    var showAppName: Bool = true
    var showUsageStats: Bool = false
    var showTodayUsage: Bool = false
    var blockerAppName: String? = nil
    var buttonText: String? = nil
    var buttonLink: String? = nil
    var buttonColor: String = "#4CAF50"
    var buttonTextColor: String = "#FFFFFF"
    var buttonBorderRadius: Float = 50
    var buttonWidth: Float = 280
    var buttonHeight: Float = 60
    var buttonMarginTop: Float = 40
    var showCloseButton: Bool = false
    var closeButtonColor: String = "#666666"
    
    func toDictionary() -> [String: Any] {
        var dict: [String: Any] = [
            "title": title,
            "backgroundColor": backgroundColor,
            "textColor": textColor,
            "titleTextSize": titleTextSize,
            "messageTextSize": messageTextSize,
            "descriptionTextSize": descriptionTextSize,
            "showAppIcon": showAppIcon,
            "showAppName": showAppName,
            "showUsageStats": showUsageStats,
            "showTodayUsage": showTodayUsage,
            "buttonColor": buttonColor,
            "buttonTextColor": buttonTextColor,
            "buttonBorderRadius": buttonBorderRadius,
            "buttonWidth": buttonWidth,
            "buttonHeight": buttonHeight,
            "buttonMarginTop": buttonMarginTop,
            "showCloseButton": showCloseButton,
            "closeButtonColor": closeButtonColor
        ]
        
        if let message = message { dict["message"] = message }
        if let description = description { dict["description"] = description }
        if let blockerAppName = blockerAppName { dict["blockerAppName"] = blockerAppName }
        if let buttonText = buttonText { dict["buttonText"] = buttonText }
        if let buttonLink = buttonLink { dict["buttonLink"] = buttonLink }
        
        return dict
    }
    
    init(from dict: [String: Any]) {
        self.title = dict["title"] as? String ?? "App Blocked"
        self.message = dict["message"] as? String
        self.description = dict["description"] as? String
        self.backgroundColor = dict["backgroundColor"] as? String ?? "#1A1A1A"
        self.textColor = dict["textColor"] as? String ?? "#FFFFFF"
        self.titleTextSize = dict["titleTextSize"] as? Float ?? 32
        self.messageTextSize = dict["messageTextSize"] as? Float ?? 18
        self.descriptionTextSize = dict["descriptionTextSize"] as? Float ?? 16
        self.showAppIcon = dict["showAppIcon"] as? Bool ?? true
        self.showAppName = dict["showAppName"] as? Bool ?? true
        self.showUsageStats = dict["showUsageStats"] as? Bool ?? false
        self.showTodayUsage = dict["showTodayUsage"] as? Bool ?? false
        self.blockerAppName = dict["blockerAppName"] as? String
        self.buttonText = dict["buttonText"] as? String
        self.buttonLink = dict["buttonLink"] as? String
        self.buttonColor = dict["buttonColor"] as? String ?? "#4CAF50"
        self.buttonTextColor = dict["buttonTextColor"] as? String ?? "#FFFFFF"
        self.buttonBorderRadius = dict["buttonBorderRadius"] as? Float ?? 50
        self.buttonWidth = dict["buttonWidth"] as? Float ?? 280
        self.buttonHeight = dict["buttonHeight"] as? Float ?? 60
        self.buttonMarginTop = dict["buttonMarginTop"] as? Float ?? 40
        self.showCloseButton = dict["showCloseButton"] as? Bool ?? false
        self.closeButtonColor = dict["closeButtonColor"] as? String ?? "#666666"
    }
}

struct AppUsageStat {
    var bundleId: String
    var appName: String
    var iconBase64: String?
    var usageTime: Int
    var lastTimeUsed: Date
    var usageTimeFormatted: String
}
```

### Step 8: Update iOS Entitlements

Create or update the entitlements file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.developer.family-controls</key>
    <true/>
</dict>
</plist>
```

### Step 9: Add Required Capabilities in Xcode

1. Open Xcode project
2. Select your target
3. Go to "Signing & Capabilities"
4. Add "Family Controls" capability
5. This requires Apple's approval for App Store release

### Step 10: Create Device Activity Extension

You need to create a new target for the DeviceActivityMonitor:

```
DeviceActivityMonitorExtension/
├── DeviceActivityMonitorExtension.swift
└── Info.plist
```

```swift
import DeviceActivity
import ManagedSettings

class DeviceActivityMonitorExtension extends DeviceActivityMonitor {
    
    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
    }
    
    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
    }
    
    override func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventDidReachThreshold(event, activity: activity)
    }
    
    override func intervalWillStartWarning(for activity: DeviceActivityName) {
        super.intervalWillStartWarning(for: activity)
    }
}
```

## Important iOS Limitations

### 1. No True App Blocking Without Parental Controls

- iOS doesn't allow third-party apps to block other apps
- Family Controls framework is the **only** official way
- Requires special entitlement from Apple (may be rejected for non-parental use)
- The app will be classified as a "Parental Control" app

### 2. Usage Stats Are Limited

- iOS doesn't expose detailed usage statistics to third-party apps
- Only available through Family Controls with user authorization
- Even then, it's limited compared to Android's UsageStatsManager

### 3. No System Overlay

- Cannot display overlay on top of other apps
- Must use in-app blocking (show blocking UI when user returns to your app)
- Or use push notifications (limited)

### 4. App Store Review

- Family Controls entitlement is scrutinized by Apple
- Must provide valid reason for the entitlement
- May not be approved for non-parental supervision use cases

## Alternative Approaches

### Option A: Family Controls (Recommended for True Blocking)

- Full app blocking capability
- Requires special entitlement
- Best for parental control apps
- Strict App Store review

### Option B: In-App Focus Mode

- Don't block system-wide
- Track usage within your own app
- Show reminders/focus prompts
- No special entitlements needed
- More lenient App Store review

### Option C: App Intents + Focus Filters (iOS 16+)

- Use App Intents framework
- Focus mode integration
- Cannot block but can limit
- Less restrictive than Family Controls

## Testing

### Local Testing

1. Build the iOS project:
   ```bash
   cd ios && xcodebuild -workspace YourApp.xcworkspace -scheme YourApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
   ```

2. Run on simulator or device

### Entitlements Testing

- Family Controls requires physical device
- Simulator doesn't support all features
- Test on real device with proper provisioning

## Files to Create/Modify Summary

| File | Action | Description |
|------|--------|-------------|
| `ios/ExpoBlockerModule.swift` | Modify | Implement all async functions |
| `ios/AppBlocker/AppBlockerManager.swift` | Create | Core business logic |
| `ios/AppBlocker/AppMonitor.swift` | Create | App info and usage tracking |
| `ios/AppBlocker/DeviceActivityHandler.swift` | Create | Screen Time integration |
| `ios/AppBlocker/StateManager.swift` | Create | UserDefaults persistence |
| `ios/AppBlocker/Models.swift` | Create | Data models |
| `ios/YourApp.entitlements` | Modify | Add Family Controls |
| `DeviceActivityMonitorExtension/` | Create | Background monitoring |

## Key Takeaways

1. **Same API, Different Implementation**: Keep the TypeScript/JavaScript API identical across platforms
2. **iOS is More Restricted**: Cannot do true app blocking without Apple's Family Controls
3. **Family Controls Requires Approval**: Contact Apple for the entitlement
4. **No Overlay on iOS**: Must show blocking UI in-app or use notifications
5. **Usage Stats Limited**: iOS doesn't expose detailed usage data like Android
6. **Consider Alternative**: If Family Controls isn't viable, implement an in-app focus/usage tracking solution instead of true blocking
