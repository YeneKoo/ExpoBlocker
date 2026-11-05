export interface BlockerState {
  isBlocking: boolean;
  blockedApps: string[];
  blockAll: boolean;
  scheduledTime: string | null;
  scheduledAtMillis: number | null;
  scheduleActivated: boolean;
  excludeApps: string[];
  pausedUntilMillis: number | null;
}

export interface PermissionStatus {
  usageStats: boolean;
  overlay: boolean;
}

export interface OverlayConfig {
  title?: string;
  message?: string;
  description?: string;
  backgroundColor?: string;
  textColor?: string;
  titleTextSize?: number;
  messageTextSize?: number;
  descriptionTextSize?: number;
  showAppIcon?: boolean;
  showAppName?: boolean;
  showUsageStats?: boolean;
  showTodayUsage?: boolean;
  blockerAppName?: string;
  buttonText?: string;
  buttonLink?: string;
  buttonColor?: string;
  buttonTextColor?: string;
  buttonBorderRadius?: number;
  buttonWidth?: number;
  buttonHeight?: number;
  buttonMarginTop?: number;
  showCloseButton?: boolean;
  closeButtonColor?: string;
  // The foreground service's notification, shown while blocking is active.
  notificationTitle?: string;
  notificationText?: string;
  notificationChannelName?: string;
}

export interface AppUsageStat {
  packageName: string;
  appName: string;
  iconBase64: string | null;
  usageTime: number;
  lastTimeUsed: number;
  usageTimeFormatted: string;
}

// Epoch milliseconds the screen turned on and off.
export interface ScreenSession {
  start: number;
  end: number;
}

export interface AppUsageTime {
  usageTime: number;
  usageTimeFormatted: string;
}

export type ButtonClickedEvent = {
  packageName: string;
  action: string;
};

export type ExpoBlockerModuleEvents = {
  onButtonClicked?: (event: ButtonClickedEvent) => void;
};
