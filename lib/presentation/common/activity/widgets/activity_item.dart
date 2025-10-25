import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../../domain/entities/activity.dart';
import '../../core/utils/activity_utils.dart';
import '../../core/utils/color_utils.dart';
import '../../core/utils/ui_utils.dart';
import '../view_model/activity_item_comments_view_model.dart';
import '../view_model/activity_item_like_view_model.dart';
import '../view_model/activity_item_view_model.dart';
import 'activity_item_details.dart';
import 'activity_item_interaction.dart';
import 'activity_item_user_informations.dart';

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

    const double borderRadius = 24;

    Activity currentActivity = state.activity ?? activity;

    final titleColor = ColorUtils.generateColorFromCalories(currentActivity.calories); // Dynamic color based on calories

    // Create gradient based on calories
    final gradientColors = ColorUtils.generateGradientFromCalories(currentActivity.calories);

    return FutureBuilder<void>(future: Future(() async {
      final likeProvider =
          ref.read(activityItemLikeViewModelProvider(activity.id).notifier);
      final commentProvider =
          ref.read(activityItemCommentsViewModelProvider(activity.id).notifier);
      likeProvider.setHasUserLiked(activity.hasCurrentUserLiked);
      likeProvider.setLikesCount(activity.likesCount);
      commentProvider.setComments(activity.comments.toList());
    }), builder: (context, snapshot) {
      if (snapshot.connectionState == ConnectionState.waiting) {
        return Center(child: UIUtils.loader);
      } else if (snapshot.hasError) {
        return Text('Error: ${snapshot.error}');
      } else {
        return InkWell(
          onTap: () async {
            if (canOpenActivity) {
              final activityDetails =
                  await provider.getActivityDetails(activity);
              provider.goToStatistics(activityDetails);
            }
          },
          child: Card(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(borderRadius),
            ),
            elevation: 2,
            margin: const EdgeInsets.all(8),
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: gradientColors,
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(borderRadius),
                boxShadow: [
                  BoxShadow(
                    color: Colors.grey.withOpacity(0.2),
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
                            ActivityUtils.getActivityTypeIcon(activity.type),
                            color: Colors.blueGrey.shade700,
                            size: 60,
                          ),
                        ),
                      ),
                    if (!displayUserName)
                      SizedBox(
                        height: 84,
                        child: VerticalDivider(
                          key: Key(activity.id.toString()),
                          color: Colors.blueGrey.shade400,
                          thickness: 4,
                          width: 4,
                        ),
                      ),
                    if (!displayUserName) const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (displayUserName)
                            ActivityItemUserInformation(activity: activity),
                          ActivityItemDetails(
                              displayUserName: displayUserName,
                              activity: activity,
                              color: titleColor),
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
                if (displayUserName)
                  ActivityItemInteraction(
                    currentActivity: currentActivity,
                  ),
                ],
              ),
            ),
          ),
        );
      }
    });
  }
}
