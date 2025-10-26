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
            : Column(
                children: [
                  const SizedBox(height: 20),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16.0),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.bluetooth,
                              color: ColorUtils.main,
                              size: 24,
                            ),
                            const SizedBox(width: 12),
                            const Text(
                              'Bicycle BLE connection',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ],
                        ),
                        CupertinoSwitch(
                          value: state.isBluetoothEnabled,
                          onChanged: (value) => provider.toggleBluetooth(),
                          activeTrackColor: ColorUtils.main,
                        ),
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
                ],
              ),
      ),
    );
  }
}
