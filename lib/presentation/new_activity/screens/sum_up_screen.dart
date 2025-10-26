import 'package:flutter/material.dart';
import 'package:be_for_bike/l10n/app_localizations.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:latlong2/latlong.dart';

import '../../common/core/utils/color_utils.dart';
import '../../common/core/utils/ui_utils.dart';
import '../../common/location/view_model/location_view_model.dart';
import '../../common/location/widgets/location_map.dart';
import '../../common/metrics/widgets/metrics.dart';
import '../../common/timer/widgets/timer_sized.dart';
import '../view_model/sum_up_view_model.dart';
import '../widgets/save_button.dart';

class SumUpScreen extends HookConsumerWidget {
  const SumUpScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(sumUpViewModelProvider);

    final locations = ref.read(locationViewModelProvider).savedPositions;

    final List<LatLng> points =
        ref.read(locationViewModelProvider.notifier).savedPositionsLatLng();

    final List<Marker> markers = [];

    // Add markers to the map if activity locations are available.
    if (locations.isNotEmpty) {
      markers.add(
        Marker(
          width: 80.0,
          height: 80.0,
          point: LatLng(
            locations.first.latitude,
            locations.first.longitude,
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

      if (locations.length > 1) {
        markers.add(
          Marker(
            width: 80.0,
            height: 80.0,
            point: LatLng(
              locations.last.latitude,
              locations.last.longitude,
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
      body: state.isSaving
          ? Center(child: UIUtils.loader)
          : SafeArea(
              child: Column(
                children: [
                  UIUtils.createHeader(
                      AppLocalizations.of(context)!.activity_sumup),
                  const SizedBox(height: 10),
                  const TimerTextSized(),
                  const Metrics(),
                  const SizedBox(height: 10),
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
                      ),
                    ),
                  )
                ],
              ),
            ),
      floatingActionButton: Stack(
        children: [
          Positioned(
            bottom: 16,
            right: 80,
            child: SaveButton(disabled: state.isSaving),
          ),
        ],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }
}
