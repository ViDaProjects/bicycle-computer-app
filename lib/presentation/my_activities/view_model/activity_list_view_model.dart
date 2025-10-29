import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../data/repositories/activity_repository_impl.dart';
import '../../../domain/entities/activity.dart';
import '../../../domain/entities/page.dart';
import '../../../main.dart';
import '../screens/activity_details_screen.dart';
import 'state/activity_list_state.dart';

/// The provider for the activity list view model.
final activityListViewModelProvider =
    StateNotifierProvider.autoDispose<ActivityListViewModel, ActivityListState>(
        (ref) => ActivityListViewModel(ref));

/// The view model for the activity list screen.
class ActivityListViewModel extends StateNotifier<ActivityListState> {
  late final Ref ref;

  ActivityListViewModel(this.ref) : super(ActivityListState.initial());

  /// Fetches the list of activities.
  Future<EntityPage<Activity>> fetchActivities({int pageNumber = 0}) async {
    try {
      final newActivities = await ref
          .read(activityRepositoryProvider)
          .getActivities(pageNumber: pageNumber);
      return newActivities;
    } catch (error) {
      return EntityPage(list: List.empty(), total: 0);
    }
  }

  /// Retrieves the details of an activity.
  Future<Activity> getActivityDetails(Activity activity) async {
    state = state.copyWith(isLoading: true);

    try {
      final activityDetails = await ref
          .read(activityRepositoryProvider)
          .getActivityById(id: activity.id);
      state = state.copyWith(isLoading: false);
      return activityDetails;
    } catch (error) {
      // Handle error
      state = state.copyWith(isLoading: false);
      rethrow;
    }
  }

  /// Navigates back to the home screen.
  void backToHome() {
    navigatorKey.currentState?.pop();
  }

  /// Navigates to the activity details screen.
  void goToActivity(Activity activityDetails) {
    navigatorKey.currentState?.push(
      PageRouteBuilder(
        transitionDuration: const Duration(milliseconds: 500),
        pageBuilder: (context, animation, secondaryAnimation) =>
            SlideTransition(
          position: Tween<Offset>(
            begin: const Offset(1.0, 0.0),
            end: Offset.zero,
          ).animate(animation),
          child: ActivityDetailsScreen(activity: activityDetails),
        ),
      ),
    );
  }

  void setIsLoading(bool isLoading) {
    state = state.copyWith(isLoading: isLoading);
  }

  /// Toggles selection mode on/off
  void toggleSelectionMode() {
    state = state.copyWith(
      isSelectionMode: !state.isSelectionMode,
      selectedActivities: state.isSelectionMode ? {} : state.selectedActivities,
    );
  }

  /// Toggles selection of a specific activity
  void toggleActivitySelection(String activityId) {
    final newSelected = Set<String>.from(state.selectedActivities);
    if (newSelected.contains(activityId)) {
      newSelected.remove(activityId);
    } else {
      newSelected.add(activityId);
    }
    state = state.copyWith(selectedActivities: newSelected);
  }

  /// Deletes selected activities
  Future<void> deleteSelectedActivities() async {
    if (state.selectedActivities.isEmpty) return;

    state = state.copyWith(isLoading: true);
    try {
      final platform = const MethodChannel('com.beforbike.app/database');
      for (final activityId in state.selectedActivities) {
        await platform.invokeMethod('deleteActivity', {'activityId': activityId});
      }
      state = state.copyWith(
        selectedActivities: {},
        isSelectionMode: false,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(isLoading: false);
      rethrow;
    }
  }

  /// Clears all selections
  void clearSelection() {
    state = state.copyWith(
      selectedActivities: {},
      isSelectionMode: false,
    );
  }
}
