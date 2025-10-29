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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Speed',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showSpeedChart,
                            onChanged: null, // Disabled - for future Raspberry Pi control
                            activeTrackColor: ColorUtils.main,
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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Cadence',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showCadenceChart,
                            onChanged: null, // Disabled - for future Raspberry Pi control
                            activeTrackColor: ColorUtils.main,
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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Power',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showPowerChart,
                            onChanged: null, // Disabled - for future Raspberry Pi control
                            activeTrackColor: ColorUtils.main,
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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Altitude',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showAltitudeChart,
                            onChanged: null, // Disabled - for future Raspberry Pi control
                            activeTrackColor: ColorUtils.main,
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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Distance traveled',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showDistanceTraveled,
                            onChanged: null, // Disabled - for future Raspberry Pi control
                            activeTrackColor: ColorUtils.main,
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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Calories',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showCalories,
                            onChanged: null, // Disabled - for future Raspberry Pi control
                            activeTrackColor: ColorUtils.main,
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
                                color: ColorUtils.main,
                                size: 20,
                              ),
                              const SizedBox(width: 12),
                              const Text(
                                'Map',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ],
                          ),
                          CupertinoSwitch(
                            value: state.showMap,
                            onChanged: null, // Disabled - map functionality removed
                            activeTrackColor: ColorUtils.main,
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
                            color: ColorUtils.main.withOpacity(0.1),
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
                            border: Border.all(color: ColorUtils.main.withOpacity(0.3)),
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
                                color: ColorUtils.main,
                              ),
                            ),
                            if (state.localDeviceName != null) ...[
                              const SizedBox(height: 4),
                              Text(
                                'BLE Name: ${state.localDeviceName}',
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: CupertinoColors.secondaryLabel,
                                ),
                              ),
                            ],
                              if (state.localDeviceMac != null) ...[
                                const SizedBox(height: 2),
                                Text(
                                  'MAC Address: ${state.localDeviceMac}',
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: CupertinoColors.secondaryLabel,
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
