import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  Button,
  StyleSheet,
  TextInput,
  FlatList,
  Alert,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import AppBlocker, { BlockerState, PermissionStatus } from '../src';

export default function AppBlockerExample() {
  const [state, setState] = useState<BlockerState | null>(null);
  const [permissions, setPermissions] = useState<PermissionStatus | null>(null);
  const [apps, setApps] = useState<string[]>([]);
  const [selectedApps, setSelectedApps] = useState<string[]>([]);
  const [scheduleTime, setScheduleTime] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    initializeBlocker();
  }, []);

  const initializeBlocker = async () => {
    try {
      const perms = await AppBlocker.checkPermissions();
      setPermissions(perms);

      if (!perms.usageStats || !perms.overlay) {
        Alert.alert(
          'Permissions Required',
          'Please grant the required permissions to use the app blocker.',
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Grant Permissions', onPress: requestPermissions },
          ]
        );
        return;
      }

      const currentState = await AppBlocker.getState();
      setState(currentState);

      const installedApps = await AppBlocker.getInstalledApps();
      setApps(installedApps);
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
      if (selectedApps.length > 0) {
        await AppBlocker.block(selectedApps);
      } else {
        await AppBlocker.blockAll();
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
      await AppBlocker.schedule(scheduleTime);
      
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

  if (isLoading) {
    return (
      <View style={styles.container}>
        <Text>Loading...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>App Blocker</Text>
      
      {/* Permission Status */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Permissions</Text>
        <Text>Usage Stats: {permissions?.usageStats ? '✓' : '✗'}</Text>
        <Text>Overlay: {permissions?.overlay ? '✓' : '✗'}</Text>
        {!permissions?.usageStats || !permissions?.overlay ? (
          <Button title="Request Permissions" onPress={requestPermissions} />
        ) : null}
      </View>

      {/* Current State */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Current State</Text>
        <Text>Blocking: {state?.isBlocking ? 'Active' : 'Inactive'}</Text>
        <Text>Block All: {state?.blockAll ? 'Yes' : 'No'}</Text>
        <Text>Blocked Apps: {state?.blockedApps?.length || 0}</Text>
        <Text>Scheduled Time: {state?.scheduledTime || 'None'}</Text>
        <Text>Schedule Active: {state?.scheduleActivated ? 'Yes' : 'No'}</Text>
      </View>

      {/* Block Controls */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Block Apps</Text>
        <Button title="Block All Non-System Apps" onPress={handleBlock} />
        <View style={styles.spacer} />
        <Button title="Clear Blocking" onPress={handleClear} />
      </View>

      {/* Schedule */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Schedule Blocking</Text>
        <TextInput
          style={styles.input}
          placeholder="HH:mm (e.g., 21:00)"
          value={scheduleTime}
          onChangeText={setScheduleTime}
          keyboardType="numbers-and-punctuation"
        />
        <Button title="Schedule" onPress={handleSchedule} />
      </View>

      {/* App Selection */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Select Apps to Block</Text>
        <FlatList
          data={apps.slice(0, 20)} // Limit for demo
          keyExtractor={(item) => item}
          renderItem={({ item }) => (
            <View style={styles.appItem}>
              <Text style={styles.appName}>{item}</Text>
              <Button
                title={selectedApps.includes(item) ? '✓' : '+'}
                onPress={() => toggleAppSelection(item)}
              />
            </View>
          )}
        />
        {selectedApps.length > 0 && (
          <Button title={`Block Selected (${selectedApps.length})`} onPress={handleBlock} />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 24,
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
  spacer: {
    height: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    padding: 8,
    marginBottom: 8,
  },
  appItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  appName: {
    flex: 1,
    fontSize: 14,
  },
});
