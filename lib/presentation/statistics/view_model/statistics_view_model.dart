import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../domain/entities/activity.dart';
import 'state/statistics_state.dart';

final statisticsViewModelProvider =
    StateNotifierProvider.autoDispose<StatisticsViewModel, StatisticsState>(
  (ref) => StatisticsViewModel(ref),
);

class StatisticsViewModel extends StateNotifier<StatisticsState> {
  final Ref ref;
  static const platform = MethodChannel('com.beforbike.app/database');

  /// Manages the state and logic of the statistics screen.
  ///
  /// [ref] - The reference to the hooks riverpod container.
  StatisticsViewModel(this.ref) : super(StatisticsState.initial());

  /// Sets the selected activity for detailed statistics.
  void setSelectedActivity(Activity? activity) {
    state = state.copyWith(selectedActivity: activity);
    if (activity != null) {
      loadActivityData(activity.id);
    }
  }

  /// Loads historical data for the selected activity from SQLite database.
  Future<void> loadActivityData(String activityId) async {
    state = state.copyWith(isLoading: true);

    try {
      final List<dynamic> data = await platform.invokeMethod('getActivityData', {
        'activityId': activityId,
      });

      // Convert the data to the expected format
      final activityData = data.map((item) {
        final map = item as Map<dynamic, dynamic>;
        return {
          'timestamp': DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int),
          'speed': (map['speed'] as num).toDouble(),
          'cadence': (map['cadence'] as num).toDouble(),
          'power': (map['power'] as num).toDouble(),
          'altitude': (map['altitude'] as num).toDouble(),
        };
      }).toList();

      state = state.copyWith(isLoading: false, activityData: activityData);
    } on PlatformException catch (e) {
      print("Failed to get activity data: '${e.message}'.");

      // Fallback to mock data if database is not available
      _loadMockData();
    }
  }

  /// Fallback method with mock data
  void _loadMockData() {
    final mockData = [
      {'timestamp': DateTime.now().subtract(const Duration(minutes: 10)), 'speed': 25.0, 'cadence': 90, 'power': 180, 'altitude': 100.0},
      {'timestamp': DateTime.now().subtract(const Duration(minutes: 9)), 'speed': 26.0, 'cadence': 92, 'power': 185, 'altitude': 102.0},
      {'timestamp': DateTime.now().subtract(const Duration(minutes: 8)), 'speed': 24.0, 'cadence': 88, 'power': 175, 'altitude': 98.0},
      {'timestamp': DateTime.now().subtract(const Duration(minutes: 7)), 'speed': 27.0, 'cadence': 95, 'power': 190, 'altitude': 105.0},
      {'timestamp': DateTime.now().subtract(const Duration(minutes: 6)), 'speed': 28.0, 'cadence': 97, 'power': 195, 'altitude': 108.0},
    ];

    state = state.copyWith(isLoading: false, activityData: mockData);
  }

  /// Calculates the maximum value for a given data type.
  double getMaxValue(String dataType) {
    if (state.activityData.isEmpty) return 0.0;

    return state.activityData
        .map((data) => (data[dataType] as num).toDouble())
        .reduce((a, b) => a > b ? a : b);
  }

  /// Calculates the average value for a given data type.
  double getAverageValue(String dataType) {
    if (state.activityData.isEmpty) return 0.0;

    final sum = state.activityData
        .map((data) => (data[dataType] as num).toDouble())
        .reduce((a, b) => a + b);

    return sum / state.activityData.length;
  }
}