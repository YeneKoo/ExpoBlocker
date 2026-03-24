import { registerWebModule, NativeModule } from 'expo';

import { ExpoBlockerModuleEvents } from './ExpoBlocker.types';

class ExpoBlockerModule extends NativeModule<ExpoBlockerModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoBlockerModule, 'ExpoBlockerModule');
