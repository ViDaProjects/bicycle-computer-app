import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';

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
      appBar: AppBar(
        title: const Text('Statistics'),
        backgroundColor: ColorUtils.mainMedium,
      ),
      body: SafeArea(
        child: state.isLoading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 20),
                    Text(
                      selectedActivity != null
                          ? 'Activity Statistics: ${selectedActivity!.type}'
                          : 'General Statistics',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
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
                          _buildStatCard('Max Speed', '${provider.getMaxValue('speed').toStringAsFixed(1)} km/h'),
                          _buildStatCard('Avg Speed', '${provider.getAverageValue('speed').toStringAsFixed(1)} km/h'),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard('Max Cadence', '${provider.getMaxValue('cadence').toStringAsFixed(0)} rpm'),
                          _buildStatCard('Avg Cadence', '${provider.getAverageValue('cadence').toStringAsFixed(0)} rpm'),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard('Max Power', '${provider.getMaxValue('power').toStringAsFixed(0)} W'),
                          _buildStatCard('Avg Power', '${provider.getAverageValue('power').toStringAsFixed(0)} W'),
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
                              color: Colors.grey.withOpacity(0.1),
                              spreadRadius: 1,
                              blurRadius: 5,
                              offset: const Offset(0, 2),
                            ),
                          ],
                        ),
                        child: const Center(
                          child: Text('Select an activity to view detailed statistics'),
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
    final spots = data.asMap().entries.map((entry) {
      final index = entry.key.toDouble();
      final value = (entry.value[dataType] as num?)?.toDouble() ?? 0.0;
      return FlSpot(index, value);
    }).toList();

    return Container(
      height: 200,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ColorUtils.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withOpacity(0.1),
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
                gridData: FlGridData(show: true),
                titlesData: FlTitlesData(
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(showTitles: true, reservedSize: 40),
                  ),
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  rightTitles: AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  topTitles: AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                ),
                borderData: FlBorderData(show: true),
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: true,
                    color: ColorUtils.main,
                    barWidth: 3,
                    belowBarData: BarAreaData(
                      show: true,
                      color: ColorUtils.main.withOpacity(0.1),
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

  Widget _buildStatCard(String title, String value) {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Text(
              value,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Color(0xFF2E7D32),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              title,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}