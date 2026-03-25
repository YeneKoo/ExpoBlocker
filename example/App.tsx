import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  FlatList,
  Image,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import AppBlocker, { 
  BlockerState, 
  PermissionStatus, 
  OverlayConfig,
  AppUsageStat,
  useButtonClickListener
} from 'expo-blocker';

const DEFAULT_OVERLAY_CONFIG: OverlayConfig = {
  showAppIcon: true,
  showAppName: true,
  showTodayUsage: true,
};

export default function App() {
  const [state, setState] = useState<BlockerState | null>(null);
  const [permissions, setPermissions] = useState<PermissionStatus | null>(null);
  const [apps, setApps] = useState<AppUsageStat[]>([]);
  const [selectedApps, setSelectedApps] = useState<string[]>([]);
  const [excludeApps, setExcludeApps] = useState<string[]>([]);
  const [scheduleTime, setScheduleTime] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [overlayConfig, setOverlayConfig] = useState<OverlayConfig>(DEFAULT_OVERLAY_CONFIG);
  
  // Custom overlay settings
  const [customTitle, setCustomTitle] = useState('App Blocked');
  const [customMessage, setCustomMessage] = useState('');
  const [customDescription, setCustomDescription] = useState('');
  const [customButtonText, setCustomButtonText] = useState('Open Settings');
  const [showTodayUsage, setShowTodayUsage] = useState(false);
  // Button customization
  const [buttonBorderRadius, setButtonBorderRadius] = useState('50');
  const [buttonWidth, setButtonWidth] = useState('280');
  const [buttonHeight, setButtonHeight] = useState('60');
  const [buttonMarginTop, setButtonMarginTop] = useState('40');
  const [buttonColor, setButtonColor] = useState('#4CAF50');

  // Listen for button clicks from overlay
  useButtonClickListener((event) => {
    Alert.alert('Button Clicked', `Package: ${event.packageName}\nAction: ${event.action}`);
  });

  useEffect(() => {
    initializeBlocker();
  }, []);

  const initializeBlocker = async () => {
    try {
      const perms = await AppBlocker.checkPermissions();
      setPermissions(perms);

      const currentState = await AppBlocker.getState();
      setState(currentState);
      setExcludeApps(currentState.excludeApps || []);

      const savedConfig = await AppBlocker.getOverlayConfig();
      setOverlayConfig(savedConfig);
      setCustomTitle(savedConfig.title || 'App Blocked');
      setCustomMessage(savedConfig.message || '');
      setCustomDescription(savedConfig.description || '');
      setCustomButtonText(savedConfig.buttonText || '');
      setShowTodayUsage(savedConfig.showTodayUsage ?? true);

      if (perms.usageStats) {
        const usageStats = await AppBlocker.getUsageStats();
        setApps(usageStats);
      }
    } catch (error) {
      console.error('Initialization error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateOverlayConfig = async () => {
    try {
      const config: OverlayConfig = {
        title: customTitle,
        message: customMessage || undefined,
        description: customDescription || undefined,
        buttonText: customButtonText || undefined,
        showAppIcon: overlayConfig.showAppIcon,
        showAppName: overlayConfig.showAppName,
        showTodayUsage: showTodayUsage,
        showUsageStats: overlayConfig.showUsageStats,
        buttonBorderRadius: parseInt(buttonBorderRadius) || 50,
        buttonWidth: parseInt(buttonWidth) || 280,
        buttonHeight: parseInt(buttonHeight) || 60,
        buttonMarginTop: parseInt(buttonMarginTop) || 40,
        buttonColor: parseInt(buttonColor.replace('#', '0xFF')) || 0xFF4CAF50,
      };
      
      await AppBlocker.updateOverlayConfig(config);
      setOverlayConfig(config);
      Alert.alert('Success', 'Overlay config updated');
    } catch (error) {
      Alert.alert('Error', 'Failed to update overlay config');
    }
  };

  const handleBlock = async () => {
    try {
      const appsToBlock = selectedApps.length > 0 ? selectedApps : null;
      await AppBlocker.block(appsToBlock, excludeApps);
      
      const newState = await AppBlocker.getState();
      setState(newState);
      
      Alert.alert('Success', selectedApps.length > 0 
        ? `${selectedApps.length} apps blocked` 
        : 'All non-system apps blocked');
    } catch (error) {
      Alert.alert('Error', 'Failed to block apps');
    }
  };

  const handleClear = async () => {
    try {
      await AppBlocker.clear();
      
      const newState = await AppBlocker.getState();
      setState(newState);
      setSelectedApps([]);
      
      Alert.alert('Success', 'Blocking cleared');
    } catch (error) {
      Alert.alert('Error', 'Failed to clear blocking');
    }
  };

  const handleSchedule = async () => {
    if (!scheduleTime || !/^\d{2}:\d{2}$/.test(scheduleTime)) {
      Alert.alert('Invalid Time', 'Please enter time in HH:mm format (e.g., 21:00)');
      return;
    }

    try {
      await AppBlocker.schedule(scheduleTime, excludeApps);
      
      const newState = await AppBlocker.getState();
      setState(newState);
      
      Alert.alert('Success', `Blocking scheduled for ${scheduleTime}`);
    } catch (error) {
      Alert.alert('Error', 'Failed to schedule blocking');
    }
  };

  const toggleAppSelection = (packageName: string) => {
    setSelectedApps(prev => 
      prev.includes(packageName)
        ? prev.filter(app => app !== packageName)
        : [...prev, packageName]
    );
  };

  const toggleExcludeSelection = (packageName: string) => {
    setExcludeApps(prev => 
      prev.includes(packageName)
        ? prev.filter(app => app !== packageName)
        : [...prev, packageName]
    );
  };

  const renderAppIcon = (iconBase64: string | null, size: number = 40) => {
    if (!iconBase64) {
      return <View style={[styles.iconPlaceholder, { width: size, height: size }]} />;
    }
    return (
      <Image 
        source={{ uri: `data:image/png;base64,${iconBase64}` }}
        style={{ width: size, height: size }}
        resizeMode="contain"
      />
    );
  };

  if (isLoading) {
    return (
      <View style={styles.container}>
        <Text>Loading...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>App Blocker 🔒</Text>
      
      {/* Permissions */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Permissions</Text>
        <Text>Usage Stats: {permissions?.usageStats ? '✓' : '✗'}</Text>
        <Text>Overlay: {permissions?.overlay ? '✓' : '✗'}</Text>
        {(!permissions?.usageStats || !permissions?.overlay) ? (
          <Button title="Request Permissions" onPress={async () => {
            if (permissions && !permissions.usageStats) {
              await AppBlocker.requestUsageStatsPermission();
            }
            if (permissions && !permissions.overlay) {
              await AppBlocker.requestOverlayPermission();
            }
          }} />
        ) : null}
      </View>

      {/* Current State */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Current State</Text>
        <Text>Blocking: {state?.isBlocking ? 'Active' : 'Inactive'}</Text>
        <Text>Excluded Apps: {excludeApps.length}</Text>
        <Text>Scheduled Time: {state?.scheduledTime || 'None'}</Text>
      </View>

      {/* Usage Stats */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Today's App Usage</Text>
        <FlatList
          data={apps.slice(0, 10)}
          keyExtractor={(item) => item.packageName}
          scrollEnabled={false}
          renderItem={({ item }) => (
            <View style={styles.appItem}>
              {renderAppIcon(item.iconBase64, 40)}
              <View style={styles.appInfo}>
                <Text style={styles.appName} numberOfLines={1}>{item.appName}</Text>
                <Text style={styles.usageTime}>{item.usageTimeFormatted}</Text>
              </View>
            </View>
          )}
        />
      </View>

      {/* Block Controls */}
      <View style={styles.section}>
        <Button title="Block All Non-System Apps" onPress={handleBlock} />
        <View style={styles.spacer} />
        <Button title="Clear Blocking" onPress={handleClear} />
      </View>

      {/* Schedule */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Schedule</Text>
        <TextInput
          style={styles.input}
          placeholder="HH:mm (e.g., 21:00)"
          value={scheduleTime}
          onChangeText={setScheduleTime}
          keyboardType="numbers-and-punctuation"
        />
        <Button title="Schedule" onPress={handleSchedule} />
      </View>

      {/* Exclude Apps */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Exclude Apps ({excludeApps.length})</Text>
        <Text style={styles.hint}>These apps will never be blocked</Text>
        <FlatList
          data={apps.slice(0, 15)}
          keyExtractor={(item) => item.packageName}
          scrollEnabled={false}
          renderItem={({ item }) => (
            <TouchableOpacity 
              style={styles.appItem}
              onPress={() => toggleExcludeSelection(item.packageName)}
            >
              {renderAppIcon(item.iconBase64, 40)}
              <View style={styles.appInfo}>
                <Text style={styles.appName} numberOfLines={1}>{item.appName}</Text>
              </View>
              <View style={[styles.checkbox, excludeApps.includes(item.packageName) && styles.checkboxChecked]}>
                {excludeApps.includes(item.packageName) && <Text style={styles.checkmark}>✓</Text>}
              </View>
            </TouchableOpacity>
          )}
        />
      </View>

      {/* Select Apps to Block */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Select Apps to Block ({selectedApps.length})</Text>
        <FlatList
          data={apps.slice(0, 15)}
          keyExtractor={(item) => item.packageName}
          scrollEnabled={false}
          renderItem={({ item }) => (
            <TouchableOpacity 
              style={styles.appItem}
              onPress={() => toggleAppSelection(item.packageName)}
            >
              {renderAppIcon(item.iconBase64, 40)}
              <View style={styles.appInfo}>
                <Text style={styles.appName} numberOfLines={1}>{item.appName}</Text>
              </View>
              <View style={[styles.checkbox, selectedApps.includes(item.packageName) && styles.checkboxChecked]}>
                {selectedApps.includes(item.packageName) && <Text style={styles.checkmark}>✓</Text>}
              </View>
            </TouchableOpacity>
          )}
        />
        {selectedApps.length > 0 && (
          <Button title={`Block Selected (${selectedApps.length})`} onPress={handleBlock} />
        )}
      </View>

      {/* Overlay Customization */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Overlay Customization</Text>
        
        <Text style={styles.label}>Title (e.g., "App Blocked")</Text>
        <TextInput
          style={styles.input}
          placeholder="Title"
          value={customTitle}
          onChangeText={setCustomTitle}
        />
        
        <Text style={styles.label}>Message (optional)</Text>
        <TextInput
          style={styles.input}
          placeholder="Short message"
          value={customMessage}
          onChangeText={setCustomMessage}
        />
        
        <Text style={styles.label}>Description (optional)</Text>
        <TextInput
          style={styles.input}
          placeholder="Longer description"
          value={customDescription}
          onChangeText={setCustomDescription}
        />
        
        <Text style={styles.label}>Button Text (optional)</Text>
        <TextInput
          style={styles.input}
          placeholder="e.g., Open Settings"
          value={customButtonText}
          onChangeText={setCustomButtonText}
        />
        
        <View style={styles.switchRow}>
          <Text>Show App Icon</Text>
          <Switch
            value={overlayConfig.showAppIcon ?? true}
            onValueChange={(v) => setOverlayConfig({ ...overlayConfig, showAppIcon: v })}
          />
        </View>
        
        <View style={styles.switchRow}>
          <Text>Show App Name</Text>
          <Switch
            value={overlayConfig.showAppName ?? true}
            onValueChange={(v) => setOverlayConfig({ ...overlayConfig, showAppName: v })}
          />
        </View>
        
        <View style={styles.switchRow}>
          <Text>Show Today's Usage</Text>
          <Switch
            value={showTodayUsage}
            onValueChange={setShowTodayUsage}
          />
        </View>
        
        <Text style={[styles.label, { marginTop: 16 }]}>Button Customization</Text>
        
        <Text style={styles.label}>Button Color (hex)</Text>
        <TextInput
          style={styles.input}
          placeholder="#4CAF50"
          value={buttonColor}
          onChangeText={setButtonColor}
        />
        
        <View style={styles.rowInput}>
          <View style={styles.halfInput}>
            <Text style={styles.labelSmall}>Border Radius</Text>
            <TextInput
              style={styles.input}
              placeholder="50"
              value={buttonBorderRadius}
              onChangeText={setButtonBorderRadius}
              keyboardType="numeric"
            />
          </View>
          <View style={styles.halfInput}>
            <Text style={styles.labelSmall}>Width</Text>
            <TextInput
              style={styles.input}
              placeholder="280"
              value={buttonWidth}
              onChangeText={setButtonWidth}
              keyboardType="numeric"
            />
          </View>
        </View>
        
        <View style={styles.rowInput}>
          <View style={styles.halfInput}>
            <Text style={styles.labelSmall}>Height</Text>
            <TextInput
              style={styles.input}
              placeholder="60"
              value={buttonHeight}
              onChangeText={setButtonHeight}
              keyboardType="numeric"
            />
          </View>
          <View style={styles.halfInput}>
            <Text style={styles.labelSmall}>Margin Top</Text>
            <TextInput
              style={styles.input}
              placeholder="40"
              value={buttonMarginTop}
              onChangeText={setButtonMarginTop}
              keyboardType="numeric"
            />
          </View>
        </View>
        
        <Button title="Apply Overlay Settings" onPress={handleUpdateOverlayConfig} />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
  },
  section: {
    marginBottom: 20,
    padding: 12,
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
  },
  hint: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
    marginTop: 8,
  },
  spacer: { height: 8 },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    padding: 10,
    marginBottom: 8,
    fontSize: 16,
  },
  appItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  iconPlaceholder: {
    backgroundColor: '#ddd',
    borderRadius: 8,
  },
  appInfo: { flex: 1, marginLeft: 12 },
  appName: { fontSize: 14, fontWeight: '500' },
  usageTime: { fontSize: 12, color: '#666', marginTop: 2 },
  checkbox: {
    width: 24,
    height: 24,
    borderWidth: 2,
    borderColor: '#ccc',
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxChecked: { backgroundColor: '#4CAF50', borderColor: '#4CAF50' },
  checkmark: { color: '#fff', fontWeight: 'bold' },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  rowInput: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  halfInput: {
    flex: 1,
    marginHorizontal: 4,
  },
  labelSmall: {
    fontSize: 12,
    color: '#666',
    marginBottom: 2,
  },
});
