import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../../../domain/entities/activity.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/location/widgets/location_map.dart';
import '../../common/location/view_model/location_view_model.dart';

/// The map screen that displays a general map view.
class MapScreen extends HookConsumerWidget {
  const MapScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final locationState = ref.watch(locationViewModelProvider);
    final currentPosition = locationState.currentPosition;

    final center = currentPosition != null
        ? LatLng(currentPosition.latitude, currentPosition.longitude)
        : const LatLng(-23.5505, -46.6333); // São Paulo as default

    return Scaffold(
      appBar: AppBar(
        title: const Text('Map'),
        backgroundColor: ColorUtils.mainMedium,
      ),
      body: FlutterMap(
        options: MapOptions(
          initialCenter: center,
          initialZoom: 15.0,
        ),
        children: [
          TileLayer(
            urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
            userAgentPackageName: 'be_for_bike',
          ),
          if (currentPosition != null)
            MarkerLayer(
              markers: [
                Marker(
                  point: LatLng(currentPosition.latitude, currentPosition.longitude),
                  child: const Icon(
                    Icons.my_location,
                    color: Colors.blue,
                    size: 30,
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}