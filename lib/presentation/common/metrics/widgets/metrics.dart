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
          // Primeira linha: Distância e Velocidade
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
          // Segunda linha: Cadência e Calorias
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
          // Terceira linha: Potência e Altitude
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
    return Row(
      children: [
        Icon(icon, size: 30, color: Colors.blueGrey),
        const SizedBox(width: 8),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(value, style: textStyle),
            Text(label, style: labelStyle),
          ],
        ),
      ],
    );
  }
}
