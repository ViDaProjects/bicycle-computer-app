import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../../domain/entities/activity.dart';
import '../../core/utils/color_utils.dart';
import '../view_model/activity_item_view_model.dart';
import '../../../my_activities/view_model/activity_list_view_model.dart';
import 'activity_item_details.dart';

class ActivityItem extends HookConsumerWidget {
  final int index;
  final Activity activity;
  final bool displayUserName;
  final bool canOpenActivity;

  const ActivityItem({
    super.key,
    required this.activity,
    required this.index,
    this.displayUserName = false,
    this.canOpenActivity = true,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider =
        ref.read(activityItemViewModelProvider(activity.id).notifier);
    final state = ref.watch(activityItemViewModelProvider(activity.id));

    // Get activity list view model for selection functionality
    final activityListProvider = ref.watch(activityListViewModelProvider.notifier);
    final activityListState = ref.watch(activityListViewModelProvider);

    const double borderRadius = 24;

    Activity currentActivity = state.activity ?? activity;

    final titleColor = ColorUtils.generateColorFromCalories(currentActivity.calories);

    final gradientColors = ColorUtils.generateGradientFromCalories(currentActivity.calories);
    
    final isDarkMode = Theme.of(context).brightness == Brightness.dark;
    final isSelected = activityListState.selectedActivities.contains(activity.id);

    return InkWell(
      onTap: () async {
        if (activityListState.isSelectionMode) {
          // In selection mode, toggle selection
          activityListProvider.toggleActivitySelection(activity.id);
        } else if (canOpenActivity) {
          // Normal mode, open activity
          final activityDetails =
              await provider.getActivityDetails(activity);
          provider.goToStatistics(activityDetails);
        }
      },
      onLongPress: () {
        // Enter selection mode and select this activity
        if (!activityListState.isSelectionMode) {
          activityListProvider.toggleSelectionMode();
        }
        activityListProvider.toggleActivitySelection(activity.id);
      },
      child: Card(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(borderRadius),
        ),
        elevation: isSelected ? 8 : 2,
        margin: const EdgeInsets.all(8),
        child: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: isSelected 
                ? [Colors.red.shade200, Colors.red.shade400]
                : gradientColors,
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(borderRadius),
            border: isSelected ? Border.all(color: Colors.red, width: 3) : null,
            boxShadow: [
              BoxShadow(
                color: Colors.grey.withValues(alpha: 0.2),
                spreadRadius: 1,
                blurRadius: 4,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Column(
            children: [
              Row(
                children: [
                  if (!displayUserName)
                    SizedBox(
                      width: 120,
                      height: 150,
                      child: Center(
                        child: Icon(
                          Icons.directions_bike,
                          color: isDarkMode ? Colors.lightGreen : Colors.blueGrey.shade700,
                          size: 60,
                        ),
                      ),
                    ),
                  if (!displayUserName)
                    SizedBox(
                      height: 84,
                      child: VerticalDivider(
                        key: Key(activity.id.toString()),
                        color: isDarkMode ? Colors.lightGreen : Colors.blueGrey.shade400,
                        thickness: 4,
                        width: 4,
                      ),
                    ),
                  if (!displayUserName) const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        ActivityItemDetails(
                          displayUserName: displayUserName,
                          activity: activity,
                          color: titleColor,
                        ),
                      ],
                    ),
                  ),
                  if (canOpenActivity)
                    Icon(
                      Icons.navigate_next,
                      color: ColorUtils.black,
                      size: 30,
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}