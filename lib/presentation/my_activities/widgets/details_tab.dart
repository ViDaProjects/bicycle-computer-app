import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:latlong2/latlong.dart';
import 'package:flutter_map/flutter_map.dart';

import '../../../domain/entities/activity.dart';
import '../../../domain/entities/enum/activity_type.dart';
import '../../common/core/utils/activity_utils.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/core/widgets/date.dart';
import '../../common/core/widgets/share_map_button.dart';
import '../../common/location/widgets/location_map.dart';
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
            label: 'Início',
            dateTime: activity.startDatetime,
          ),
          _buildDateTimeItem(
            icon: Icons.stop,
            label: 'Término',
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
    final provider = ref.read(activityDetailsViewModelProvider.notifier);

    final displayedActivity = state.activity ?? activity;
    ActivityType selectedType = state.type ?? displayedActivity.type;

    // Calculate the points for the location map.
    final List<LatLng> points = provider.savedPositionsLatLng(activity);
    final List<Marker> markers = [];

    // Add markers to the map if activity locations are available.
    if (activity.locations.isNotEmpty) {
      markers.add(
        Marker(
          width: 80.0,
          height: 80.0,
          point: LatLng(
            activity.locations.first.latitude,
            activity.locations.first.longitude,
          ),
          child: Column(
            children: [
              IconButton(
                icon: const Icon(Icons.location_on_rounded),
                color: ColorUtils.greenDarker,
                iconSize: 35.0,
                onPressed: () {},
              ),
            ],
          ),
        ),
      );

      if (activity.locations.length > 1) {
        markers.add(
          Marker(
            width: 80.0,
            height: 80.0,
            point: LatLng(
              activity.locations.last.latitude,
              activity.locations.last.longitude,
            ),
            child: Column(
              children: [
                IconButton(
                  icon: const Icon(Icons.location_on_rounded),
                  color: ColorUtils.red,
                  iconSize: 35.0,
                  onPressed: () {},
                ),
              ],
            ),
          ),
        );
      }
    }

    return Scaffold(
      body: Column(
        children: [
          const SizedBox(height: 10),
          state.isEditing
              ? Column(children: [
                  ActivityUtils.buildActivityTypeDropdown(
                      context, selectedType, provider),
                  const SizedBox(height: 20)
                ])
              : Container(),
          Date(date: displayedActivity.startDatetime),
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
          Expanded(
            child: SizedBox(
                height: 500,
                child: ClipRRect(
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(150),
                    topRight: Radius.circular(150),
                  ),
                  child: RepaintBoundary(
                    key: state.boundaryKey,
                    child: LocationMap(
                      points: points,
                      markers: markers,
                      mapController: MapController(),
                    ),
                  ),
                )),
          )
        ],
      ),
      floatingActionButton: state.isEditing || state.isLoading
          ? FloatingActionButton(
              heroTag: 'save_button',
              backgroundColor: ColorUtils.main,
              elevation: 4.0,
              onPressed: state.isLoading
                  ? null
                  : () {
                      provider.save(displayedActivity);
                    },
              child: Icon(
                Icons.save,
                color: ColorUtils.white,
              ),
            )
          : Stack(
              children: [
                Positioned(
                  bottom: 16,
                  right: 80,
                  child: ShareMapButton(
                      activity: displayedActivity,
                      boundaryKey: state.boundaryKey),
                ),
              ],
            ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }
}
