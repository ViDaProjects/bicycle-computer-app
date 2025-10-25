import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:latlong2/latlong.dart';

import '../../core/utils/color_utils.dart';
import '../../core/utils/map_utils.dart';

/// Widget that displays a map with markers and polylines representing locations.
class LocationMap extends HookConsumerWidget {
  final List<LatLng> points;
  final List<Marker> markers;
  final MapController? mapController;
  final LatLng? currentPosition;
  final VoidCallback? onMapReady;
  final bool useSatelliteView;

  const LocationMap(
      {super.key,
      required this.points,
      required this.markers,
      required this.mapController,
      this.currentPosition,
      this.onMapReady,
      this.useSatelliteView = false});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final center = MapUtils.getCenterOfMap(points);
    final zoomLevel = MapUtils.getZoomLevel(points, center);

    // Default to São Paulo if no position is available
    const defaultLocation = LatLng(-23.5505, -46.6333); // São Paulo coordinates

    // Prioritize current position over saved points for initial center
    final initialCenter = currentPosition ?? (points.isNotEmpty ? center : defaultLocation);

    // Call onMapReady callback when the map is built
    useEffect(() {
      if (onMapReady != null) {
        // Call immediately to ensure map is marked as ready
        onMapReady!();
      }
      return null;
    }, [onMapReady]);

    return FlutterMap(
        key: ValueKey('${MediaQuery.of(context).orientation}-$useSatelliteView-${currentPosition?.latitude}-${currentPosition?.longitude}'),
        mapController: mapController,
        options: MapOptions(
          initialCenter: initialCenter,
          initialZoom: zoomLevel,
        ),
        children: [
          // Choose tile layer based on satellite view preference
          useSatelliteView
              ? TileLayer(
                  urlTemplate: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                  userAgentPackageName: 'be_for_bike',
                  subdomains: const ['a', 'b', 'c'],
                )
              : TileLayer(
                  urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  userAgentPackageName: 'be_for_bike',
                ),
          PolylineLayer(
            polylines: [
              Polyline(
                  points: points,
                  strokeWidth: 4,
                  color: ColorUtils.blueGrey),
            ],
          ),
          MarkerLayer(markers: markers),
        ],
      );
  }
}
