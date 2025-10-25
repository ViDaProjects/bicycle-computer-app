import 'package:permission_handler/permission_handler.dart';

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
      Permission.storage,
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
    final locationStatus = await Permission.location.status;
    final bluetoothStatus = await Permission.bluetooth.status;
    final storageStatus = await Permission.storage.status;

    if (locationStatus.isPermanentlyDenied ||
        bluetoothStatus.isPermanentlyDenied ||
        storageStatus.isPermanentlyDenied) {
      return await openAppSettings();
    }

    return false;
  }
}