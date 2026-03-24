import * as React from 'react';

import { ExpoBlockerViewProps } from './ExpoBlocker.types';

export default function ExpoBlockerView(props: ExpoBlockerViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
