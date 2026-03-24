import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoBlockerViewProps } from './ExpoBlocker.types';

const NativeView: React.ComponentType<ExpoBlockerViewProps> =
  requireNativeView('ExpoBlocker');

export default function ExpoBlockerView(props: ExpoBlockerViewProps) {
  return <NativeView {...props} />;
}
