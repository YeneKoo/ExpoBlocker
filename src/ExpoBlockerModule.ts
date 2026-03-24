import { NativeModule, requireNativeModule } from 'expo';

import { BlockerState, ExpoBlockerModuleEvents, PermissionStatus } from './ExpoBlocker.types';

declare class ExpoBlockerModule extends NativeModule<ExpoBlockerModuleEvents> {
  block(apps: string[] | null): Promise<{ success: boolean }>;
  clear(): Promise<{ success: boolean }>;
  schedule(time: string): Promise<{ success: boolean }>;
  getState(): Promise<BlockerState>;
  isBlocking(): Promise<boolean>;
  hasUsageStatsPermission(): Promise<boolean>;
  hasOverlayPermission(): Promise<boolean>;
  requestUsageStatsPermission(): Promise<{ success: boolean }>;
  requestOverlayPermission(): Promise<{ success: boolean }>;
  getInstalledApps(): Promise<string[]>;
  checkPermissions(): Promise<PermissionStatus>;
}

export default requireNativeModule<ExpoBlockerModule>('ExpoBlocker');
