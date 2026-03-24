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
  backgroundColor?: number;
  textColor?: number;
  titleTextSize?: number;
  messageTextSize?: number;
  showAppIcon?: boolean;
  showAppName?: boolean;
  showUsageStats?: boolean;
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

export type ExpoBlockerModuleEvents = {
  onBlockStateChange?: (params: BlockerState) => void;
};
