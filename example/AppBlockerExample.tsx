import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  Button,
  StyleSheet,
  TextInput,
  FlatList,
  Alert,
  Image,
  ScrollView,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import AppBlocker, { BlockerState, PermissionStatus, OverlayConfig, AppUsageStat } from '../src';
import { useButtonClickListener } from '../src';

interface AppItem {
  packageName: string;
  appName: string;
  iconBase64: string | null;
}

export default function AppBlockerExample() {
  const [state, setState] = useState<BlockerState | null>(null);
  const [permissions, setPermissions] = useState<PermissionStatus | null>(null);
  const [apps, setApps] = useState<AppItem[]>([]);
  const [selectedApps, setSelectedApps] = useState<string[]>([]);
  const [excludeApps, setExcludeApps] = useState<string[]>([]);
  const [scheduleTime, setScheduleTime] = useState('');
  const [usageStats, setUsageStats] = useState<AppUsageStat[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'block' | 'usage' | 'settings'>('block');

  useButtonClickListener((event) => {
    Alert.alert('Button Clicked', `User tapped button for blocked app: ${event.packageName}`);
    initializeBlocker();
  });

  useEffect(() => {
    initializeBlocker();
  }, []);

  const initializeBlocker = async () => {
    try {
      const perms = await AppBlocker.checkPermissions();
      setPermissions(perms);

      if (!perms.usageStats || !perms.overlay) {
        return;
      }

      const currentState = await AppBlocker.getState();
      setState(currentState);

      const installedApps = await AppBlocker.getInstalledApps();
      const appDetails = await Promise.all(
        installedApps.slice(0, 30).map(async (pkg) => {
          try {
            const name = await AppBlocker.getAppName(pkg);
            const icon = await AppBlocker.getAppIcon(pkg);
            return { packageName: pkg, appName: name, iconBase64: icon };
          } catch {
            return { packageName: pkg, appName: pkg.split('.').pop() || pkg, iconBase64: null };
          }
        })
      );
      setApps(appDetails.sort((a, b) => a.appName.localeCompare(b.appName)));

      const stats = await AppBlocker.getUsageStats();
      setUsageStats(stats.slice(0, 15));

      const excluded = await AppBlocker.getExcludeApps();
      setExcludeApps(excluded);
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
      
      setTimeout(initializeBlocker, 1500);
    } catch (error) {
      console.error('Permission request error:', error);
    }
  };

  const handleBlock = async () => {
    try {
      const overlayConfig: OverlayConfig = {
        title: 'App Blocked',
        message: 'Time to focus on what matters!',
        backgroundColor: '#1A1A1A',
        textColor: '#FFFFFF',
        showAppIcon: true,
        showAppName: true,
        showTodayUsage: true,
        buttonText: 'Open App',
        buttonLink: 'expo://home',
        buttonColor: '#4CAF50',
        buttonTextColor: '#FFFFFF',
        buttonBorderRadius: 25,
        buttonWidth: 200,
        buttonHeight: 50,
      };
      
      await AppBlocker.updateOverlayConfig(overlayConfig);
      
      if (selectedApps.length > 0) {
        await AppBlocker.block(selectedApps, excludeApps);
      } else {
        await AppBlocker.blockAll(excludeApps);
      }
      
      const newState = await AppBlocker.getState();
      setState(newState);
      
      Alert.alert('Success', 'Apps are now blocked');
    } catch (error) {
      Alert.alert('Error', 'Failed to block apps');
      console.error(error);
    }
  };

  const handleClear = async () => {
    try {
      await AppBlocker.clear();
      
      const newState = await AppBlocker.getState();
      setState(newState);
      
      Alert.alert('Success', 'Blocking has been cleared');
    } catch (error) {
      Alert.alert('Error', 'Failed to clear blocking');
      console.error(error);
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
      console.error(error);
    }
  };

  const toggleAppSelection = (packageName: string) => {
    setSelectedApps(prev => 
      prev.includes(packageName)
        ? prev.filter(app => app !== packageName)
        : [...prev, packageName]
    );
  };

  const toggleExcludeApp = (packageName: string) => {
    const newExclude = excludeApps.includes(packageName)
      ? excludeApps.filter(app => app !== packageName)
      : [...excludeApps, packageName];
    setExcludeApps(newExclude);
    AppBlocker.setExcludeApps(newExclude);
  };

  const formatUsageTime = (millis: number): string => {
    const seconds = millis / 1000;
    const minutes = seconds / 60;
    const hours = minutes / 60;
    
    if (hours > 0) return `${Math.floor(hours)}h ${Math.floor(minutes % 60)}m`;
    if (minutes > 0) return `${Math.floor(minutes)}m ${Math.floor(seconds % 60)}s`;
    return `${Math.floor(seconds)}s`;
  };

  const renderAppIcon = (iconBase64: string | null, size: number = 40) => {
    if (!iconBase64) {
      return (
        <View style={[styles.defaultIcon, { width: size, height: size }]}>
          <Text style={{ fontSize: size * 0.5 }}>📱</Text>
        </View>
      );
    }
    return (
      <Image
        source={{ uri: `data:image/png;base64,${iconBase64}` }}
        style={{ width: size, height: size }}
      />
    );
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>Loading...</Text>
      </View>
    );
  }

  if (!permissions?.usageStats || !permissions?.overlay) {
    return (
      <View style={styles.permissionContainer}>
        <Text style={styles.permissionTitle}>Permissions Required</Text>
        <Text style={styles.permissionText}>
          This app needs special permissions to work:
        </Text>
        <View style={styles.permissionList}>
          <Text style={styles.permissionItem}>
            {permissions?.usageStats ? '✅' : '⬜'} Usage Access - Detect foreground apps
          </Text>
          <Text style={styles.permissionItem}>
            {permissions?.overlay ? '✅' : '⬜'} Display Over Apps - Show blocking overlay
          </Text>
        </View>
        <Button title="Grant Permissions" onPress={requestPermissions} />
      </View>
    );
  }

  const renderBlockTab = () => (
    <ScrollView style={styles.tabContent}>
      {/* Current State */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>📊 Status</Text>
        <View style={styles.statusRow}>
          <Text>Blocking: </Text>
          <Text style={[styles.statusValue, state?.isBlocking ? styles.active : styles.inactive]}>
            {state?.isBlocking ? 'Active' : 'Inactive'}
          </Text>
        </View>
        <View style={styles.statusRow}>
          <Text>Block Mode: </Text>
          <Text style={styles.statusValue}>{state?.blockAll ? 'All Apps' : 'Selected'}</Text>
        </View>
        {state?.blockedApps && state.blockedApps.length > 0 && (
          <Text style={styles.statusDetail}>Blocked: {state.blockedApps.length} apps</Text>
        )}
        {state?.scheduledTime && (
          <View style={styles.statusRow}>
            <Text>Scheduled: </Text>
            <Text style={styles.statusValue}>{state.scheduledTime}</Text>
          </View>
        )}
      </View>

      {/* Quick Actions */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>⚡ Quick Actions</Text>
        <View style={styles.buttonRow}>
          <Button title="Block All" onPress={handleBlock} />
          <View style={styles.buttonSpacer} />
          <Button title="Clear" onPress={handleClear} color="#ff4444" />
        </View>
      </View>

      {/* Schedule */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>⏰ Schedule</Text>
        <TextInput
          style={styles.input}
          placeholder="HH:mm (e.g., 21:00)"
          value={scheduleTime}
          onChangeText={setScheduleTime}
          keyboardType="numbers-and-punctuation"
        />
        <Button title="Set Schedule" onPress={handleSchedule} />
      </View>

      {/* Exclude Apps */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🚫 Excluded Apps (Whitelist)</Text>
        <Text style={styles.helperText}>These apps will never be blocked</Text>
        <FlatList
          data={apps.slice(0, 15)}
          keyExtractor={(item) => item.packageName}
          scrollEnabled={false}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.appItem}
              onPress={() => toggleExcludeApp(item.packageName)}
            >
              {renderAppIcon(item.iconBase64, 32)}
              <Text style={styles.appName} numberOfLines={1}>{item.appName}</Text>
              <Text style={[styles.checkbox, excludeApps.includes(item.packageName) && styles.checked]}>
                {excludeApps.includes(item.packageName) ? '✓' : '○'}
              </Text>
            </TouchableOpacity>
          )}
        />
      </View>

      {/* Select Apps to Block */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🎯 Select Apps to Block</Text>
        {selectedApps.length > 0 && (
          <Text style={styles.selectedCount}>{selectedApps.length} apps selected</Text>
        )}
        <FlatList
          data={apps}
          keyExtractor={(item) => item.packageName}
          scrollEnabled={false}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.appItem}
              onPress={() => toggleAppSelection(item.packageName)}
            >
              {renderAppIcon(item.iconBase64, 32)}
              <Text style={styles.appName} numberOfLines={1}>{item.appName}</Text>
              <Text style={[styles.checkbox, selectedApps.includes(item.packageName) && styles.checked]}>
                {selectedApps.includes(item.packageName) ? '✓' : '○'}
              </Text>
            </TouchableOpacity>
          )}
        />
        {selectedApps.length > 0 && (
          <Button title={`Block ${selectedApps.length} Apps`} onPress={handleBlock} />
        )}
      </View>
    </ScrollView>
  );

  const renderUsageTab = () => (
    <ScrollView style={styles.tabContent}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>📱 Today's App Usage</Text>
        {usageStats.length === 0 ? (
          <Text style={styles.helperText}>No usage data yet. Usage stats appear after you use apps.</Text>
        ) : (
          usageStats.map((stat) => (
            <View key={stat.packageName} style={styles.usageItem}>
              {renderAppIcon(stat.iconBase64, 40)}
              <View style={styles.usageInfo}>
                <Text style={styles.appName}>{stat.appName}</Text>
                <Text style={styles.usageTime}>{stat.usageTimeFormatted}</Text>
              </View>
            </View>
          ))
        )}
      </View>
    </ScrollView>
  );

  const renderSettingsTab = () => (
    <ScrollView style={styles.tabContent}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🎨 Overlay Preview</Text>
        <Text style={styles.helperText}>Configure how the blocking overlay looks</Text>
        <Button
          title="Test Overlay"
          onPress={async () => {
            await AppBlocker.updateOverlayConfig({
              title: 'App Blocked',
              message: 'Take a break!',
              buttonText: 'Open App',
              buttonLink: 'expo://home',
            });
            await AppBlocker.block(['com.android.settings']);
          }}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🔔 Permissions Status</Text>
        <Text style={styles.permissionItem}>Usage Stats: {permissions?.usageStats ? '✅ Granted' : '❌ Not Granted'}</Text>
        <Text style={styles.permissionItem}>Overlay: {permissions?.overlay ? '✅ Granted' : '❌ Not Granted'}</Text>
        {!permissions?.usageStats && (
          <Button title="Request Usage Permission" onPress={() => AppBlocker.requestUsageStatsPermission()} />
        )}
        {!permissions?.overlay && (
          <Button title="Request Overlay Permission" onPress={() => AppBlocker.requestOverlayPermission()} />
        )}
      </View>
    </ScrollView>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <Text style={styles.title}>🛡️ App Blocker</Text>
      
      {/* Tab Navigation */}
      <View style={styles.tabBar}>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'block' && styles.activeTab]}
          onPress={() => setActiveTab('block')}
        >
          <Text style={[styles.tabText, activeTab === 'block' && styles.activeTabText]}>Block</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'usage' && styles.activeTab]}
          onPress={() => setActiveTab('usage')}
        >
          <Text style={[styles.tabText, activeTab === 'usage' && styles.activeTabText]}>Usage</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'settings' && styles.activeTab]}
          onPress={() => setActiveTab('settings')}
        >
          <Text style={[styles.tabText, activeTab === 'settings' && styles.activeTabText]}>Settings</Text>
        </TouchableOpacity>
      </View>

      {activeTab === 'block' && renderBlockTab()}
      {activeTab === 'usage' && renderUsageTab()}
      {activeTab === 'settings' && renderSettingsTab()}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f0f0',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    fontSize: 18,
    color: '#666',
  },
  permissionContainer: {
    flex: 1,
    padding: 20,
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  permissionTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 16,
  },
  permissionText: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
    color: '#666',
  },
  permissionList: {
    marginBottom: 20,
  },
  permissionItem: {
    fontSize: 16,
    marginBottom: 8,
    color: '#333',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    paddingVertical: 16,
    backgroundColor: '#fff',
    marginBottom: 8,
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    marginBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  tab: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
  },
  activeTab: {
    borderBottomWidth: 2,
    borderBottomColor: '#4CAF50',
  },
  tabText: {
    fontSize: 14,
    color: '#666',
  },
  activeTabText: {
    color: '#4CAF50',
    fontWeight: '600',
  },
  tabContent: {
    flex: 1,
    padding: 12,
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
    color: '#333',
  },
  statusRow: {
    flexDirection: 'row',
    marginBottom: 4,
  },
  statusValue: {
    fontWeight: '500',
  },
  statusDetail: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  active: {
    color: '#4CAF50',
  },
  inactive: {
    color: '#999',
  },
  helperText: {
    fontSize: 12,
    color: '#999',
    marginBottom: 8,
  },
  buttonRow: {
    flexDirection: 'row',
  },
  buttonSpacer: {
    width: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 8,
    fontSize: 16,
  },
  appItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  defaultIcon: {
    backgroundColor: '#e0e0e0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  appName: {
    flex: 1,
    fontSize: 14,
    marginLeft: 12,
    color: '#333',
  },
  checkbox: {
    fontSize: 20,
    color: '#ccc',
    paddingHorizontal: 8,
  },
  checked: {
    color: '#4CAF50',
  },
  selectedCount: {
    fontSize: 12,
    color: '#4CAF50',
    marginBottom: 8,
  },
  usageItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  usageInfo: {
    flex: 1,
    marginLeft: 12,
  },
  usageTime: {
    fontSize: 14,
    color: '#4CAF50',
    fontWeight: '500',
  },
});