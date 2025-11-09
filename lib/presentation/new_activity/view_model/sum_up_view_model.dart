import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../main.dart';
import '../../common/location/view_model/location_view_model.dart';
import '../../common/metrics/view_model/metrics_view_model.dart';
import '../../common/timer/viewmodel/timer_view_model.dart';
import 'state/sum_up_state.dart';

/// Provides the instance of [SumUpViewModel].
final sumUpViewModel = Provider.autoDispose((ref) {
  return SumUpViewModel();
});

/// Provides the state management for the SumUpScreen.
final sumUpViewModelProvider =
    NotifierProvider.autoDispose<SumUpViewModel, SumUpState>(
  () => SumUpViewModel(),
);

/// Represents the view model for the SumUpScreen.
class SumUpViewModel extends Notifier<SumUpState> {
  @override
  SumUpState build() {
    return SumUpState.initial();
  }

  /// Finalizes the activity (data is saved by Android via BLE during the ride).
  void save() async {
    state = state.copyWith(isSaving: true);

    // Note: Flutter doesn't save activities - data is saved by Android via BLE
    // This method just finalizes the UI state

    // Reset all providers
    ref.read(timerViewModelProvider.notifier).resetTimer();
    ref.read(locationViewModelProvider.notifier).resetSavedPositions();
    ref.read(metricsViewModelProvider.notifier).reset();
    ref.read(locationViewModelProvider.notifier).startGettingLocation();

    state = state.copyWith(isSaving: false);
    navigatorKey.currentState?.pop();
  }
}
