import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../common/core/utils/color_utils.dart';
import '../../common/core/utils/ui_utils.dart';
import '../view_model/settings_view_model.dart';

class SettingsScreen extends HookConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(settingsViewModelProvider);
    final provider = ref.watch(settingsViewModelProvider.notifier);

    return Scaffold(
      body: Center(
        child: state.isLoading
            ? Center(child: UIUtils.loader)
            : SingleChildScrollView(
                child: Column(
                  children: [
                    const SizedBox(height: 20),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Row(
                                  children: [
                                    Icon(
                                      Icons.bluetooth,
                                      color: ColorUtils.main,
                                      size: 24,
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Text(
                                        'BLE Service',
                                        style: TextStyle(
                                          fontSize: 18,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              CupertinoSwitch(
                                value: state.isBluetoothEnabled,
                                onChanged: (value) => provider.toggleBluetooth(value),
                                activeTrackColor: ColorUtils.main,
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              const SizedBox(width: 36), // Align with text
                              Expanded(
                                child: Text(
                                  !state.isBluetoothEnabled
                                      ? 'Status: Service Off'
                                      : state.isBleConnected && state.connectedDeviceName != null
                                          ? 'Status: Connected - ${state.connectedDeviceName} (${state.connectedDeviceMac})'
                                          : 'Status: Disconnected',
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Theme.of(context).brightness == Brightness.dark
                                        ? Colors.white70
                                        : CupertinoColors.secondaryLabel,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          if (state.isBluetoothEnabled) ...[
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                const SizedBox(width: 36),
                                ElevatedButton(
                                  onPressed: state.isBleConnected
                                      ? provider.disconnectFromDevice
                                      : provider.scanAndConnectToBicycleComputer,
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: ColorUtils.main,
                                    foregroundColor: Colors.white,
                                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                                  ),
                                  child: Text(state.isBleConnected ? 'Disconnect' : 'Connect to Device'),
                                ),
                              ],
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                state.isDarkMode ? Icons.nightlight_round : Icons.wb_sunny,
                                color: ColorUtils.main,
                                size: 24,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Dark Mode',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.isDarkMode,
                            onChanged: (value) => provider.toggleDarkMode(),
                            activeTrackColor: ColorUtils.main,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 16.0),
                      child: Text(
                        'Bicycle computer visibility',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    if (!state.isBluetoothEnabled) ...[
                      const SizedBox(height: 4),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16.0),
                        child: Text(
                          'These settings are only available when BLE service is active',
                          style: TextStyle(
                            fontSize: 12,
                            color: CupertinoColors.systemGrey,
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.speed,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Speed',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showSpeedChart,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.toggleSpeedChart() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.pedal_bike,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Cadence',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showCadenceChart,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.toggleCadenceChart() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.flash_on,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Power',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showPowerChart,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.togglePowerChart() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.terrain,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Altitude',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showAltitudeChart,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.toggleAltitudeChart() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.straighten,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Distance traveled',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showDistanceTraveled,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.toggleDistanceTraveled() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.local_fire_department,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Calories',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showCalories,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.toggleCalories() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.map,
                                color: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                'Map',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                  color: state.isBluetoothEnabled ? null : CupertinoColors.systemGrey,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showMap,
                            onChanged: state.isBluetoothEnabled ? (value) => provider.toggleMap() : null,
                            activeTrackColor: state.isBluetoothEnabled ? ColorUtils.main : CupertinoColors.systemGrey,
                          ),
                        ],
                      ),
                    ),
                    if (state.connectedDeviceName != null && state.connectedDeviceMac != null) ...[
                      const SizedBox(height: 20),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16.0),
                        child: Container(
                          padding: const EdgeInsets.all(12),
                          width: double.infinity,
                          decoration: BoxDecoration(
                            color: ColorUtils.main.withAlpha(26),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                            Text(
                              'Connected BLE Device',
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: ColorUtils.main,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Name: ${state.connectedDeviceName}',
                              style: const TextStyle(
                                fontSize: 12,
                                color: CupertinoColors.secondaryLabel,
                              ),
                            ),
                              Text(
                                'MAC: ${state.connectedDeviceMac}',
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: CupertinoColors.secondaryLabel,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                    if (state.localDeviceName != null || state.localDeviceMac != null) ...[
                      const SizedBox(height: 20),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16.0),
                        child: Container(
                          padding: const EdgeInsets.all(12),
                          width: double.infinity,
                          decoration: BoxDecoration(
                            border: Border.all(color: ColorUtils.main.withAlpha(77)),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                            Text(
                              'Local Device Information',
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: state.isDarkMode ? Colors.white : Colors.black,
                              ),
                            ),
                            if (state.localDeviceName != null) ...[
                              const SizedBox(height: 4),
                              Text(
                                'BLE Name: ${state.localDeviceName}',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: state.isDarkMode ? Colors.white : Colors.black,
                                ),
                              ),
                            ],
                              if (state.localDeviceMac != null) ...[
                                const SizedBox(height: 2),
                                Text(
                                  'MAC Address: ${state.localDeviceMac}',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: state.isDarkMode ? Colors.white : Colors.black,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 20), // Prevent overflow
                  ],
                ),
            ),
      ),
    );
  }
}
