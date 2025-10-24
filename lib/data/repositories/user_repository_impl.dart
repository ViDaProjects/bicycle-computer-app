import 'dart:typed_data';

import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../core/utils/storage_utils.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/user_repository.dart';
import '../api/user_api.dart';
import '../model/request/edit_password_request.dart';
import '../model/request/edit_profile_request.dart';

/// Provider for the UserRepository implementation.
final userRepositoryProvider =
    Provider<UserRepository>((ref) => UserRepositoryImpl());

/// Implementation of the UserRepository.
class UserRepositoryImpl extends UserRepository {
  UserRepositoryImpl();

  @override
  Future<void> logout() async {
    await UserApi.logout();
    await StorageUtils.removeJwt();
    await StorageUtils.removeRefreshToken();
    return;
  }

  @override
  Future<void> delete() async {
    return UserApi.delete();
  }

  @override
  Future<void> editPassword(EditPasswordRequest request) async {
    await UserApi.editPassword(request);
  }

  @override
  Future<void> editProfile(EditProfileRequest request) async {
    await UserApi.editProfile(request);
  }

  @override
  Future<List<User>> search(String text) async {
    final userResponses = await UserApi.search(text);
    return userResponses.map((response) => response.toEntity()).toList();
  }

  @override
  Future<Uint8List?> downloadProfilePicture(String id) async {
    return await UserApi.downloadProfilePicture(id);
  }

  @override
  Future<void> uploadProfilePicture(Uint8List file) async {
    return await UserApi.uploadProfilePicture(file);
  }
}
