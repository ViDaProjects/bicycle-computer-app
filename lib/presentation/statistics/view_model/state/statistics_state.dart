import '../../../../domain/entities/activity.dart';

class StatisticsState {
  final bool isLoading;
  final Activity? selectedActivity;
  final List<Map<String, dynamic>> activityData;

  /// Represents the state of the statistics screen.
  ///
  /// [isLoading] indicates whether the screen is in a loading state.
  /// [selectedActivity] the currently selected activity for detailed statistics.
  /// [activityData] historical data points for the selected activity.
  const StatisticsState({
    required this.isLoading,
    this.selectedActivity,
    required this.activityData,
  });

  /// Creates an initial state for the statistics screen.
  factory StatisticsState.initial() {
    return const StatisticsState(
      isLoading: false,
      selectedActivity: null,
      activityData: [],
    );
  }

  /// Creates a copy of this state object with the provided changes.
  StatisticsState copyWith({
    bool? isLoading,
    Activity? selectedActivity,
    List<Map<String, dynamic>>? activityData,
  }) {
    return StatisticsState(
      isLoading: isLoading ?? this.isLoading,
      selectedActivity: selectedActivity ?? this.selectedActivity,
      activityData: activityData ?? this.activityData,
    );
  }
}