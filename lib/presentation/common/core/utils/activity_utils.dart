import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../../core/utils/storage_utils.dart';
import '../../../../domain/entities/activity.dart';
import '../../../../domain/entities/user.dart';
import '../enums/infinite_scroll_list.enum.dart';
import '../widgets/view_model/infinite_scroll_list_view_model.dart';

enum ActivityUpdateActionEnum { add, edit, remove }

/// Utility class for activity-related operations.
class ActivityUtils {

  static List<List<Activity>> replaceActivity(
      List<List<Activity>> activities, Activity updatedActivity) {
    return activities.map((innerList) {
      return innerList.map((activity) {
        if (activity.id == updatedActivity.id) {
          return updatedActivity;
        } else {
          return activity;
        }
      }).toList();
    }).toList();
  }

  static List<List<Activity>> deleteActivity(
      List<List<Activity>> activities, Activity activityToDelete) {
    var newData = activities.map((innerList) {
      return innerList
          .where((activity) => activity.id != activityToDelete.id)
          .toList();
    }).toList();

    return newData
        .where((innerList) =>
            innerList.any((activity) => activity.id != activityToDelete.id))
        .toList();
  }

  static List<List<Activity>> prependActivity(
      List<List<Activity>> activities, Activity activity) {
    bool addNewGroup = !(activity.startDatetime.month ==
            activities.first.first.startDatetime.month &&
        activity.startDatetime.year ==
            activities.first.first.startDatetime.year);

    if (addNewGroup) {
      activities.insert(0, [activity]);
    } else {
      activities.first.insert(0, activity);
    }

    return activities;
  }

  static Future<void> updateActivity(Ref ref, Activity updatedActivity,
      ActivityUpdateActionEnum action) async {
    await _updateActivityList(
        ref,
        InfiniteScrollListEnum.myActivities.toString(),
        updatedActivity,
        action);
    await _updateActivityList(ref, InfiniteScrollListEnum.community.toString(),
        updatedActivity, action);

    User? currentUser = await StorageUtils.getUser();
    if (currentUser != null) {
      await _updateActivityList(
          ref,
          '${InfiniteScrollListEnum.profile}_${currentUser.id}',
          updatedActivity,
          action);
    }
  }

  static Future<void> _updateActivityList(Ref ref, String listType,
      Activity updatedActivity, ActivityUpdateActionEnum action) async {
    var data = ref
        .read(infiniteScrollListViewModelProvider(listType))
        .data
        .cast<List<Activity>>();

    if (data.isNotEmpty) {
      List<List<Activity>> newData = [];

      if (action == ActivityUpdateActionEnum.edit) {
        newData = ActivityUtils.replaceActivity(data, updatedActivity);
      } else if (action == ActivityUpdateActionEnum.remove) {
        newData = ActivityUtils.deleteActivity(data, updatedActivity);
      } else if (action == ActivityUpdateActionEnum.add) {
        newData = ActivityUtils.prependActivity(data, updatedActivity);
      }

      ref
          .read(infiniteScrollListViewModelProvider(listType).notifier)
          .replaceData(newData);
    }
  }
}
