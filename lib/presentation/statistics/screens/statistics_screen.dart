import 'package:flutter/material.dart';
import 'package:be_for_bike/l10n/app_localizations.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';

import '../../common/core/utils/color_utils.dart';

/// The statistics screen that displays general activity statistics and charts.
class StatisticsScreen extends HookConsumerWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.statistics),
        backgroundColor: ColorUtils.mainMedium,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 20),
              Text(
                'Activity Statistics',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 20),
              // Sample chart
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
                child: LineChart(
                  LineChartData(
                    gridData: FlGridData(show: false),
                    titlesData: FlTitlesData(show: false),
                    borderData: FlBorderData(show: false),
                    lineBarsData: [
                      LineChartBarData(
                        spots: const [
                          FlSpot(0, 1),
                          FlSpot(1, 3),
                          FlSpot(2, 2),
                          FlSpot(3, 5),
                          FlSpot(4, 3),
                          FlSpot(5, 4),
                          FlSpot(6, 6),
                        ],
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
              const SizedBox(height: 20),
              // Summary stats
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildStatCard('Total Activities', '0'),
                  _buildStatCard('Total Distance', '0 km'),
                  _buildStatCard('Total Time', '0h 0m'),
                ],
              ),
              const SizedBox(height: 20),
              // Additional chart placeholder
              Container(
                height: 150,
                decoration: BoxDecoration(
                  color: ColorUtils.mainLight.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Center(
                  child: Text('More charts coming soon...'),
                ),
              ),
            ],
          ),
        ),
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
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              title,
              style: const TextStyle(
                fontSize: 12,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}