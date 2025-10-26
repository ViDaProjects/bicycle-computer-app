import 'package:equatable/equatable.dart';

import '../../../domain/entities/activity.dart';
import '../../../domain/entities/location.dart';
import 'location_response.dart';

/// Represents a response object for an activity.
class ActivityResponse extends Equatable {
  /// The ID of the activity.
  final String id;

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

  /// The average cadence of the activity.
  final double cadence;

  /// The calories burned during the activity.
  final double calories;

  /// The average power output during the activity.
  final double power;

  /// The altitude gain during the activity.
  final double altitude;

  /// Constructs an ActivityResponse object with the given parameters.
  const ActivityResponse(
      {required this.id,
      required this.startDatetime,
      required this.endDatetime,
      required this.distance,
      required this.speed,
      required this.time,
      required this.cadence,
      required this.calories,
      required this.power,
      required this.altitude,
      required this.locations});

  @override
  List<Object?> get props => [
        id,
        startDatetime,
        endDatetime,
        distance,
        speed,
        time,
        cadence,
        calories,
        power,
        altitude,
        ...locations
      ];

  /// Creates an ActivityResponse object from a JSON map.
  factory ActivityResponse.fromMap(Map<String, dynamic> map) {
    return ActivityResponse(
      id: map['id'].toString(),
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
      locations: (map['locations'] as List<dynamic>)
          .map<LocationResponse>((item) => LocationResponse.fromMap(item))
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

    return Activity(
        id: id,
        startDatetime: startDatetime,
        endDatetime: endDatetime,
        distance: distance,
        speed: speed,
        time: time,
        cadence: cadence,
        calories: calories,
        power: power,
        altitude: altitude,
        locations: activityLocations);
  }
}
