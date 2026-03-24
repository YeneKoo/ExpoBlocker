export interface BlockerState {
  isBlocking: boolean;
  blockedApps: string[];
  blockAll: boolean;
  scheduledTime: string | null;
  scheduleActivated: boolean;
}

export interface PermissionStatus {
  usageStats: boolean;
  overlay: boolean;
}

export interface OperationResult {
  success: boolean;
}

export type ExpoBlockerModuleEvents = {
  onBlockStateChange?: (params: BlockerState) => void;
};
