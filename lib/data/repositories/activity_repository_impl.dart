import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../domain/entities/activity.dart';
import '../../domain/entities/activity_comment.dart';
import '../../domain/entities/enum/activity_type.dart';
import '../../domain/entities/location.dart';
import '../../domain/entities/page.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/activity_repository.dart';
import '../api/activity_api.dart';
import '../model/request/activity_request.dart';

/// Provider for the ActivityRepository implementation.
final activityRepositoryProvider =
    Provider<ActivityRepository>((ref) => ActivityRepositoryImpl());

/// Implementation of the ActivityRepository.
class ActivityRepositoryImpl extends ActivityRepository {
  static const platform = MethodChannel('com.beforbike.app/database');

  ActivityRepositoryImpl();

  @override
  Future<EntityPage<Activity>> getActivities({int pageNumber = 0}) async {
    try {
      // Try to get activities from local database first
      final List<dynamic> activitiesData = await platform.invokeMethod('getAllActivities');

      if (activitiesData.isNotEmpty) {
        List<Activity> activities = [];
        for (var activityMap in activitiesData) {
          final activity = await _mapDatabaseActivityToEntity(activityMap as Map<dynamic, dynamic>);
          activities.add(activity);
        }

        return EntityPage(list: activities, total: activities.length);
      }
    } catch (e) {
      // Fall back to API if database fails
    }

    // Fallback to API
    final activityResponses = await ActivityApi.getActivities(pageNumber);
    List<Activity> activities =
        activityResponses.list.map((response) => response.toEntity()).toList();
    return EntityPage(list: activities, total: activityResponses.total);
  }

  @override
  Future<EntityPage<Activity>> getMyAndMyFriendsActivities(
      {int pageNumber = 0}) async {
    // For now, return the same as getActivities since we don't have user filtering in local DB
    return getActivities(pageNumber: pageNumber);
  }

  @override
  Future<EntityPage<Activity>> getUserActivities(String userId,
      {int pageNumber = 0}) async {
    // For now, return the same as getActivities since we don't have user filtering in local DB
    return getActivities(pageNumber: pageNumber);
  }

  @override
  Future<Activity> getActivityById({required String id}) async {
    try {
      // Try to get activity from local database first
      final List<dynamic> activitiesData = await platform.invokeMethod('getAllActivities');
      final activityMap = activitiesData.firstWhere(
        (activity) => activity['id'] == id,
        orElse: () => null,
      );

      if (activityMap != null) {
        return await _mapDatabaseActivityToEntity(activityMap as Map<dynamic, dynamic>);
      }
    } catch (e) {
      // Fall back to API if database fails
    }

    // Fallback to API
    final activityResponse = await ActivityApi.getActivityById(id);
    return activityResponse.toEntity();
  }

  Future<Activity> _mapDatabaseActivityToEntity(Map<dynamic, dynamic> activityMap) async {
    // Get locations for this activity
    final locations = await _getLocationsForActivity(activityMap['id'] as String);

    return Activity(
      id: activityMap['id'] as String,
      type: ActivityType.values.firstWhere(
        (type) => type.toString().split('.').last == activityMap['type'],
        orElse: () => ActivityType.cycling,
      ),
      startDatetime: DateTime.fromMillisecondsSinceEpoch(activityMap['startDatetime'] as int),
      endDatetime: DateTime.fromMillisecondsSinceEpoch(activityMap['endDatetime'] as int),
      distance: (activityMap['distance'] as num).toDouble(),
      speed: (activityMap['speed'] as num).toDouble(),
      cadence: (activityMap['cadence'] as num).toDouble(),
      calories: (activityMap['calories'] as num).toDouble(),
      power: (activityMap['power'] as num).toDouble(),
      altitude: (activityMap['altitude'] as num).toDouble(),
      time: (activityMap['time'] as num).toDouble(),
      locations: locations,
      user: User(
        id: activityMap['userId'] as String,
        username: activityMap['userName'] as String,
        firstname: '', // Not stored in our simple DB
        lastname: '', // Not stored in our simple DB
      ),
      likesCount: (activityMap['likesCount'] as num).toDouble(),
      hasCurrentUserLiked: activityMap['hasCurrentUserLiked'] as bool,
      comments: const [], // Not implemented in our simple DB
    );
  }

  Future<List<Location>> _getLocationsForActivity(String activityId) async {
    try {
      final List<dynamic> locationsData = await platform.invokeMethod('getActivityLocations', {'activityId': activityId});
      
      return locationsData.map((locationMap) {
        return Location(
          id: locationMap['id'] as String,
          datetime: DateTime.fromMillisecondsSinceEpoch(locationMap['datetime'] as int),
          latitude: (locationMap['latitude'] as num).toDouble(),
          longitude: (locationMap['longitude'] as num).toDouble(),
        );
      }).toList();
    } catch (e) {
      return [];
    }
  }

  @override
  Future<String?> removeActivity({required String id}) async {
    // Not implemented for local database
    throw UnimplementedError('Remove activity not implemented for local database');
  }

  @override
  Future<Activity?> addActivity(ActivityRequest request) async {
    // Not implemented for local database
    throw UnimplementedError('Add activity not implemented for local database');
  }

  @override
  Future<Activity> editActivity(ActivityRequest request) async {
    // Not implemented for local database
    throw UnimplementedError('Edit activity not implemented for local database');
  }

  @override
  Future<void> like(String id) async {
    // Not implemented for local database
    throw UnimplementedError('Like activity not implemented for local database');
  }

  @override
  Future<void> dislike(String id) async {
    // Not implemented for local database
    throw UnimplementedError('Dislike activity not implemented for local database');
  }

  @override
  Future<ActivityComment?> createComment(String activityId, String comment) async {
    // Not implemented for local database
    throw UnimplementedError('Create comment not implemented for local database');
  }

  @override
  Future<ActivityComment> editComment(String id, String comment) async {
    // Not implemented for local database
    throw UnimplementedError('Edit comment not implemented for local database');
  }

  @override
  Future<String?> removeComment({required String id}) async {
    // Not implemented for local database
    throw UnimplementedError('Remove comment not implemented for local database');
  }
}
