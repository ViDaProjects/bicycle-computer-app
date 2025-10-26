import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:google_nav_bar/google_nav_bar.dart';

import '../../../domain/entities/activity.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/core/utils/ui_utils.dart';
import '../../home/view_model/home_view_model.dart';
import '../view_model/activity_details_view_model.dart';
import '../widgets/details_tab.dart';
import '../widgets/graph_tab.dart';

/// The screen that displays details of a specific activity.
class ActivityDetailsScreen extends HookConsumerWidget {
  final Activity activity;

  const ActivityDetailsScreen({super.key, required this.activity});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(activityDetailsViewModelProvider);
    final homeViewModel = ref.read(homeViewModelProvider.notifier);
    final homeState = ref.watch(homeViewModelProvider);

    final tabController = useTabController(initialLength: 2);

    return Scaffold(
        body: SafeArea(
            child: state.isLoading
                ? Center(child: UIUtils.loader)
                : Column(children: [
                    Container(
                      padding: const EdgeInsets.only(left: 25, top: 12),
                      child: Row(children: [
                        Text(
                          'Cycling',
                          style: TextStyle(
                              color: ColorUtils.blueGrey,
                              fontSize: 28,
                              fontWeight: FontWeight.bold),
                        ),
                        const Spacer(),
                      ]),
                    ),
                    Expanded(
                        child: DefaultTabController(
                            length: 2,
                            child: Scaffold(
                                appBar: TabBar(
                                    controller: tabController,
                                    labelColor: ColorUtils.blueGrey,
                                    dividerColor: ColorUtils.blueGrey,
                                    indicatorColor: ColorUtils.blueGrey,
                                    tabs: const [
                                      Tab(
                                        icon: Icon(Icons.short_text),
                                      ),
                                      Tab(
                                        icon: Icon(Icons.graphic_eq_outlined),
                                      ),
                                    ]),
                                body: TabBarView(
                                    controller: tabController,
                                    children: [
                                      DetailsTab(activity: activity),
                                      GraphTab(activity: activity)
                                    ])))),
                    Container(
                        color: ColorUtils.mainMedium,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 20),
                          child: GNav(
                            backgroundColor: ColorUtils.mainMedium,
                            color: ColorUtils.white,
                            activeColor: ColorUtils.white,
                            tabBackgroundColor: ColorUtils.mainDarker,
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
                            selectedIndex: homeState.currentIndex,
                            onTabChange: (value) {
                              homeViewModel.setCurrentIndex(value);
                            },
                            gap: 4,
                            tabs: const [
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
                        ))
                  ])));
  }
}
