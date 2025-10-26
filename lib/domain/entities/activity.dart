import 'package:equatable/equatable.dart';

import 'location.dart';

/// Represents an activity.
class Activity extends Equatable {
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

  /// Constructs an Activity object with the given parameters.
  const Activity(
      {required this.id,
      required this.startDatetime,
      required this.endDatetime,
      required this.distance,
      required this.speed,
      required this.cadence,
      required this.calories,
      required this.power,
      required this.altitude,
      required this.time,
      required this.locations});

  Activity copy() {
    return Activity(
        id: id,
        startDatetime: startDatetime,
        endDatetime: endDatetime,
        distance: distance,
        speed: speed,
        cadence: cadence,
        calories: calories,
        power: power,
        altitude: altitude,
        time: time,
        locations: locations);
  }

  @override
  List<Object?> get props => [
        id,
        startDatetime,
        endDatetime,
        distance,
        speed,
        cadence,
        calories,
        power,
        altitude,
        time,
        ...locations
      ];
}
