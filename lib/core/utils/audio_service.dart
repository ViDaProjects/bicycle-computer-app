import 'package:vibration/vibration.dart';
import 'package:flutter/foundation.dart';

/// Service for managing haptic feedback in the app.
/// Provides fitness-inspired vibration patterns for button interactions.
class AudioService {
  static final AudioService _instance = AudioService._internal();
  factory AudioService() => _instance;

  AudioService._internal();

  bool _isInitialized = false;
  bool _hasVibrator = false;

  /// Initialize the haptic service
  Future<void> initialize() async {
    if (_isInitialized) return;

    try {
      _hasVibrator = await Vibration.hasVibrator() ?? false;
      _isInitialized = true;
      debugPrint('AudioService initialized with vibration support: $_hasVibrator');
    } catch (e) {
      debugPrint('Failed to initialize AudioService: $e');
      _hasVibrator = false;
    }
  }

  /// Play button click vibration - for general button interactions
  Future<void> playButtonClick() async {
    await _vibrate(pattern: [0, 10, 5, 5]); // Quick double tap
  }

  /// Play navigation click vibration - for screen transitions
  Future<void> playNavigationClick() async {
    await _vibrate(pattern: [0, 15, 5, 10]); // Slightly longer for navigation
  }

  /// Play success vibration - for completed actions
  Future<void> playActionSuccess() async {
    await _vibrate(pattern: [0, 20, 10, 20, 10, 10]); // Success pattern
  }

  /// Internal method to vibrate with pattern
  Future<void> _vibrate({required List<int> pattern}) async {
    if (!_isInitialized) {
      await initialize();
    }

    if (!_hasVibrator) {
      debugPrint('Vibration not supported on this device');
      return;
    }

    try {
      await Vibration.vibrate(pattern: pattern);
    } catch (e) {
      debugPrint('Failed to vibrate: $e');
      // Silently fail - don't crash the app if vibration fails
    }
  }

  /// Dispose of the haptic service
  void dispose() {
    _isInitialized = false;
  }
}