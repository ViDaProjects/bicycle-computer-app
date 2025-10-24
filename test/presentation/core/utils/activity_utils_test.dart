import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:be_for_bike/domain/entities/enum/activity_type.dart';
import 'package:be_for_bike/presentation/common/core/utils/activity_utils.dart';

void main() {
  group('ActivityUtils', () {
    test('getActivityTypeIcon should return correct icons', () {
      expect(ActivityUtils.getActivityTypeIcon(ActivityType.cycling),
          Icons.directions_bike);
    });
  });
}
