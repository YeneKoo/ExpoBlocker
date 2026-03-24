import ExpoBlocker from './ExpoBlockerModule';
import type { BlockerState, PermissionStatus } from './ExpoBlocker.types';

export class AppBlocker {
  private module = ExpoBlocker;

  async block(apps?: string[]): Promise<void> {
    await this.module.block(apps ?? null);
  }

  async blockAll(): Promise<void> {
    await this.module.block(null);
  }

  async clear(): Promise<void> {
    await this.module.clear();
  }

  async schedule(time: string): Promise<void> {
    if (!/^\d{2}:\d{2}$/.test(time)) {
      throw new Error('Invalid time format. Use HH:mm (24-hour format)');
    }
    await this.module.schedule(time);
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
}

export default new AppBlocker();
export { ExpoBlocker };
export type { BlockerState, PermissionStatus } from './ExpoBlocker.types';
