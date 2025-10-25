class SettingsState {
  final bool isLoading;
  final bool isBluetoothEnabled;
  final bool isDarkMode;
  final bool showSpeedChart;
  final bool showCadenceChart;
  final bool showPowerChart;
  final bool showAltitudeChart;

  /// Represents the state of the settings screen.
  ///
  /// [isLoading] indicates whether the screen is in a loading state.
  /// [isBluetoothEnabled] indicates whether bluetooth is enabled.
  /// [isDarkMode] indicates whether dark mode is enabled.
  /// [showSpeedChart] indicates whether to show speed chart.
  /// [showCadenceChart] indicates whether to show cadence chart.
  /// [showPowerChart] indicates whether to show power chart.
  /// [showAltitudeChart] indicates whether to show altitude chart.
  const SettingsState({
    required this.isLoading,
    required this.isBluetoothEnabled,
    required this.isDarkMode,
    required this.showSpeedChart,
    required this.showCadenceChart,
    required this.showPowerChart,
    required this.showAltitudeChart,
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
  }) {
    return SettingsState(
      isLoading: isLoading ?? this.isLoading,
      isBluetoothEnabled: isBluetoothEnabled ?? this.isBluetoothEnabled,
      isDarkMode: isDarkMode ?? this.isDarkMode,
      showSpeedChart: showSpeedChart ?? this.showSpeedChart,
      showCadenceChart: showCadenceChart ?? this.showCadenceChart,
      showPowerChart: showPowerChart ?? this.showPowerChart,
      showAltitudeChart: showAltitudeChart ?? this.showAltitudeChart,
    );
  }
}
