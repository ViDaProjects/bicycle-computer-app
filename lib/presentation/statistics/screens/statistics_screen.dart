import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';
import 'dart:math';

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
                      // Time and Duration Cards (Top Priority)
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildTimeCard('Start Time', DateFormat('HH:mm:ss\ndd/MM/yyyy').format(selectedActivity!.startDatetime), Icons.access_time),
                          _buildTimeCard('End Time', DateFormat('HH:mm:ss\ndd/MM/yyyy').format(selectedActivity!.endDatetime), Icons.access_time_filled),
                          _buildTimeCard('Duration', _formatDuration(selectedActivity!.endDatetime.difference(selectedActivity!.startDatetime)), Icons.timer),
                        ],
                      ),
                      const SizedBox(height: 16),

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
                          _buildStatCard('Distance', '${selectedActivity!.distance.toStringAsFixed(2)} km', Icons.straighten),
                          _buildStatCard('Calories', '${selectedActivity!.calories.toStringAsFixed(0)} kcal', Icons.local_fire_department),
                        ],
                      ),
                      const SizedBox(height: 30),

                      // Speed Chart
                      _buildChartCard('Speed (km/h)', 'speed', state.activityData),
                      const SizedBox(height: 20),

                      // Cadence Chart
                      _buildChartCard('Cadence (rpm)', 'cadence', state.activityData),
                      const SizedBox(height: 20),

                      // Power Chart
                      _buildChartCard('Power (W)', 'power', state.activityData),
                      const SizedBox(height: 20),

                      // Altitude Chart
                      _buildChartCard('Altitude (m)', 'altitude', state.activityData),
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

    // Calculate Y-axis range: start at 0, end at a nice grid value
    final values = data.map((entry) => (entry[dataType] as num?)?.toDouble() ?? 0.0).toList();
    final maxValue = values.isNotEmpty ? values.reduce((a, b) => a > b ? a : b) : 0.0;
    final minY = 0.0;
    final maxY = _calculateNiceMax(maxValue);

    return Container(
      height: 280,
      padding: const EdgeInsets.symmetric(vertical: 16),
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
        crossAxisAlignment: CrossAxisAlignment.center,
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
                    leftTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        reservedSize: 45, // Reduced from 60 to 45 for less empty space
                        getTitlesWidget: (value, meta) {
                          return Padding(
                            padding: const EdgeInsets.only(right: 8), // Add padding between Y-axis labels and chart
                            child: Text(
                              value.toStringAsFixed(1), // Format to 1 decimal place
                              style: const TextStyle(
                                fontSize: 10,
                                color: Colors.black87,
                                fontWeight: FontWeight.w500,
                              ),
                              textAlign: TextAlign.right,
                              overflow: TextOverflow.visible, // Prevent text wrapping
                              maxLines: 1,
                            ),
                          );
                        },
                      ),
                    ),
                  bottomTitles: AxisTitles(
                    axisNameWidget: const Text(
                      'hour',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    axisNameSize: 20,
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 40, // Reduced from 60 to 40 for less empty space
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
                minY: minY,
                maxY: maxY,
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

  Widget _buildTimeCard(String title, String value, [IconData? icon]) {
    return SizedBox(
      width: 105, // Smaller width for time cards to fit 3 in a row
      height: 140,
      child: Card(
        elevation: 4,
        child: Padding(
          padding: const EdgeInsets.all(8.0), // Reduced padding for smaller cards
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(
                  icon,
                  size: 24, // Smaller icon for time cards
                  color: ColorUtils.main,
                ),
                const SizedBox(height: 2), // Reduced spacing
              ],
              Text(
                title,
                style: const TextStyle(
                  fontSize: 12, // Smaller font for title
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 2), // Reduced spacing
              Text(
                value,
                style: const TextStyle(
                  fontSize: 10, // Smaller font for value
                  color: Colors.grey,
                  height: 1.2, // Better line height for readability
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

  /// Calculates a nice maximum value for the Y-axis that creates clean grid lines.
  /// Returns the next higher "nice" number (like 10, 20, 50, 100, etc.) that encompasses the maxValue.
  double _calculateNiceMax(double maxValue) {
    if (maxValue <= 0) return 10.0; // Minimum chart height

    // Find the magnitude (power of 10)
    final magnitude = (log(maxValue) / ln10).floor(); // log10
    final power = magnitude.toDouble();

    // Get the first digit
    final firstDigit = (maxValue / pow(10.0, power)).floor();

    // Calculate nice maximum based on first digit
    double niceMax;
    if (firstDigit <= 1) {
      niceMax = 2.0;
    } else if (firstDigit <= 2) {
      niceMax = 5.0;
    } else if (firstDigit <= 5) {
      niceMax = 10.0;
    } else {
      niceMax = 20.0;
    }

    // Scale back to original magnitude
    return niceMax * pow(10.0, power);
  }

  /// Formats a Duration into a readable string (HH:MM:SS).
  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, '0');
    final hours = twoDigits(duration.inHours);
    final minutes = twoDigits(duration.inMinutes.remainder(60));
    final seconds = twoDigits(duration.inSeconds.remainder(60));
    return '$hours:$minutes:$seconds';
  }
}