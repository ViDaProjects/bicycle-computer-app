import 'package:equatable/equatable.dart';

import 'location_request.dart';

/// Represents a request object for creating or updating an activity.
class ActivityRequest extends Equatable {
  /// The id of the activity.
  final String? id;

  /// The start datetime of the activity.
  final DateTime startDatetime;

  /// The end datetime of the activity.
  final DateTime endDatetime;

  /// The distance of the activity.
  final double distance;

  /// The list of locations associated with the activity.
  final List<LocationRequest> locations;

  /// Constructs an ActivityRequest object with the given parameters.
  const ActivityRequest({
    this.id,
    required this.startDatetime,
    required this.endDatetime,
    required this.distance,
    required this.locations,
  });

  @override
  List<Object?> get props =>
      [id, startDatetime, endDatetime, distance, locations];

  /// Converts the ActivityRequest object to a JSON map.
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'startDatetime': startDatetime.toIso8601String(),
      'endDatetime': endDatetime.toIso8601String(),
      'distance': distance,
      'locations': locations.map((location) => location.toMap()).toList(),
    };
  }

  @override
  bool get stringify => true;
}
