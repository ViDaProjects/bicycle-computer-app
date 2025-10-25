class SettingsState {
  final bool isLoading;
  final bool isBluetoothEnabled;

  /// Represents the state of the settings screen.
  ///
  /// [isLoading] indicates whether the screen is in a loading state.
  /// [isBluetoothEnabled] indicates whether bluetooth is enabled.
  const SettingsState({
    required this.isLoading,
    required this.isBluetoothEnabled,
  });

  /// Creates an initial state for the settings screen.
  factory SettingsState.initial() {
    return const SettingsState(
      isLoading: false,
      isBluetoothEnabled: false,
    );
  }

  /// Creates a copy of this state object with the provided changes.
  SettingsState copyWith({
    bool? isLoading,
    bool? isBluetoothEnabled,
  }) {
    return SettingsState(
      isLoading: isLoading ?? this.isLoading,
      isBluetoothEnabled: isBluetoothEnabled ?? this.isBluetoothEnabled,
    );
  }
}
