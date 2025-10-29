import 'package:flutter/material.dart';
import 'package:google_nav_bar/google_nav_bar.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../common/core/utils/color_utils.dart';
import '../../../core/utils/audio_service.dart';
import '../../my_activities/screens/activity_list_screen.dart';
import '../../settings/screens/settings_screen.dart';
import '../../statistics/screens/statistics_screen.dart';
import 'map_screen.dart';
import '../view_model/home_view_model.dart';
import '../../settings/view_model/settings_view_model.dart';

/// An enumeration representing the available tabs in the home screen.
enum Tabs { list, map, statistics, settings }

/// The home screen widget.
class HomeScreen extends HookConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(homeViewModelProvider);
    final homeViewModel = ref.watch(homeViewModelProvider.notifier);
    final settingsState = ref.watch(settingsViewModelProvider);
    final currentIndex = state.currentIndex;

    // Build tabs based on visibility settings
    final tabs = <Widget>[];
    final gButtons = <GButton>[];

    // Always show list tab
    tabs.add(ActivityListScreen());
    gButtons.add(const GButton(
      icon: Icons.list,
      text: 'List',
    ));

    // Show map tab only if enabled in settings
    if (settingsState.showMap) {
      tabs.add(const MapScreen());
      gButtons.add(const GButton(
        icon: Icons.map,
        text: 'Map',
      ));
    }

    // Always show statistics tab
    tabs.add(StatisticsScreen(selectedActivity: state.selectedActivity));
    gButtons.add(const GButton(
      icon: Icons.bar_chart,
      text: 'Statistics',
    ));

    // Always show settings tab
    tabs.add(const SettingsScreen());
    gButtons.add(const GButton(
      icon: Icons.settings,
      text: 'Settings',
    ));

    // Adjust current index if map tab is hidden and we're on a higher index
    var adjustedIndex = currentIndex;
    if (!settingsState.showMap && currentIndex > 0) {
      adjustedIndex = currentIndex - 1;
    }

    return Scaffold(
        body: SafeArea(child: tabs[adjustedIndex]),
        bottomNavigationBar: Container(
            color: ColorUtils.mainMedium,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
              child: GNav(
                backgroundColor: ColorUtils.mainMedium,
                color: ColorUtils.white,
                activeColor: ColorUtils.white,
                tabBackgroundColor: ColorUtils.mainDarker,
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                selectedIndex: adjustedIndex,
                onTabChange: (value) {
                  AudioService().playNavigationClick();
                  // Adjust the index when map is hidden
                  var actualIndex = value;
                  if (!settingsState.showMap && value >= 1) {
                    actualIndex = value + 1;
                  }
                  homeViewModel.setCurrentIndex(actualIndex);
                },
                gap: 4,
                iconSize: 20,
                tabs: gButtons,
              ),
            )));
  }
}
