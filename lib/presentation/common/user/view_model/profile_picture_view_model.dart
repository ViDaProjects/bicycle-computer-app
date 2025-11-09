import 'dart:typed_data';

import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../../data/repositories/user_repository_impl.dart';
import 'state/profile_picture_state.dart';

/// Provider for the profile picture view model.
final profilePictureViewModelProvider = NotifierProvider.family<
    ProfilePictureViewModel,
    ProfilePictureState,
    String>((userId) => ProfilePictureViewModel(userId));

class ProfilePictureViewModel extends Notifier<ProfilePictureState> {
  final String userId;

  ProfilePictureViewModel(this.userId);

  @override
  ProfilePictureState build() {
    return ProfilePictureState.initial();
  }

  Future<void> getProfilePicture(String userId) async {
    if (state.loaded == false) {
      ref.read(userRepositoryProvider).downloadProfilePicture(userId).then(
          (value) =>
              state = state.copyWith(profilePicture: value, loaded: true));
    }
  }

  void editProfilePicture(Uint8List? image) {
    state = state.copyWith(profilePicture: image);
  }
}
