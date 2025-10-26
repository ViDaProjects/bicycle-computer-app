import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/activity.dart';
import '../../common/core/utils/color_utils.dart';
import '../view_model/statistics_view_model.dart';

/// The statistics screen that displays activity statistics and charts.
class StatisticsScreen extends HookConsumerWidget {
  final Activity? selectedActivity;

  const StatisticsScreen({super.key, this.selectedActivity});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(statisticsViewModelProvider);
    final provider = ref.watch(statisticsViewModelProvider.notifier);

    // Update selected activity when this screen is built
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (selectedActivity != state.selectedActivity) {
        provider.setSelectedActivity(selectedActivity);
      }
    });

    return Scaffold(
      body: SafeArea(
        child: state.isLoading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 20),
                    if (selectedActivity != null) ...[
                      // Speed Chart
                      _buildChartCard('Speed (km/h)', 'speed', state.activityData),
                      const SizedBox(height: 20),

                      // Cadence Chart
                      _buildChartCard('Cadence (rpm)', 'cadence', state.activityData),
                      const SizedBox(height: 20),

                      // Power Chart
                      _buildChartCard('Power (watts)', 'power', state.activityData),
                      const SizedBox(height: 20),

                      // Altitude Chart
                      _buildChartCard('Altitude (m)', 'altitude', state.activityData),
                      const SizedBox(height: 20),

                      // Statistics Cards
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard('Speed', 'Max: ${provider.getMaxValue('speed').toStringAsFixed(1)} km/h\nAvg: ${provider.getAverageValue('speed').toStringAsFixed(1)} km/h', Icons.speed),
                          _buildStatCard('Cadence', 'Max: ${provider.getMaxValue('cadence').toStringAsFixed(0)} rpm\nAvg: ${provider.getAverageValue('cadence').toStringAsFixed(0)} rpm', Icons.pedal_bike),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard('Power', 'Max: ${provider.getMaxValue('power').toStringAsFixed(0)} W\nAvg: ${provider.getAverageValue('power').toStringAsFixed(0)} W', Icons.flash_on),
                          _buildStatCard('Altitude', 'Max: ${provider.getMaxValue('altitude').toStringAsFixed(0)} m\nAvg: ${provider.getAverageValue('altitude').toStringAsFixed(0)} m', Icons.terrain),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard('Distance', '${(selectedActivity!.distance / 1000).toStringAsFixed(2)} km', Icons.straighten),
                          _buildStatCard('Calories', '${selectedActivity!.calories.toStringAsFixed(0)} kcal', Icons.local_fire_department),
                        ],
                      ),
                    ] else ...[
                      // General statistics when no activity is selected
                      Container(
                        height: 200,
                        padding: const EdgeInsets.all(16),
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
                        child: const Center(
                          child: Text('Select an activity from the list to view detailed statistics'),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
      ),
    );
  }

  Widget _buildChartCard(String title, String dataType, List<Map<String, dynamic>> data) {
    if (data.isEmpty) {
      return Container(
        height: 200,
        padding: const EdgeInsets.all(16),
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
        child: const Center(
          child: Text('No data available'),
        ),
      );
    }

    final spots = data.map((entry) {
      final timestamp = (entry['timestamp'] as DateTime).millisecondsSinceEpoch.toDouble();
      final value = (entry[dataType] as num?)?.toDouble() ?? 0.0;
      return FlSpot(timestamp, value);
    }).toList();

    // Find min and max timestamps for proper scaling
    final timestamps = data.map((entry) => (entry['timestamp'] as DateTime).millisecondsSinceEpoch.toDouble()).toList();
    final minX = timestamps.isNotEmpty ? timestamps.reduce((a, b) => a < b ? a : b) : 0.0;
    final maxX = timestamps.isNotEmpty ? timestamps.reduce((a, b) => a > b ? a : b) : 1.0;

    return Container(
      height: 200,
      padding: const EdgeInsets.all(16),
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
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: LineChart(
              LineChartData(
                gridData: const FlGridData(show: true),
                titlesData: FlTitlesData(
                  leftTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: true, reservedSize: 40),
                  ),
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 60, // Increased for vertical text
                      getTitlesWidget: (value, meta) {
                        final dateTime = DateTime.fromMillisecondsSinceEpoch(value.toInt());
                        final format = DateFormat('HH:mm');
                        return RotatedBox(
                          quarterTurns: 1, // Rotate 90 degrees
                          child: Text(
                            format.format(dateTime),
                            style: const TextStyle(fontSize: 10),
                          ),
                        );
                      },
                    ),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                ),
                borderData: FlBorderData(show: true),
                minX: minX,
                maxX: maxX,
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: true,
                    color: ColorUtils.main,
                    barWidth: 3,
                    belowBarData: BarAreaData(
                      show: true,
                      color: ColorUtils.main.withValues(alpha: 0.1),
                    ),
                    dotData: FlDotData(show: false),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(String title, String value, [IconData? icon]) {
    return SizedBox(
      width: 140, // Increased width further
      height: 140, // Increased height to prevent overflow
      child: Card(
        elevation: 4,
        child: Padding(
          padding: const EdgeInsets.all(12.0), // Reduced padding to fit more content
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(
                  icon,
                  size: 28, // Slightly smaller icon to fit better
                  color: ColorUtils.main,
                ),
                const SizedBox(height: 4), // Reduced spacing
              ],
              Text(
                title,
                style: const TextStyle(
                  fontSize: 14, // Slightly smaller font for title
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 4), // Reduced spacing
              Text(
                value,
                style: const TextStyle(
                  fontSize: 12, // Slightly smaller font for value
                  color: Colors.grey,
                  height: 1.3, // Better line height for readability
                ),
                textAlign: TextAlign.center,
                maxLines: 4, // Allow more lines for the value text
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}