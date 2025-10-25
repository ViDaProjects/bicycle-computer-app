import 'package:equatable/equatable.dart';

import '../../../domain/entities/activity.dart';
import '../../../domain/entities/activity_comment.dart';
import '../../../domain/entities/enum/activity_type.dart';
import '../../../domain/entities/location.dart';
import '../../../domain/entities/user.dart';
import 'activity_comment_response.dart';
import 'location_response.dart';
import 'user_response.dart';

/// Represents a response object for an activity.
class ActivityResponse extends Equatable {
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

  /// The total time of the activity.
  final double time;

  /// The list of locations associated with the activity.
  final Iterable<LocationResponse> locations;

  /// The user concerned by the activity
  final UserResponse user;

  /// The count of likes on the activity
  final double likesCount;

  /// has current user liked ?
  final bool hasCurrentUserLiked;

  /// The average cadence of the activity.
  final double cadence;

  /// The calories burned during the activity.
  final double calories;

  /// The average power output during the activity.
  final double power;

  /// The altitude gain during the activity.
  final double altitude;

  /// The list of comments
  final Iterable<ActivityCommentResponse> comments;

  /// Constructs an ActivityResponse object with the given parameters.
  const ActivityResponse(
      {required this.id,
      required this.type,
      required this.startDatetime,
      required this.endDatetime,
      required this.distance,
      required this.speed,
      required this.time,
      required this.cadence,
      required this.calories,
      required this.power,
      required this.altitude,
      required this.locations,
      required this.user,
      required this.likesCount,
      required this.hasCurrentUserLiked,
      required this.comments});

  @override
  List<Object?> get props => [
        id,
        type,
        startDatetime,
        endDatetime,
        distance,
        speed,
        time,
        cadence,
        calories,
        power,
        altitude,
        ...locations,
        user,
        likesCount,
        hasCurrentUserLiked,
        ...comments
      ];

  /// Creates an ActivityResponse object from a JSON map.
  factory ActivityResponse.fromMap(Map<String, dynamic> map) {
    final activityTypeString = map['type']?.toString().toLowerCase();
    final activityType = ActivityType.values.firstWhere(
      (type) => type.name.toLowerCase() == activityTypeString,
      orElse: () => ActivityType.cycling,
    );

    return ActivityResponse(
      id: map['id'].toString(),
      type: activityType,
      startDatetime: DateTime.parse(map['startDatetime']),
      endDatetime: DateTime.parse(map['endDatetime']),
      distance: map['distance'].toDouble(),
      speed: map['speed'] is String
          ? double.parse(map['speed'])
          : map['speed'].toDouble(),
      time: map['time'].toDouble(),
      cadence: map['cadence'] is String
          ? double.parse(map['cadence'])
          : map['cadence'].toDouble(),
      calories: map['calories'] is String
          ? double.parse(map['calories'])
          : map['calories'].toDouble(),
      power: map['power'] is String
          ? double.parse(map['power'])
          : map['power'].toDouble(),
      altitude: map['altitude'] is String
          ? double.parse(map['altitude'])
          : map['altitude'].toDouble(),
      likesCount: map['likesCount'].toDouble(),
      hasCurrentUserLiked: map['hasCurrentUserLiked'],
      locations: (map['locations'] as List<dynamic>)
          .map<LocationResponse>((item) => LocationResponse.fromMap(item))
          .toList(),
      user: UserResponse.fromMap(
        map['user'],
      ),
      comments: (map['comments'] as List<dynamic>)
          .map<ActivityCommentResponse>(
              (item) => ActivityCommentResponse.fromMap(item))
          .toList(),
    );
  }

  /// Converts the ActivityResponse object to an Activity entity.
  Activity toEntity() {
    final activityLocations = locations.map<Location>((location) {
      return Location(
        id: location.id,
        datetime: location.datetime,
        latitude: location.latitude,
        longitude: location.longitude,
      );
    }).toList()
      ..sort((a, b) => a.datetime.compareTo(b.datetime));

    final activityComments = comments.map<ActivityComment>((comment) {
      return ActivityComment(
        id: comment.id,
        createdAt: comment.createdAt,
        user: comment.user.toEntity(),
        content: comment.content,
      );
    }).toList()
      ..sort((a, b) => a.createdAt.compareTo(b.createdAt));

    return Activity(
        id: id,
        type: type,
        startDatetime: startDatetime,
        endDatetime: endDatetime,
        distance: distance,
        speed: speed,
        time: time,
        cadence: cadence,
        calories: calories,
        power: power,
        altitude: altitude,
        locations: activityLocations,
        likesCount: likesCount,
        hasCurrentUserLiked: hasCurrentUserLiked,
        user: User(
            id: user.id,
            username: user.username,
            firstname: user.firstname,
            lastname: user.lastname),
        comments: activityComments);
  }
}
