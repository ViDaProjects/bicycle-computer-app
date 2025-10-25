import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../view_model/metrics_view_model.dart';

/// A widget that displays the metrics information such as speed and distance.
class Metrics extends HookConsumerWidget {
  final double? speed;
  final double? distance;
  final double? cadence;
  final double? calories;
  final double? power;
  final double? altitude;

  /// Creates a Metrics widget.
  const Metrics({super.key, this.speed, this.distance, this.cadence, this.calories, this.power, this.altitude});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(metricsViewModelProvider);
    const textStyle = TextStyle(fontSize: 20.0, fontWeight: FontWeight.bold);
    const labelStyle = TextStyle(fontSize: 12.0, color: Colors.grey);

    double speedToDisplay = state.globalSpeed;
    double distanceToDisplay = state.distance;
    double cadenceToDisplay = 0;
    double caloriesToDisplay = 0;
    double powerToDisplay = 0;
    double altitudeToDisplay = 0;

    if (speed != null) {
      speedToDisplay = speed!;
    }
    if (distance != null) {
      distanceToDisplay = distance!;
    }
    if (cadence != null) {
      cadenceToDisplay = cadence!;
    }
    if (calories != null) {
      caloriesToDisplay = calories!;
    }
    if (power != null) {
      powerToDisplay = power!;
    }
    if (altitude != null) {
      altitudeToDisplay = altitude!;
    }

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0),
      child: Column(
        children: [
          // First row: Distance and Speed
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildMetricItem(
                icon: Icons.location_on,
                value: distanceToDisplay.toStringAsFixed(2),
                label: 'km',
                textStyle: textStyle,
                labelStyle: labelStyle,
              ),
              _buildMetricItem(
                icon: Icons.speed,
                value: speedToDisplay.toStringAsFixed(2),
                label: 'km/h',
                textStyle: textStyle,
                labelStyle: labelStyle,
              ),
            ],
          ),
          const SizedBox(height: 20),
          // Second row: Cadence and Calories
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildMetricItem(
                icon: Icons.repeat,
                value: cadenceToDisplay.toStringAsFixed(1),
                label: 'rpm',
                textStyle: textStyle,
                labelStyle: labelStyle,
              ),
              _buildMetricItem(
                icon: Icons.local_fire_department,
                value: caloriesToDisplay.toStringAsFixed(0),
                label: 'cal',
                textStyle: textStyle,
                labelStyle: labelStyle,
              ),
            ],
          ),
          const SizedBox(height: 20),
          // Third row: Power and Altitude
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildMetricItem(
                icon: Icons.flash_on,
                value: powerToDisplay.toStringAsFixed(0),
                label: 'W',
                textStyle: textStyle,
                labelStyle: labelStyle,
              ),
              _buildMetricItem(
                icon: Icons.terrain,
                value: altitudeToDisplay.toStringAsFixed(0),
                label: 'm',
                textStyle: textStyle,
                labelStyle: labelStyle,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMetricItem({
    required IconData icon,
    required String value,
    required String label,
    required TextStyle textStyle,
    required TextStyle labelStyle,
  }) {
    return Container(
      width: 140,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
        border: Border.all(
          color: Colors.grey.withValues(alpha: 0.2),
          width: 1,
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(
              icon,
              size: 24,
              color: Colors.blue.shade600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: textStyle.copyWith(
              color: Colors.black87,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          Text(
            label,
            style: labelStyle.copyWith(
              color: Colors.grey.shade600,
              fontSize: 11,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
