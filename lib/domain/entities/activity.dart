import 'package:equatable/equatable.dart';

import 'activity_comment.dart';
import 'enum/activity_type.dart';
import 'location.dart';
import 'user.dart';

/// Represents an activity.
class Activity extends Equatable {
  /// The ID of the activity.
  final String id;

  /// The type of the activity.
  final ActivityType type;

  /// The start datetime of the activity.
  final DateTime startDatetime;

  /// The end datetime of the activity.
  final DateTime endDatetime;

  /// The distance covered in the activity.
  final double distance;

  /// The average speed in the activity.
  final double speed;

  /// The average cadence in the activity.
  final double cadence;

  /// The calories burned in the activity.
  final double calories;

  /// The average power in the activity.
  final double power;

  /// The altitude gain in the activity.
  final double altitude;

  /// The total time of the activity.
  final double time;

  /// The list of locations associated with the activity.
  final Iterable<Location> locations;

  // The user concerned by the activity
  final User user;

  /// The count of likes on the activity
  final double likesCount;

  /// has current user liked ?
  final bool hasCurrentUserLiked;

  /// The list of comments associated with the activity.
  final Iterable<ActivityComment> comments;

  /// Constructs an Activity object with the given parameters.
  const Activity(
      {required this.id,
      required this.type,
      required this.startDatetime,
      required this.endDatetime,
      required this.distance,
      required this.speed,
      required this.cadence,
      required this.calories,
      required this.power,
      required this.altitude,
      required this.time,
      required this.locations,
      required this.user,
      required this.likesCount,
      required this.hasCurrentUserLiked,
      required this.comments});

  Activity copy(
      {ActivityType? type,
      double? likesCount,
      bool? hasCurrentUserLiked,
      Iterable<ActivityComment>? comments}) {
    return Activity(
        id: id,
        type: type ?? this.type,
        startDatetime: startDatetime,
        endDatetime: endDatetime,
        distance: distance,
        speed: speed,
        cadence: cadence,
        calories: calories,
        power: power,
        altitude: altitude,
        time: time,
        locations: locations,
        user: user,
        likesCount: likesCount ?? this.likesCount,
        hasCurrentUserLiked: hasCurrentUserLiked ?? this.hasCurrentUserLiked,
        comments: comments ?? this.comments);
  }

  @override
  List<Object?> get props => [
        id,
        type,
        startDatetime,
        endDatetime,
        distance,
        speed,
        cadence,
        calories,
        power,
        altitude,
        time,
        ...locations,
        user,
        likesCount,
        hasCurrentUserLiked,
        ...comments
      ];
}
