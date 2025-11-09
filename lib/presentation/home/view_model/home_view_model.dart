import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../domain/entities/activity.dart';
import 'state/home_state.dart';

final homeViewModelProvider =
    NotifierProvider.autoDispose<HomeViewModel, HomeState>(
  () => HomeViewModel(),
);

class HomeViewModel extends Notifier<HomeState> {
  @override
  HomeState build() {
    return HomeState.initial();
  }

  /// Returns the current index value from the state.
  int getCurrentIndex() {
    return state.currentIndex;
  }

  /// Sets the current index value to the specified [index].
  void setCurrentIndex(int index) {
    state = state.copyWith(currentIndex: index);
  }

  /// Sets the selected activity.
  void setSelectedActivity(Activity? activity) {
    state = state.copyWith(selectedActivity: activity);
  }
}
