import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  FlatList,
  Image,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import AppBlocker, { 
  BlockerState, 
  PermissionStatus, 
  OverlayConfig,
  AppUsageStat 
} from 'expo-blocker';

const DEFAULT_OVERLAY_CONFIG: OverlayConfig = {
  title: 'App Blocked 🔒',
  message: 'Time to focus on something else!',
  backgroundColor: 0xFF1A1A1A,
  textColor: 0xFFFFFFFF,
  titleTextSize: 32,
  messageTextSize: 18,
  showAppIcon: true,
  showAppName: true,
  showUsageStats: true,
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
  const [showSettings, setShowSettings] = useState(false);
  const [showUsage, setShowUsage] = useState(false);

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

  const requestPermissions = async () => {
    try {
      if (permissions && !permissions.usageStats) {
        await AppBlocker.requestUsageStatsPermission();
      }
      if (permissions && !permissions.overlay) {
        await AppBlocker.requestOverlayPermission();
      }
      
      setTimeout(initializeBlocker, 1000);
    } catch (error) {
      console.error('Permission request error:', error);
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

  const handleUpdateOverlayConfig = async () => {
    try {
      await AppBlocker.updateOverlayConfig(overlayConfig);
      Alert.alert('Success', 'Overlay config updated');
    } catch (error) {
      Alert.alert('Error', 'Failed to update overlay config');
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

  const renderAppIcon = (iconBase64: string | null, size: number = 48) => {
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
          <Button title="Request Permissions" onPress={requestPermissions} />
        ) : null}
      </View>

      {/* Current State */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Current State</Text>
        <Text>Blocking: {state?.isBlocking ? 'Active' : 'Inactive'}</Text>
        <Text>Block All: {state?.blockAll ? 'Yes' : 'No'}</Text>
        <Text>Blocked Apps: {state?.blockedApps?.length || 0}</Text>
        <Text>Excluded Apps: {excludeApps.length}</Text>
        <Text>Scheduled Time: {state?.scheduledTime || 'None'}</Text>
      </View>

      {/* Usage Stats */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Today's App Usage</Text>
          <Button 
            title={showUsage ? 'Hide' : 'Show'} 
            onPress={() => setShowUsage(!showUsage)}
          />
        </View>
        
        {showUsage && apps.length > 0 ? (
          <FlatList
            data={apps.slice(0, 10)}
            keyExtractor={(item) => item.packageName}
            scrollEnabled={false}
            renderItem={({ item }) => (
              <View style={styles.appItem}>
                {renderAppIcon(item.iconBase64, 40)}
                <View style={styles.appInfo}>
                  <Text style={styles.appName} numberOfLines={1}>
                    {item.appName}
                  </Text>
                  <Text style={styles.usageTime}>{item.usageTimeFormatted}</Text>
                </View>
              </View>
            )}
          />
        ) : showUsage ? (
          <Text style={styles.emptyText}>No usage data available</Text>
        ) : null}
      </View>

      {/* Block Controls */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Block Controls</Text>
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
        <Text style={styles.sectionTitle}>Apps to Exclude ({excludeApps.length})</Text>
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
                <Text style={styles.appName} numberOfLines={1}>
                  {item.appName}
                </Text>
                <Text style={styles.usageTime}>{item.usageTimeFormatted}</Text>
              </View>
              <View style={[
                styles.checkbox,
                excludeApps.includes(item.packageName) && styles.checkboxChecked
              ]}>
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
                <Text style={styles.appName} numberOfLines={1}>
                  {item.appName}
                </Text>
                <Text style={styles.usageTime}>{item.usageTimeFormatted}</Text>
              </View>
              <View style={[
                styles.checkbox,
                selectedApps.includes(item.packageName) && styles.checkboxChecked
              ]}>
                {selectedApps.includes(item.packageName) && <Text style={styles.checkmark}>✓</Text>}
              </View>
            </TouchableOpacity>
          )}
        />
        {selectedApps.length > 0 && (
          <Button 
            title={`Block Selected (${selectedApps.length})`} 
            onPress={handleBlock} 
          />
        )}
      </View>

      {/* Overlay Settings */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Overlay Customization</Text>
          <Button 
            title={showSettings ? 'Hide' : 'Show'} 
            onPress={() => setShowSettings(!showSettings)}
          />
        </View>
        
        {showSettings && (
          <>
            <TextInput
              style={styles.input}
              placeholder="Title"
              value={overlayConfig.title}
              onChangeText={(text) => setOverlayConfig({ ...overlayConfig, title: text })}
            />
            <TextInput
              style={styles.input}
              placeholder="Message"
              value={overlayConfig.message}
              onChangeText={(text) => setOverlayConfig({ ...overlayConfig, message: text })}
            />
            <View style={styles.checkboxRow}>
              <TouchableOpacity 
                style={styles.checkboxLabel}
                onPress={() => setOverlayConfig({ ...overlayConfig, showAppIcon: !overlayConfig.showAppIcon })}
              >
                <View style={[styles.checkbox, overlayConfig.showAppIcon && styles.checkboxChecked]}>
                  {overlayConfig.showAppIcon && <Text style={styles.checkmark}>✓</Text>}
                </View>
                <Text>Show App Icon</Text>
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={styles.checkboxLabel}
                onPress={() => setOverlayConfig({ ...overlayConfig, showAppName: !overlayConfig.showAppName })}
              >
                <View style={[styles.checkbox, overlayConfig.showAppName && styles.checkboxChecked]}>
                  {overlayConfig.showAppName && <Text style={styles.checkmark}>✓</Text>}
                </View>
                <Text>Show App Name</Text>
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={styles.checkboxLabel}
                onPress={() => setOverlayConfig({ ...overlayConfig, showUsageStats: !overlayConfig.showUsageStats })}
              >
                <View style={[styles.checkbox, overlayConfig.showUsageStats && styles.checkboxChecked]}>
                  {overlayConfig.showUsageStats && <Text style={styles.checkmark}>✓</Text>}
                </View>
                <Text>Show Usage</Text>
              </TouchableOpacity>
            </View>
            <Button title="Apply Overlay Settings" onPress={handleUpdateOverlayConfig} />
          </>
        )}
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
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
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
  spacer: {
    height: 8,
  },
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
    marginRight: 12,
  },
  appInfo: {
    flex: 1,
    marginLeft: 12,
  },
  appName: {
    fontSize: 14,
    fontWeight: '500',
  },
  usageTime: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderWidth: 2,
    borderColor: '#ccc',
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxChecked: {
    backgroundColor: '#4CAF50',
    borderColor: '#4CAF50',
  },
  checkmark: {
    color: '#fff',
    fontWeight: 'bold',
  },
  checkboxRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 12,
  },
  checkboxLabel: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 16,
    marginBottom: 8,
  },
  emptyText: {
    fontStyle: 'italic',
    color: '#666',
    textAlign: 'center',
    padding: 16,
  },
});
