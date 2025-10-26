import 'package:flutter/cupertino.dart';

/// Represents the state of the SumUpScreen.
class SumUpState {
  final bool isSaving;
  final GlobalKey boundaryKey;

  /// Creates a new instance of SumUpState.
  const SumUpState({required this.isSaving, required this.boundaryKey});

  /// Creates an initial state with default values.
  factory SumUpState.initial() {
    return SumUpState(isSaving: false, boundaryKey: GlobalKey());
  }

  /// Creates a copy of the state with optional updates.
  SumUpState copyWith({
    bool? isSaving,
  }) {
    return SumUpState(
        isSaving: isSaving ?? this.isSaving, boundaryKey: boundaryKey);
  }
}
