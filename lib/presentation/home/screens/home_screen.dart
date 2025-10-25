import 'package:flutter/material.dart';
import 'package:google_nav_bar/google_nav_bar.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../common/core/utils/color_utils.dart';
import '../../my_activities/screens/activity_list_screen.dart';
import '../../settings/screens/settings_screen.dart';
import '../../statistics/screens/statistics_screen.dart';
import 'map_screen.dart';
import '../view_model/home_view_model.dart';

/// An enumeration representing the available tabs in the home screen.
enum Tabs { list, map, statistics, settings }

/// The home screen widget.
class HomeScreen extends HookConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(homeViewModelProvider);
    final homeViewModel = ref.watch(homeViewModelProvider.notifier);
    final currentIndex = state.currentIndex;

    final tabs = [
      ActivityListScreen(),
      const MapScreen(),
      StatisticsScreen(selectedActivity: state.selectedActivity),
      const SettingsScreen(),
    ];

    return Scaffold(
        body: SafeArea(child: tabs[currentIndex]),
        bottomNavigationBar: Container(
            color: ColorUtils.mainMedium,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 20),
              child: GNav(
                backgroundColor: ColorUtils.mainMedium,
                color: ColorUtils.white,
                activeColor: ColorUtils.white,
                tabBackgroundColor: ColorUtils.mainDarker,
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
                selectedIndex: currentIndex,
                onTabChange: (value) {
                  homeViewModel.setCurrentIndex(value);
                },
                gap: 4,
                tabs: [
                  GButton(
                    icon: Icons.list,
                    text: 'List',
                  ),
                  GButton(
                    icon: Icons.map,
                    text: 'Map',
                  ),
                  GButton(
                    icon: Icons.bar_chart,
                    text: 'Statistics',
                  ),
                  GButton(
                    icon: Icons.settings,
                    text: 'Settings',
                  ),
                ],
              ),
            )));
  }
}
