import '../../data/model/request/activity_request.dart';
import '../entities/activity.dart';
import '../entities/page.dart';

/// Abstract class representing the activity repository.
abstract class ActivityRepository {
  /// Retrieves a page of activities.
  Future<EntityPage<Activity>> getActivities({int pageNumber});

  /// Retrieves a page of my activities and my friends.
  Future<EntityPage<Activity>> getMyAndMyFriendsActivities({int pageNumber});

  /// Retrieves a page of a user activities.
  Future<EntityPage<Activity>> getUserActivities(String userId,
      {int pageNumber});

  /// Retrieves an activity by its ID.
  Future<Activity> getActivityById({required String id});

  /// Removes an activity by its ID.
  Future<String?> removeActivity({required String id});

  /// Adds a new activity.
  Future<Activity?> addActivity(ActivityRequest request);

  /// Edits an existing activity.
  Future<Activity> editActivity(ActivityRequest request);
}
