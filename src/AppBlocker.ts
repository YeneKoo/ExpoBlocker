import ExpoBlocker from './ExpoBlockerModule';
import type { 
  BlockerState, 
  PermissionStatus, 
  OverlayConfig,
  AppUsageStat,
  AppUsageTime 
} from './ExpoBlocker.types';

export class AppBlocker {
  private module = ExpoBlocker;

  async block(apps?: string[] | null, excludeApps?: string[]): Promise<void> {
    if (excludeApps && excludeApps.length > 0) {
      await this.module.blockWithExclude(apps ?? null, excludeApps);
    } else {
      await this.module.block(apps ?? null);
    }
  }

  async blockAll(excludeApps?: string[]): Promise<void> {
    await this.block(null, excludeApps);
  }

  async clear(): Promise<void> {
    await this.module.clear();
  }

  async schedule(time: string, excludeApps?: string[]): Promise<void> {
    if (!/^\d{2}:\d{2}$/.test(time)) {
      throw new Error('Invalid time format. Use HH:mm (24-hour format)');
    }
    if (excludeApps && excludeApps.length > 0) {
      await this.module.scheduleWithExclude(time, excludeApps);
    } else {
      await this.module.schedule(time);
    }
  }

  async getState(): Promise<BlockerState> {
    return await this.module.getState();
  }

  async isBlocking(): Promise<boolean> {
    return await this.module.isBlocking();
  }

  async getInstalledApps(): Promise<string[]> {
    return await this.module.getInstalledApps();
  }

  async getAppName(packageName: string): Promise<string> {
    return await this.module.getAppName(packageName);
  }

  async getAppIcon(packageName: string): Promise<string | null> {
    return await this.module.getAppIcon(packageName);
  }

  async getUsageStats(): Promise<AppUsageStat[]> {
    return await this.module.getUsageStats();
  }

  async getAppUsageTime(packageName: string): Promise<AppUsageTime> {
    return await this.module.getAppUsageTime(packageName);
  }

  async checkPermissions(): Promise<PermissionStatus> {
    return await this.module.checkPermissions();
  }

  async hasUsageStatsPermission(): Promise<boolean> {
    return await this.module.hasUsageStatsPermission();
  }

  async hasOverlayPermission(): Promise<boolean> {
    return await this.module.hasOverlayPermission();
  }

  async requestUsageStatsPermission(): Promise<void> {
    await this.module.requestUsageStatsPermission();
  }

  async requestOverlayPermission(): Promise<void> {
    await this.module.requestOverlayPermission();
  }

  async setExcludeApps(apps: string[]): Promise<void> {
    await this.module.setExcludeApps(apps);
  }

  async getExcludeApps(): Promise<string[]> {
    return await this.module.getExcludeApps();
  }

  async updateOverlayConfig(config: OverlayConfig): Promise<void> {
    await this.module.updateOverlayConfig(config);
  }

  async getOverlayConfig(): Promise<OverlayConfig> {
    return await this.module.getOverlayConfig();
  }
}

export default new AppBlocker();
export { ExpoBlocker };
export type { 
  BlockerState, 
  PermissionStatus, 
  OverlayConfig,
  AppUsageStat,
  AppUsageTime 
} from './ExpoBlocker.types';
