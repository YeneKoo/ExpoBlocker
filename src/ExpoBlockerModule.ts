import { requireNativeModule } from 'expo-modules-core';
import { NativeModules, NativeEventEmitter } from 'react-native';

import { 
  BlockerState, 
  PermissionStatus, 
  OverlayConfig,
  AppUsageStat,
  AppUsageTime,
  ButtonClickedEvent
} from './ExpoBlocker.types';

type ExpoBlockerNativeModule = {
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
};

const NativeModule = requireNativeModule<ExpoBlockerNativeModule>('ExpoBlocker');
const emitter = new NativeEventEmitter(NativeModules.ExpoBlocker);

export default NativeModule;

export function addButtonClickListener(callback: (event: ButtonClickedEvent) => void) {
  return emitter.addListener('onButtonClicked', callback);
}
