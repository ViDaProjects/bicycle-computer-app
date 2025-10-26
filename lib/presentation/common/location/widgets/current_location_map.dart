import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:latlong2/latlong.dart';

import '../../core/utils/color_utils.dart';
import '../view_model/location_view_model.dart';
import 'location_map.dart';

/// Widget that displays the current location on a map.
class CurrentLocationMap extends HookConsumerWidget {
  const CurrentLocationMap({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = ref.read(locationViewModelProvider.notifier);
    final state = ref.watch(locationViewModelProvider);

    final points = provider.savedPositionsLatLng();

    final currentPosition = state.currentPosition;
    final currentLatitude = currentPosition?.latitude ?? 0;
    final currentLongitude = currentPosition?.longitude ?? 0;

    // State for satellite view toggle
    final useSatelliteView = useState(false);

    // Start location service when widget is built
    useEffect(() {
      provider.startGettingLocation();
      return () async {
        await provider.cancelLocationStream();
      };
    }, [provider]);

    // Force map ready state immediately
    useEffect(() {
      provider.setMapReady();
      return null;
    }, []);

    final markers = <Marker>[];
    if (currentPosition != null) {
      markers.add(
        Marker(
          width: 80,
          height: 80,
          point: LatLng(currentLatitude, currentLongitude),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.8),
              shape: BoxShape.circle,
              border: Border.all(color: Colors.white, width: 2),
            ),
            child: const Icon(
              Icons.my_location,
              size: 20,
              color: Colors.white,
            ),
          ),
        ),
      );
    }

    if (points.isNotEmpty) {
      markers.add(
        Marker(
          width: 80.0,
          height: 80.0,
          point: LatLng(
            points.first.latitude,
            points.first.longitude,
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
    }

    // Always show the map
    return Expanded(
        child: SizedBox(
            height: 500,
            child: Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(150),
                    topRight: Radius.circular(150),
                  ),
                  child: LocationMap(
                    points: points,
                    markers: markers,
                    currentPosition: currentPosition != null ? LatLng(currentLatitude, currentLongitude) : null,
                    mapController: MapController(),
                    onMapReady: () {
                      // Map ready functionality removed
                    },
                    useSatelliteView: useSatelliteView.value,
                  ),
                ),
                // Satellite/Map toggle button - always visible
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
            )));
  }
}
