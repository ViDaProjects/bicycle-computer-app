import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import 'state/activity_list_state.dart';

/// Provider for the activity list view model.
final activityListWidgetViewModelProvider = NotifierProvider.family<
    ActivityListWidgetViewModel,
    ActivityListWidgetState,
    String>((listId) => ActivityListWidgetViewModel(listId));

/// View model for the activity item widget.
class ActivityListWidgetViewModel
    extends Notifier<ActivityListWidgetState> {
  final String listId;
  final ScrollController scrollController = ScrollController();

  ActivityListWidgetViewModel(this.listId);

  @override
  ActivityListWidgetState build() {
    return ActivityListWidgetState.initial();
  }

  int calculateTotalElements(List<dynamic> listOfLists) {
    int totalElements =
        listOfLists.fold(0, (sum, list) => sum + (list.length as int));

    return totalElements;
  }

  bool hasMoreData(List<dynamic> list, int total) {
    return calculateTotalElements(list) < total;
  }
}
