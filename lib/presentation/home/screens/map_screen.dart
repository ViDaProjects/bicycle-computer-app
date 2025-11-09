import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:flutter_hooks/flutter_hooks.dart';

import '../../common/core/utils/color_utils.dart';
import '../../common/location/view_model/location_view_model.dart';
import '../../../core/utils/audio_service.dart';
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

    // State to track if map has been moved away from activity path
    final mapMoved = useState(false);

    // Start getting location when no activity is selected
    useEffect(() {
      if (selectedActivity == null) {
        // Force location update when screen is first loaded
        Future.microtask(() async {
          try {
            await locationViewModel.startGettingLocation();
          } catch (e) {
            // Handle location service errors gracefully
            debugPrint('Error starting location service: $e');
          }
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

    // Calculate optimal zoom level to fit the entire path on screen
    double calculateOptimalZoom(List<LatLng> points, double mapWidth, double mapHeight) {
      if (points.isEmpty || points.length < 2) return 15.0;

      try {
        final minLat = points.map((p) => p.latitude).reduce((a, b) => a < b ? a : b);
        final maxLat = points.map((p) => p.latitude).reduce((a, b) => a > b ? a : b);
        final minLng = points.map((p) => p.longitude).reduce((a, b) => a < b ? a : b);
        final maxLng = points.map((p) => p.longitude).reduce((a, b) => a > b ? a : b);

        final latDiff = maxLat - minLat;
        final lngDiff = maxLng - minLng;

        // Add padding to prevent cutting off start/end
        final paddedLatDiff = latDiff * 1.2;
        final paddedLngDiff = lngDiff * 1.2;

        // If the path is very small (less than 100m), use a high zoom
        if (paddedLatDiff < 0.001 && paddedLngDiff < 0.001) {
          debugPrint('Path is very small, using zoom 18.0');
          return 18.0;
        }

        // Calculate zoom level based on the larger dimension
        // Formula: zoom = log2( screenPixels * 360 / (diffDegrees * 256 * cos(lat)) )
        final latZoom = math.log(mapHeight * 360 / (paddedLatDiff * 256)) / math.ln2;
        final lngZoom = math.log(mapWidth * 360 / (paddedLngDiff * 256 * math.cos(maxLat * math.pi / 180))) / math.ln2;

        final optimalZoom = [latZoom, lngZoom].reduce((a, b) => a < b ? a : b);

        // Clamp zoom between reasonable bounds
        final clampedZoom = optimalZoom.clamp(8.0, 18.0);
        debugPrint('Zoom calculation: latDiff=$latDiff, lngDiff=$lngDiff, paddedLat=$paddedLatDiff, paddedLng=$paddedLngDiff, latZoom=$latZoom, lngZoom=$lngZoom, optimal=$optimalZoom, clamped=$clampedZoom');
        return clampedZoom;
      } catch (e) {
        // Fallback zoom if calculation fails
        debugPrint('Zoom calculation failed: $e, using fallback 14.0');
        return 14.0;
      }
    }
    List<LatLng> smoothActivityPoints(List<LatLng> points) {
      // Handle edge cases
      if (points.isEmpty) return points;
      if (points.length == 1) return points;
      if (points.length == 2) return points;

      // Process all GPS points without artificial limits
      final effectivePoints = points;

      // Use Catmull-Rom spline interpolation for smooth curves
      final smoothed = <LatLng>[];

      // Add first point
      smoothed.add(effectivePoints[0]);

      // For each segment between points, interpolate
      for (int i = 0; i < effectivePoints.length - 1; i++) {
        final p0 = i > 0 ? effectivePoints[i - 1] : effectivePoints[i]; // Previous point or current if first
        final p1 = effectivePoints[i]; // Current point
        final p2 = effectivePoints[i + 1]; // Next point
        final p3 = i < effectivePoints.length - 2 ? effectivePoints[i + 2] : effectivePoints[i + 1]; // Next next point or current if last

        // Generate intermediate points using Catmull-Rom spline
        const int segments = 5; // Reduced from 10 to 5 for better performance
        for (int t = 1; t <= segments; t++) {
          final double u = t / segments;
          final double u2 = u * u;
          final double u3 = u2 * u;

          // Catmull-Rom spline formula
          final double lat = 0.5 * (
            (2 * p1.latitude) +
            (-p0.latitude + p2.latitude) * u +
            (2 * p0.latitude - 5 * p1.latitude + 4 * p2.latitude - p3.latitude) * u2 +
            (-p0.latitude + 3 * p1.latitude - 3 * p2.latitude + p3.latitude) * u3
          );

          final double lng = 0.5 * (
            (2 * p1.longitude) +
            (-p0.longitude + p2.longitude) * u +
            (2 * p0.longitude - 5 * p1.longitude + 4 * p2.longitude - p3.longitude) * u2 +
            (-p0.longitude + 3 * p1.longitude - 3 * p2.longitude + p3.longitude) * u3
          );

          smoothed.add(LatLng(lat, lng));
        }
      }

      // Ensure the last point is included
      if (smoothed.isNotEmpty && effectivePoints.isNotEmpty) {
        final lastPoint = effectivePoints.last;
        if (smoothed.last.latitude != lastPoint.latitude ||
            smoothed.last.longitude != lastPoint.longitude) {
          smoothed.add(lastPoint);
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
      // Activity selection effect - reset map moved state
      mapMoved.value = false;
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
          child: IconButton(
            icon: const Icon(
              Icons.play_arrow,
              color: Colors.green,
              size: 30,
            ),
            onPressed: () {},
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
            child: IconButton(
              icon: const Icon(
                Icons.stop,
                color: Colors.red,
                size: 30,
              ),
              onPressed: () {},
            ),
          ),
        );
      }
    }

    // Determine map center
    LatLng center;
    if (activityPoints.isNotEmpty && activityPoints.length >= 2) {
      // Center on activity path
      try {
        final minLat = activityPoints.map((p) => p.latitude).reduce((a, b) => a < b ? a : b);
        final maxLat = activityPoints.map((p) => p.latitude).reduce((a, b) => a > b ? a : b);
        final minLng = activityPoints.map((p) => p.longitude).reduce((a, b) => a < b ? a : b);
        final maxLng = activityPoints.map((p) => p.longitude).reduce((a, b) => a > b ? a : b);
        center = LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2);
      } catch (e) {
        // Fallback if bounds calculation fails
        center = currentPosition != null
            ? LatLng(currentPosition.latitude, currentPosition.longitude)
            : const LatLng(-23.5505, -46.6333);
      }
    } else {
      // Default to current position or São Paulo
      center = currentPosition != null
          ? LatLng(currentPosition.latitude, currentPosition.longitude)
          : const LatLng(-23.5505, -46.6333);
    }

    // Get screen dimensions for zoom calculation
    final screenSize = MediaQuery.of(context).size;
    final mapWidth = screenSize.width - 24; // Account for margins
    final mapHeight = screenSize.height * 0.7; // Use 70% of screen height for map

    // Calculate optimal zoom level
    final optimalZoom = activityPoints.isNotEmpty
        ? calculateOptimalZoom(activityPoints, mapWidth, mapHeight)
        : 15.0;

    // Debug logging for zoom calculation
    if (activityPoints.isNotEmpty) {
      debugPrint('Map zoom calculation: ${activityPoints.length} points, screen: $mapWidth x $mapHeight, optimal zoom: $optimalZoom');
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
                    initialZoom: optimalZoom,
                    onMapReady: () {
                      mapReady.value = true;
                    },
                    onPositionChanged: (position, hasGesture) {
                      // Mark map as moved when user gestures (pans/zooms)
                      if (hasGesture && activityPoints.isNotEmpty) {
                        mapMoved.value = true;
                      }
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
                            strokeWidth: 4,
                            color: ColorUtils.blueGrey,
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
                // Map control buttons
                Positioned(
                  top: 16,
                  right: 16,
                  child: Column(
                    children: [
                      // Satellite/Map toggle button
                      Container(
                        margin: const EdgeInsets.only(bottom: 8),
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
                            AudioService().playButtonClick();
                            useSatelliteView.value = !useSatelliteView.value;
                          },
                          tooltip: useSatelliteView.value ? 'Mostrar mapa' : 'Mostrar satélite',
                        ),
                      ),
                    ],
                  ),
                ),
                // Zoom buttons at bottom right
                Positioned(
                  bottom: 16,
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
                    child: Column(
                      children: [
                        IconButton(
                          icon: Icon(
                            Icons.add,
                            color: ColorUtils.main,
                          ),
                          onPressed: () {
                            AudioService().playButtonClick();
                            final currentZoom = mapController.camera.zoom;
                            mapController.move(
                              mapController.camera.center,
                              currentZoom + 1,
                            );
                          },
                          tooltip: 'Aumentar zoom',
                        ),
                        Container(
                          height: 1,
                          color: ColorUtils.main.withValues(alpha: 0.3),
                          margin: const EdgeInsets.symmetric(horizontal: 8),
                        ),
                        IconButton(
                          icon: Icon(
                            Icons.remove,
                            color: ColorUtils.main,
                          ),
                          onPressed: () {
                            AudioService().playButtonClick();
                            final currentZoom = mapController.camera.zoom;
                            mapController.move(
                              mapController.camera.center,
                              currentZoom - 1,
                            );
                          },
                          tooltip: 'Diminuir zoom',
                        ),
                      ],
                    ),
                  ),
                ),
                // Recenter button at bottom left (only visible when map is moved)
                if (mapMoved.value && activityPoints.isNotEmpty)
                  Positioned(
                    bottom: 16,
                    left: 16,
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
                        icon: const Icon(
                          Icons.my_location,
                          color: Colors.blue,
                        ),
                        onPressed: () {
                          AudioService().playButtonClick();
                          final mapWidth = MediaQuery.of(context).size.width;
                          final mapHeight = MediaQuery.of(context).size.height;

                          final optimalZoom = calculateOptimalZoom(activityPoints, mapWidth, mapHeight);
                          
                          // Calculate bounds of activity path
                          final minLat = activityPoints.map((p) => p.latitude).reduce((a, b) => a < b ? a : b);
                          final maxLat = activityPoints.map((p) => p.latitude).reduce((a, b) => a > b ? a : b);
                          final minLng = activityPoints.map((p) => p.longitude).reduce((a, b) => a < b ? a : b);
                          final maxLng = activityPoints.map((p) => p.longitude).reduce((a, b) => a > b ? a : b);

                          // Center on activity path with optimal zoom
                          final center = LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2);
                          mapController.moveAndRotate(center, optimalZoom, 0.0);
                          mapMoved.value = false; // Reset moved state
                        },
                        tooltip: 'Recentrar na rota',
                      ),
                    ),
                  ),
                // Compass button at upper left
                Positioned(
                  top: 16,
                  left: 16,
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
                        Icons.explore,
                        color: ColorUtils.main,
                      ),
                      onPressed: () {
                        AudioService().playButtonClick();
                        mapController.rotate(0.0);
                      },
                      tooltip: 'Orientar para o norte',
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