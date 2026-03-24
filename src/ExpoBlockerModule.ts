import { NativeModule, requireNativeModule } from 'expo';

import { 
  BlockerState, 
  ExpoBlockerModuleEvents, 
  PermissionStatus, 
  OverlayConfig,
  AppUsageStat,
  AppUsageTime 
} from './ExpoBlocker.types';

declare class ExpoBlockerModule extends NativeModule<ExpoBlockerModuleEvents> {
  block(apps: string[] | null): Promise<{ success: boolean }>;
  blockWithExclude(apps: string[] | null, excludeApps: string[]): Promise<{ success: boolean }>;
  clear(): Promise<{ success: boolean }>;
  schedule(time: string): Promise<{ success: boolean }>;
  scheduleWithExclude(time: string, excludeApps: string[]): Promise<{ success: boolean }>;
  getState(): Promise<BlockerState>;
  isBlocking(): Promise<boolean>;
  hasUsageStatsPermission(): Promise<boolean>;
  hasOverlayPermission(): Promise<boolean>;
  requestUsageStatsPermission(): Promise<{ success: boolean }>;
  requestOverlayPermission(): Promise<{ success: boolean }>;
  getInstalledApps(): Promise<string[]>;
  getAppName(packageName: string): Promise<string>;
  getAppIcon(packageName: string): Promise<string | null>;
  getUsageStats(): Promise<AppUsageStat[]>;
  getAppUsageTime(packageName: string): Promise<AppUsageTime>;
  checkPermissions(): Promise<PermissionStatus>;
  setExcludeApps(apps: string[]): Promise<{ success: boolean }>;
  getExcludeApps(): Promise<string[]>;
  updateOverlayConfig(config: OverlayConfig): Promise<{ success: boolean }>;
  getOverlayConfig(): Promise<OverlayConfig>;
}

export default requireNativeModule<ExpoBlockerModule>('ExpoBlocker');
