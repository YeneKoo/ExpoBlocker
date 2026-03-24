import { NativeModule, requireNativeModule } from 'expo';

import { ExpoBlockerModuleEvents } from './ExpoBlocker.types';

declare class ExpoBlockerModule extends NativeModule<ExpoBlockerModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoBlockerModule>('ExpoBlocker');
