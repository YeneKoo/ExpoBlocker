export interface BlockerState {
  isBlocking: boolean;
  blockedApps: string[];
  blockAll: boolean;
  scheduledTime: string | null;
  scheduleActivated: boolean;
  excludeApps: string[];
}

export interface PermissionStatus {
  usageStats: boolean;
  overlay: boolean;
}

export interface OverlayConfig {
  title?: string;
  message?: string;
  description?: string;
  backgroundColor?: number;
  textColor?: number;
  titleTextSize?: number;
  messageTextSize?: number;
  descriptionTextSize?: number;
  showAppIcon?: boolean;
  showAppName?: boolean;
  showUsageStats?: boolean;
  showTodayUsage?: boolean;
  blockerAppName?: string;
  buttonText?: string;
  buttonColor?: number;
  buttonTextColor?: number;
  buttonBorderRadius?: number;
  buttonWidth?: number;
  buttonHeight?: number;
  buttonMarginTop?: number;
  showCloseButton?: boolean;
  closeButtonColor?: number;
}

export interface AppUsageStat {
  packageName: string;
  appName: string;
  iconBase64: string | null;
  usageTime: number;
  lastTimeUsed: number;
  usageTimeFormatted: string;
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
