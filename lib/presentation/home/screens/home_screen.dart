import 'package:flutter/material.dart';
import 'package:be_for_bike/l10n/app_localizations.dart';
import 'package:google_nav_bar/google_nav_bar.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../common/core/utils/color_utils.dart';
import '../../my_activities/screens/activity_list_screen.dart';
import '../../new_activity/screens/new_activity_screen.dart';
import '../../settings/screens/settings_screen.dart';
import '../../statistics/screens/statistics_screen.dart';
import '../view_model/home_view_model.dart';

/// An enumeration representing the available tabs in the home screen.
enum Tabs { home, list, statistics, settings }

/// The home screen widget.
class HomeScreen extends HookConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(homeViewModelProvider);
    final homeViewModel = ref.watch(homeViewModelProvider.notifier);
    final currentIndex = state.currentIndex;

    final tabs = [
      const NewActivityScreen(),
      ActivityListScreen(),
      const StatisticsScreen(),
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
                    icon: Icons.flash_on,
                    text: AppLocalizations.of(context)!.start_activity,
                  ),
                  GButton(
                    icon: Icons.list,
                    text: AppLocalizations.of(context)!.list,
                  ),
                  GButton(
                    icon: Icons.bar_chart,
                    text: AppLocalizations.of(context)!.statistics,
                  ),
                  GButton(
                    icon: Icons.settings,
                    text: AppLocalizations.of(context)!.settings,
                  ),
                ],
              ),
            )));
  }
}
