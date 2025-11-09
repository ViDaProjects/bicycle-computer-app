import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:quickalert/quickalert.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../core/utils/storage_utils.dart';
import '../../../data/repositories/user_repository_impl.dart';
import '../../../domain/entities/user.dart';
import '../../../main.dart';
import '../../common/core/enums/infinite_scroll_list.enum.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/core/widgets/view_model/infinite_scroll_list_view_model.dart';
import 'state/settings_state.dart';

const String bleChannel = 'com.beforbike.ble';

final settingsViewModelProvider =
    NotifierProvider<SettingsViewModel, SettingsState>(
  () => SettingsViewModel(),
);

class SettingsViewModel extends Notifier<SettingsState> {
  static const MethodChannel _channel = MethodChannel(bleChannel);
  static const MethodChannel _databaseChannel = MethodChannel('com.beforbike.app/database');

  @override
  SettingsState build() {
    // Initialize BLE status
    _updateBluetoothStatus();
    _loadVisibilitySettings();
    return SettingsState.initial();
  }

  /// Loads visibility settings from SharedPreferences.
  Future<void> _loadVisibilitySettings() async {
    final prefs = await SharedPreferences.getInstance();
    state = state.copyWith(
      showSpeedChart: prefs.getBool('showSpeedChart') ?? true,
      showCadenceChart: prefs.getBool('showCadenceChart') ?? true,
      showPowerChart: prefs.getBool('showPowerChart') ?? true,
      showAltitudeChart: prefs.getBool('showAltitudeChart') ?? true,
      showDistanceTraveled: prefs.getBool('showDistanceTraveled') ?? true,
      showCalories: prefs.getBool('showCalories') ?? true,
    );
  }

  Future<void> _updateBluetoothStatus() async {
    try {
      // First check if Bluetooth adapter is enabled
      final isAdapterEnabled = await _channel.invokeMethod('isBluetoothAdapterEnabled') as bool;
      if (!isAdapterEnabled) {
        // Request user to enable Bluetooth
        await _requestEnableBluetooth();
        // Check again after request
        await Future.delayed(const Duration(seconds: 1));
        final isAdapterEnabledAfter = await _channel.invokeMethod('isBluetoothAdapterEnabled') as bool;
        if (!isAdapterEnabledAfter) {
          // User didn't enable Bluetooth, don't start BLE service
          state = state.copyWith(isBluetoothEnabled: false);
          return;
        }
      }

      // Now check if BLE service is running
      final isEnabled = await _channel.invokeMethod('isBleEnabled') as bool;
      state = state.copyWith(isBluetoothEnabled: isEnabled);
      if (isEnabled) {
        _updateConnectedStatus();
        _updateLocalDeviceInfo();
      }
    } catch (e) {
      // Handle errors
    }
  }

  Future<void> _requestEnableBluetooth() async {
    try {
      await _channel.invokeMethod('requestEnableBluetooth');
    } catch (e) {
      // Handle errors
    }
  }

  Future<void> _updateLocalDeviceInfo() async {
    try {
      final name = await _channel.invokeMethod('getLocalBluetoothName') as String?;
      final mac = await _channel.invokeMethod('getLocalBluetoothMac') as String?;
      // Filter out fake MAC address (02:00:00:00:00:00) which is returned when MAC is not accessible
      final filteredMac = (mac != null && mac != "02:00:00:00:00:00") ? mac : null;
      state = state.copyWith(localDeviceName: name, localDeviceMac: filteredMac);
    } catch (e) {
      // Handle errors
    }
  }

  Future<void> _updateConnectedStatus() async {
    try {
      final isConnected = await _channel.invokeMethod('getConnectedStatus') as bool;
      if (isConnected) {
        final name = await _channel.invokeMethod('getConnectedDeviceName') as String;
        final mac = await _channel.invokeMethod('getConnectedDeviceMac') as String;
        state = state.copyWith(isBleConnected: true, connectedDeviceName: name, connectedDeviceMac: mac);
      } else {
        state = state.copyWith(isBleConnected: false, connectedDeviceName: null, connectedDeviceMac: null);
      }
    } catch (e) {
      // Handle errors
    }
  }

  /// Scan for BLE devices advertising the bicycle computer service
  Future<void> scanAndConnectToBicycleComputer() async {
    try {
      // Request permissions via Android native code
      await _channel.invokeMethod('requestPermissions');
      await _channel.invokeMethod('scanAndConnectToDevice');
      await _updateConnectedStatus();
    } catch (e) {
      // Handle error
    }
  }

  /// Disconnect from BLE device
  Future<void> disconnectFromDevice() async {
    try {
      await _channel.invokeMethod('disconnectDevice');
      state = state.copyWith(isBleConnected: false, connectedDeviceName: null, connectedDeviceMac: null);
    } catch (e) {
      // Handle error
    }
  }

  /// Toggles the BLE service state in the app (doesn't control system Bluetooth).
  void toggleBluetooth(bool enabled) async {
    // Set loading
    state = state.copyWith(isLoading: true);

    try {
      // Request permissions first if enabling
      if (enabled) {
        await _channel.invokeMethod('requestPermissions');
      }

      // Control BLE service on Android side
      final result = await _channel.invokeMethod('setBleEnabled', {'enabled': enabled}) as bool?;

      if (enabled && result == false) {
        // Failed - was likely permissions denied. Show error to user
        state = state.copyWith(isLoading: false);
        return;
      }

      // Success - update UI state
      state = state.copyWith(isBluetoothEnabled: enabled, isLoading: false);
      if (enabled) {
        await _updateLocalDeviceInfo();
      }
      await _updateConnectedStatus();

      if (!enabled) {
        state = state.copyWith(isBleConnected: false, connectedDeviceName: null, connectedDeviceMac: null);
      }

    } catch (e) {
      // Handle errors
      state = state.copyWith(isLoading: false);
    }
  }

  /// Toggles the dark mode state.
  void toggleDarkMode() {
    state = state.copyWith(isDarkMode: !state.isDarkMode);
    // Update the global theme mode
    ref.read(themeModeProvider.notifier).setThemeMode(
        state.isDarkMode ? ThemeMode.dark : ThemeMode.light);
  }

  /// Toggles the speed chart visibility and sends command to bicycle computer.
  void toggleSpeedChart() {
    final newValue = !state.showSpeedChart;
    state = state.copyWith(showSpeedChart: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showSpeedChart', newValue);
    });
    _sendVisibilityCommand('speed', newValue);
  }

  /// Toggles the cadence chart visibility and sends command to bicycle computer.
  void toggleCadenceChart() {
    final newValue = !state.showCadenceChart;
    state = state.copyWith(showCadenceChart: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showCadenceChart', newValue);
    });
    _sendVisibilityCommand('cadence', newValue);
  }

  /// Toggles the power chart visibility and sends command to bicycle computer.
  void togglePowerChart() {
    final newValue = !state.showPowerChart;
    state = state.copyWith(showPowerChart: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showPowerChart', newValue);
    });
    _sendVisibilityCommand('power', newValue);
  }

  /// Toggles the altitude chart visibility and sends command to bicycle computer.
  void toggleAltitudeChart() {
    final newValue = !state.showAltitudeChart;
    state = state.copyWith(showAltitudeChart: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showAltitudeChart', newValue);
    });
    _sendVisibilityCommand('altitude', newValue);
  }

  /// Toggles the distance traveled visibility and sends command to bicycle computer.
  void toggleDistanceTraveled() {
    final newValue = !state.showDistanceTraveled;
    state = state.copyWith(showDistanceTraveled: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showDistanceTraveled', newValue);
    });
    _sendVisibilityCommand('distance', newValue);
  }

  /// Toggles the calories visibility and sends command to bicycle computer.
  void toggleCalories() {
    final newValue = !state.showCalories;
    state = state.copyWith(showCalories: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showCalories', newValue);
    });
    _sendVisibilityCommand('calories', newValue);
  }

  /// Toggles the map visibility.
  void toggleMap() {
    final newValue = !state.showMap;
    state = state.copyWith(showMap: newValue);
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('showMap', newValue);
    });
    _sendVisibilityCommand('map', newValue);
  }

  /// Sends a visibility command to the bicycle computer via BLE.
  void _sendVisibilityCommand(String statistic, bool visible) {
    try {
      final command = '{"type": "visibility", "statistic": "$statistic", "visible": $visible}';
      _databaseChannel.invokeMethod('sendData', {'data': command.codeUnits});
    } catch (e) {
      // Silently fail if BLE is not connected or command fails
    }
  }

  /// Deletes the user account.
  Future<void> deleteUserAccount() async {
    try {
      state = state.copyWith(isLoading: true);
      await ref.read(userRepositoryProvider).delete();
      await clearStorage();
      await resetInfiniteLists();
      // App will restart and go to home screen
    } catch (error) {
      state = state.copyWith(isLoading: false);
    }
  }

  /// Display an alert to confirm or cancel the deletion of the account
  void showDeleteAccountAlert(BuildContext context, String title,
      String confirmBtnText, String cancelBtnText) {
    QuickAlert.show(
        context: context,
        type: QuickAlertType.confirm,
        title: title,
        confirmBtnText: confirmBtnText,
        cancelBtnText: cancelBtnText,
        confirmBtnColor: ColorUtils.red,
        onCancelBtnTap: () => Navigator.of(context).pop(),
        onConfirmBtnTap: () {
          Navigator.of(context).pop();
          deleteUserAccount();
        });
  }

  /// Clears the local storage.
  Future<void> clearStorage() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    await prefs.clear();
  }

  /// reset infinite lists
  Future<void> resetInfiniteLists() async {
    ref
        .read(infiniteScrollListViewModelProvider(
                InfiniteScrollListEnum.myActivities.toString())
            .notifier)
        .reset();
    ref
        .read(infiniteScrollListViewModelProvider(
                InfiniteScrollListEnum.community.toString())
            .notifier)
        .reset();
    User? currentUser = await StorageUtils.getUser();
    if (currentUser != null) {
      ref
          .read(infiniteScrollListViewModelProvider(
                  '${InfiniteScrollListEnum.profile}_${currentUser.id}')
              .notifier)
          .reset();
    }
  }
}
