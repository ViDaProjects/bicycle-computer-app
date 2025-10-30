import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:quickalert/quickalert.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../core/utils/storage_utils.dart';
import '../../../data/repositories/user_repository_impl.dart';
import '../../../domain/entities/user.dart';
import '../../../main.dart';
import '../../common/core/enums/infinite_scroll_list.enum.dart';
import '../../common/core/services/permission_service.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/core/widgets/view_model/infinite_scroll_list_view_model.dart';
import 'state/settings_state.dart';

const String bleChannel = 'com.beforbike.ble';

final settingsViewModelProvider =
    StateNotifierProvider.autoDispose<SettingsViewModel, SettingsState>(
  (ref) => SettingsViewModel(ref),
);

class SettingsViewModel extends StateNotifier<SettingsState> {
  Ref ref;
  static const MethodChannel _channel = MethodChannel(bleChannel);

  /// Manages the state and logic of the settings screen.
  ///
  /// [ref] - The reference to the hooks riverpod container.
  SettingsViewModel(this.ref) : super(SettingsState.initial()) {
    // Initialize BLE status
    _updateBluetoothStatus();
  }

  Future<void> _updateBluetoothStatus() async {
    try {
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
    // Request permissions first
    final permissionService = PermissionService();
    final hasPermission = await permissionService.checkAllPermissionsGranted();
    if (!hasPermission) {
      await _channel.invokeMethod('requestPermissions');
      final newCheck = await permissionService.checkAllPermissionsGranted();
      if (!newCheck) {
        return;
      }
    }

    try {
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

    if (enabled) {
      // Request permissions when enabling BLE via Android side
      try {
        await _channel.invokeMethod('requestPermissions');
      } catch (e) {
        // Handle errors if needed
      }
    }

    // Control BLE service on Android side
    try {
      await _channel.invokeMethod('setBleEnabled', {'enabled': enabled});
    } catch (e) {
      // Handle errors if needed
      state = state.copyWith(isLoading: false);
      return;
    }

    // Update status after changing service state
    await _updateConnectedStatus();
    if (enabled) {
      _updateLocalDeviceInfo();
    }

    state = state.copyWith(isBluetoothEnabled: enabled, isLoading: false);
    if (!enabled) {
      state = state.copyWith(isBleConnected: false, connectedDeviceName: null, connectedDeviceMac: null);
    }
  }

  /// Toggles the dark mode state.
  void toggleDarkMode() {
    state = state.copyWith(isDarkMode: !state.isDarkMode);
    // Update the global theme mode
    ref.read(themeModeProvider.notifier).state =
        state.isDarkMode ? ThemeMode.dark : ThemeMode.light;
  }

  /// Toggles the speed chart visibility (no functionality for now).
  void toggleSpeedChart() {
    state = state.copyWith(showSpeedChart: !state.showSpeedChart);
  }

  /// Toggles the cadence chart visibility (no functionality for now).
  void toggleCadenceChart() {
    state = state.copyWith(showCadenceChart: !state.showCadenceChart);
  }

  /// Toggles the power chart visibility (no functionality for now).
  void togglePowerChart() {
    state = state.copyWith(showPowerChart: !state.showPowerChart);
  }

  /// Toggles the altitude chart visibility (no functionality for now).
  void toggleAltitudeChart() {
    state = state.copyWith(showAltitudeChart: !state.showAltitudeChart);
  }

  /// Toggles the distance traveled visibility (no functionality for now).
  void toggleDistanceTraveled() {
    state = state.copyWith(showDistanceTraveled: !state.showDistanceTraveled);
  }

  /// Toggles the calories visibility (no functionality for now).
  void toggleCalories() {
    state = state.copyWith(showCalories: !state.showCalories);
  }

  /// Toggles the map visibility (no functionality for now).
  void toggleMap() {
    state = state.copyWith(showMap: !state.showMap);
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
