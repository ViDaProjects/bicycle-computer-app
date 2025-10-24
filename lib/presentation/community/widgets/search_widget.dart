import 'package:flutter/material.dart';
import 'package:be_for_bike/l10n/app_localizations.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../domain/entities/user.dart';
import '../../common/core/utils/color_utils.dart';

class SearchWidget extends HookConsumerWidget implements PreferredSizeWidget {
  final TextEditingController searchController;
  final Future<List<User>> Function(String) onSearchChanged;

  const SearchWidget({
    super.key,
    required this.searchController,
    required this.onSearchChanged,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AppBar(
      backgroundColor: ColorUtils.white,
      title: TextField(
        controller: searchController,
        decoration: InputDecoration(
          hintText: '${AppLocalizations.of(context)!.search}...',
          border: InputBorder.none,
          suffixIconColor: ColorUtils.main,
          suffixIcon: const Icon(Icons.search),
        ),
        onChanged: (query) {
          if (query.isNotEmpty) {
            onSearchChanged(query);
          }
        },
      ),
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);
}
