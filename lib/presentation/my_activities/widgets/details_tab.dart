import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/activity.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/metrics/widgets/metrics.dart';
import '../../common/timer/widgets/timer_text.dart';
import '../view_model/activity_details_view_model.dart';

/// The tab that displays details of a specific activity.
class DetailsTab extends HookConsumerWidget {
  final Activity activity;

  const DetailsTab({super.key, required this.activity});

  Widget _buildDateTimeInfo(Activity activity) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _buildDateTimeItem(
            icon: Icons.play_arrow,
            label: 'Start',
            dateTime: activity.startDatetime,
          ),
          _buildDateTimeItem(
            icon: Icons.stop,
            label: 'End',
            dateTime: activity.endDatetime,
          ),
        ],
      ),
    );
  }

  Widget _buildDateTimeItem({
    required IconData icon,
    required String label,
    required DateTime dateTime,
  }) {
    final timeFormat = DateFormat('HH:mm:ss');
    final dateFormat = DateFormat('dd/MM/yyyy');

    return Column(
      children: [
        Icon(icon, color: Colors.blueGrey, size: 24),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: Colors.grey,
            fontWeight: FontWeight.w500,
          ),
        ),
        Text(
          timeFormat.format(dateTime),
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
        Text(
          dateFormat.format(dateTime),
          style: const TextStyle(
            fontSize: 12,
            color: Colors.grey,
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(activityDetailsViewModelProvider);

    final displayedActivity = state.activity ?? activity;

    return Scaffold(
      body: Column(
        children: [
          const SizedBox(height: 10),
          const SizedBox(height: 10),
          _buildDateTimeInfo(displayedActivity),
          const SizedBox(height: 20),
          Center(
            child: TimerText(timeInMs: displayedActivity.time.toInt()),
          ),
          const SizedBox(height: 15),
          Metrics(
            distance: displayedActivity.distance,
            speed: displayedActivity.speed,
            cadence: displayedActivity.cadence,
            calories: displayedActivity.calories,
            power: displayedActivity.power,
            altitude: displayedActivity.altitude,
          ),
          const SizedBox(height: 20),
          // Activity summary information
          Container(
            padding: const EdgeInsets.all(16),
            margin: const EdgeInsets.symmetric(horizontal: 16),
            decoration: BoxDecoration(
              color: ColorUtils.white,
              borderRadius: BorderRadius.circular(12),
              boxShadow: [
                BoxShadow(
                  color: Colors.grey.withValues(alpha: 0.1),
                  spreadRadius: 1,
                  blurRadius: 5,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Activity Summary',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: ColorUtils.blueGrey,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  'Distance: ${displayedActivity.distance.toStringAsFixed(2)} km',
                  style: const TextStyle(fontSize: 16),
                ),
                const SizedBox(height: 8),
                Text(
                  'Duration: ${displayedActivity.time ~/ 3600}:${((displayedActivity.time % 3600) ~/ 60).toString().padLeft(2, '0')}:${(displayedActivity.time % 60).toString().padLeft(2, '0')}',
                  style: const TextStyle(fontSize: 16),
                ),
              ],
            ),
          ),
        ],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }
}
