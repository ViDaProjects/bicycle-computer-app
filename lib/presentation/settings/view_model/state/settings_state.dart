class SettingsState {
  final bool isLoading;
  final bool isBluetoothEnabled;
  final bool isDarkMode;
  final bool showSpeedChart;
  final bool showCadenceChart;
  final bool showPowerChart;
  final bool showAltitudeChart;
  final bool showDistanceTraveled;
  final bool showCalories;
  final bool showMap;
  final String? connectedDeviceName;
  final String? connectedDeviceMac;
  final bool isBleConnected;

  /// Represents the state of the settings screen.
  ///
  /// [isLoading] indicates whether the screen is in a loading state.
  /// [isBluetoothEnabled] indicates whether bluetooth is enabled.
  /// [isDarkMode] indicates whether dark mode is enabled.
  /// [showSpeedChart] indicates whether to show speed chart.
  /// [showCadenceChart] indicates whether to show cadence chart.
  /// [showPowerChart] indicates whether to show power chart.
  /// [showAltitudeChart] indicates whether to show altitude chart.
  /// [showDistanceTraveled] indicates whether to show distance traveled.
  /// [showCalories] indicates whether to show calories.
  /// [showMap] indicates whether to show map.
  /// [connectedDeviceName] indicates the name of the connected BLE device.
  /// [connectedDeviceMac] indicates the MAC address of the connected BLE device.
  /// [isBleConnected] indicates whether BLE is connected to a device.
  const SettingsState({
    required this.isLoading,
    required this.isBluetoothEnabled,
    required this.isDarkMode,
    required this.showSpeedChart,
    required this.showCadenceChart,
    required this.showPowerChart,
    required this.showAltitudeChart,
    required this.showDistanceTraveled,
    required this.showCalories,
    required this.showMap,
    this.connectedDeviceName,
    this.connectedDeviceMac,
    this.isBleConnected = false,
  });

  /// Creates an initial state for the settings screen.
  factory SettingsState.initial() {
    return const SettingsState(
      isLoading: false,
      isBluetoothEnabled: false,
      isDarkMode: false,
      showSpeedChart: true,
      showCadenceChart: true,
      showPowerChart: true,
      showAltitudeChart: true,
      showDistanceTraveled: true,
      showCalories: true,
      showMap: true,
      connectedDeviceName: null,
      connectedDeviceMac: null,
      isBleConnected: false,
    );
  }

  /// Creates a copy of this state object with the provided changes.
  SettingsState copyWith({
    bool? isLoading,
    bool? isBluetoothEnabled,
    bool? isDarkMode,
    bool? showSpeedChart,
    bool? showCadenceChart,
    bool? showPowerChart,
    bool? showAltitudeChart,
    bool? showDistanceTraveled,
    bool? showCalories,
    bool? showMap,
    String? connectedDeviceName,
    String? connectedDeviceMac,
    bool? isBleConnected,
  }) {
    return SettingsState(
      isLoading: isLoading ?? this.isLoading,
      isBluetoothEnabled: isBluetoothEnabled ?? this.isBluetoothEnabled,
      isDarkMode: isDarkMode ?? this.isDarkMode,
      showSpeedChart: showSpeedChart ?? this.showSpeedChart,
      showCadenceChart: showCadenceChart ?? this.showCadenceChart,
      showPowerChart: showPowerChart ?? this.showPowerChart,
      showAltitudeChart: showAltitudeChart ?? this.showAltitudeChart,
      showDistanceTraveled: showDistanceTraveled ?? this.showDistanceTraveled,
      showCalories: showCalories ?? this.showCalories,
      showMap: showMap ?? this.showMap,
      connectedDeviceName: connectedDeviceName ?? this.connectedDeviceName,
      connectedDeviceMac: connectedDeviceMac ?? this.connectedDeviceMac,
      isBleConnected: isBleConnected ?? this.isBleConnected,
    );
  }
}
