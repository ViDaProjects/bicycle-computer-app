/// The state class for the activity list.
class ActivityListState {
  final bool isLoading; // Indicates if the list is currently loading
  final Set<String> selectedActivities; // IDs of selected activities for deletion
  final bool isSelectionMode; // Whether we're in selection mode

  const ActivityListState({
    required this.isLoading,
    required this.selectedActivities,
    required this.isSelectionMode,
  });

  /// Factory method to create the initial state.
  factory ActivityListState.initial() {
    return const ActivityListState(
      isLoading: false,
      selectedActivities: {},
      isSelectionMode: false,
    );
  }

  /// Method to create a copy of the state with updated values.
  ActivityListState copyWith({
    bool? isLoading, // Updated loading state
    Set<String>? selectedActivities,
    bool? isSelectionMode,
  }) {
    return ActivityListState(
      isLoading: isLoading ?? this.isLoading,
      selectedActivities: selectedActivities ?? this.selectedActivities,
      isSelectionMode: isSelectionMode ?? this.isSelectionMode,
    );
  }
}
