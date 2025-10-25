import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:flutter_hooks/flutter_hooks.dart';

import '../../common/core/utils/color_utils.dart';
import '../../common/location/view_model/location_view_model.dart';
import '../view_model/home_view_model.dart';

/// The map screen that displays a general map view.
class MapScreen extends HookConsumerWidget {
  const MapScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final locationState = ref.watch(locationViewModelProvider);
    final locationViewModel = ref.watch(locationViewModelProvider.notifier);
    final homeState = ref.watch(homeViewModelProvider);
    final currentPosition = locationState.currentPosition;
    final selectedActivity = homeState.selectedActivity;

    // State for satellite view toggle
    final useSatelliteView = useState(false);

    // Map controller for dynamic movement
    final mapController = useMemoized(() => MapController());

    // State to track if map is ready
    final mapReady = useState(false);

    // Start getting location when no activity is selected
    useEffect(() {
      if (selectedActivity == null) {
        // Force location update when screen is first loaded
        Future.microtask(() async {
          await locationViewModel.startGettingLocation();
        });
      }
      return null;
    }, [selectedActivity]);

    // Move map to current position when location becomes available and map is ready
    useEffect(() {
      if (currentPosition != null && selectedActivity == null && mapReady.value) {
        try {
          // Move map to current position with animation
          mapController.move(
            LatLng(currentPosition.latitude, currentPosition.longitude),
            15.0,
          );
        } catch (e) {
          // Silently ignore if controller is not ready
        }
      }
      return null;
    }, [currentPosition, selectedActivity, mapReady.value]);

    // Calculate activity path points with smoothing
    List<LatLng> smoothActivityPoints(List<LatLng> points) {
      if (points.length < 3) return points;

      final smoothed = <LatLng>[];
      const windowSize = 3; // Moving average window

      for (int i = 0; i < points.length; i++) {
        if (i < windowSize - 1) {
          smoothed.add(points[i]);
        } else {
          double sumLat = 0;
          double sumLng = 0;
          int count = 0;

          for (int j = i - windowSize + 1; j <= i; j++) {
            sumLat += points[j].latitude;
            sumLng += points[j].longitude;
            count++;
          }

          smoothed.add(LatLng(sumLat / count, sumLng / count));
        }
      }

      return smoothed;
    }

    final List<LatLng> activityPoints = selectedActivity != null
        ? smoothActivityPoints(selectedActivity.locations
            .map((location) => LatLng(location.latitude, location.longitude))
            .toList())
        : [];

    // Log activity information when selected
    useEffect(() {
      // Activity selection effect - no logging needed in production
      return null;
    }, [selectedActivity]);

    // Create markers for activity start and end points
    final List<Marker> activityMarkers = [];
    if (selectedActivity != null && selectedActivity.locations.isNotEmpty) {
      // Start marker
      activityMarkers.add(
        Marker(
          width: 40.0,
          height: 40.0,
          point: LatLng(
            selectedActivity.locations.first.latitude,
            selectedActivity.locations.first.longitude,
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              Icon(
                Icons.location_pin,
                color: ColorUtils.greenDarker,
                size: 40,
              ),
              const Positioned(
                top: 8,
                child: Icon(
                  Icons.star,
                  color: Colors.white,
                  size: 16,
                ),
              ),
            ],
          ),
        ),
      );

      // End marker (if more than one location)
      if (selectedActivity.locations.length > 1) {
        activityMarkers.add(
          Marker(
            width: 40.0,
            height: 40.0,
            point: LatLng(
              selectedActivity.locations.last.latitude,
              selectedActivity.locations.last.longitude,
            ),
            child: Stack(
              alignment: Alignment.center,
              children: [
                Icon(
                  Icons.location_pin,
                  color: ColorUtils.red,
                  size: 40,
                ),
                const Positioned(
                  top: 8,
                  child: Icon(
                    Icons.stop,
                    color: Colors.white,
                    size: 16,
                  ),
                ),
              ],
            ),
          ),
        );
      }
    }

    // Determine map center
    LatLng center;
    if (activityPoints.isNotEmpty) {
      // Center on activity path
      final minLat = activityPoints.map((p) => p.latitude).reduce((a, b) => a < b ? a : b);
      final maxLat = activityPoints.map((p) => p.latitude).reduce((a, b) => a > b ? a : b);
      final minLng = activityPoints.map((p) => p.longitude).reduce((a, b) => a < b ? a : b);
      final maxLng = activityPoints.map((p) => p.longitude).reduce((a, b) => a > b ? a : b);
      center = LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2);
    } else {
      // Default to current position or São Paulo
      center = currentPosition != null
          ? LatLng(currentPosition.latitude, currentPosition.longitude)
          : const LatLng(-23.5505, -46.6333);
    }

    return Scaffold(
      backgroundColor: ColorUtils.mainMedium,
      body: SafeArea(
        child: Container(
          margin: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.1),
                blurRadius: 20,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(20),
            child: Stack(
              children: [
                FlutterMap(
                  mapController: mapController,
                  options: MapOptions(
                    initialCenter: center,
                    initialZoom: activityPoints.isNotEmpty ? 14.0 : 15.0,
                    onMapReady: () {
                      mapReady.value = true;
                    },
                  ),
                  children: [
                    TileLayer(
                      urlTemplate: useSatelliteView.value
                          ? 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'
                          : 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      userAgentPackageName: 'be_for_bike',
                    ),
                    // Hybrid overlay for satellite view (streets and labels)
                    if (useSatelliteView.value)
                      TileLayer(
                        urlTemplate: 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}',
                        userAgentPackageName: 'be_for_bike',
                      ),
                    if (useSatelliteView.value)
                      TileLayer(
                        urlTemplate: 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}',
                        userAgentPackageName: 'be_for_bike',
                      ),
                    // Activity path polyline
                    if (activityPoints.isNotEmpty)
                      PolylineLayer(
                        polylines: [
                          Polyline(
                            points: activityPoints,
                            strokeWidth: 5,
                            color: ColorUtils.main,
                            borderColor: Colors.white,
                            borderStrokeWidth: 2,
                          ),
                        ],
                      ),
                    // Activity markers
                    if (activityMarkers.isNotEmpty)
                      MarkerLayer(markers: activityMarkers),
                    // Current position marker
                    if (currentPosition != null)
                      MarkerLayer(
                        markers: [
                          Marker(
                            point: LatLng(currentPosition.latitude, currentPosition.longitude),
                            child: Container(
                              decoration: BoxDecoration(
                                color: Colors.blue.withValues(alpha: 0.9),
                                shape: BoxShape.circle,
                                border: Border.all(color: Colors.white, width: 3),
                                boxShadow: [
                                  BoxShadow(
                                    color: Colors.black.withValues(alpha: 0.2),
                                    blurRadius: 8,
                                    offset: const Offset(0, 2),
                                  ),
                                ],
                              ),
                              child: const Icon(
                                Icons.my_location,
                                color: Colors.white,
                                size: 20,
                              ),
                            ),
                          ),
                        ],
                      ),
                    // Loading indicator when getting location
                    if (currentPosition == null && selectedActivity == null)
                      MarkerLayer(
                        markers: [
                          Marker(
                            point: center,
                            child: Container(
                              decoration: BoxDecoration(
                                color: Colors.white.withValues(alpha: 0.9),
                                shape: BoxShape.circle,
                                border: Border.all(color: ColorUtils.main, width: 2),
                              ),
                              child: const Padding(
                                padding: EdgeInsets.all(8.0),
                                child: SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF2E7D32)),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                  ],
                ),
                // Satellite/Map toggle button
                Positioned(
                  top: 16,
                  right: 16,
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.9),
                      borderRadius: BorderRadius.circular(8),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.2),
                          blurRadius: 4,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: IconButton(
                      icon: Icon(
                        useSatelliteView.value ? Icons.map : Icons.satellite,
                        color: ColorUtils.main,
                      ),
                      onPressed: () {
                        useSatelliteView.value = !useSatelliteView.value;
                      },
                      tooltip: useSatelliteView.value ? 'Mostrar mapa' : 'Mostrar satélite',
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}