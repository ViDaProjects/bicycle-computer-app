import '../../../../domain/entities/activity.dart';

class HomeState {
  final int currentIndex;
  final Activity? selectedActivity;

  /// Constructs a `HomeState` object with the provided [currentIndex] and [selectedActivity].
  const HomeState({
    required this.currentIndex,
    this.selectedActivity,
  });

  /// Constructs an initial `HomeState` object with the default [currentIndex].
  factory HomeState.initial() {
    return const HomeState(currentIndex: 0, selectedActivity: null);
  }

  /// Creates a copy of this `HomeState` object with the specified attributes overridden.
  HomeState copyWith({
    int? currentIndex,
    Activity? selectedActivity,
  }) {
    return HomeState(
      currentIndex: currentIndex ?? this.currentIndex,
      selectedActivity: selectedActivity ?? this.selectedActivity,
    );
  }
}
