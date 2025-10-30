import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Provider for the permission service.
final permissionServiceProvider = Provider<PermissionService>((ref) {
  return PermissionService();
});

/// Service for handling app permissions
class PermissionService {
  /// Request all necessary permissions for the app
  Future<Map<Permission, PermissionStatus>> requestAllPermissions() async {
    final permissions = [
      Permission.location,
      Permission.locationWhenInUse,
      Permission.locationAlways,
      Permission.bluetooth,
      Permission.bluetoothConnect,
      Permission.bluetoothScan,
      Permission.bluetoothAdvertise,
      Permission.storage,
      // Permissões futuras (Android 15+) serão tratadas apenas no código nativo
    ];

    final Map<Permission, PermissionStatus> statuses = {};

    for (final permission in permissions) {
      final status = await permission.request();
      statuses[permission] = status;
    }

    return statuses;
  }

  /// Check if all necessary permissions are granted
  Future<bool> checkAllPermissionsGranted() async {
    final permissions = [
      Permission.location,
      Permission.locationWhenInUse,
      Permission.bluetooth,
      Permission.bluetoothConnect,
      Permission.bluetoothScan,
      Permission.bluetoothAdvertise,
      Permission.storage,
    ];

    for (final permission in permissions) {
      final status = await permission.status;
      if (!status.isGranted) {
        return false;
      }
    }

    return true;
  }

  /// Open app settings if permissions are permanently denied
  Future<bool> openAppSettingsIfNeeded() async {
    // Simple method to open app settings - permissão handling is done natively
    return await openAppSettings();
  }

  /// This method is kept for compatibility but permissions are handled natively
  Future<bool> checkAndReinforceBlePermissions() async {
    // Delegate to native code - avoid permission_handler issues
    return true;
  }
}
