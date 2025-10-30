import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../domain/entities/activity.dart';
import '../../../domain/entities/page.dart';
import '../../common/activity/widgets/activity_list.dart';
import '../../common/core/enums/infinite_scroll_list.enum.dart';
import '../../common/core/utils/ui_utils.dart';
import '../view_model/activity_list_view_model.dart';

/// The screen that displays a list of activities.
class ActivityListScreen extends HookConsumerWidget {
  const ActivityListScreen({super.key});

  Future<void> _fetchActivities(WidgetRef ref, ValueNotifier<EntityPage<Activity>?> activityPage, ValueNotifier<bool> isLoading) async {
    isLoading.value = true;
    try {
      final provider = ref.read(activityListViewModelProvider.notifier);
      final data = await provider.fetchActivities();
      activityPage.value = data;
    } finally {
      isLoading.value = false;
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final activityPage = useState<EntityPage<Activity>?>(null);
    final isLoading = useState<bool>(true);

    useEffect(() {
      _fetchActivities(ref, activityPage, isLoading);
      return null;
    }, []);

    final activityListState = ref.watch(activityListViewModelProvider);
    final provider = ref.watch(activityListViewModelProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: Text(activityListState.isSelectionMode
          ? '${activityListState.selectedActivities.length} selected'
          : 'My Activities'),
        leading: activityListState.isSelectionMode
          ? IconButton(
              icon: const Icon(Icons.close),
              onPressed: () => provider.clearSelection(),
              tooltip: 'Cancel selection',
            )
          : null,
        actions: [
          if (activityListState.isSelectionMode && activityListState.selectedActivities.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.delete),
              onPressed: () {
                _showDeleteConfirmationDialog(context, ref, activityListState.selectedActivities.length, activityPage, isLoading);
              },
              tooltip: 'Delete selected activities',
            ),
          if (!activityListState.isSelectionMode)
            IconButton(
              icon: const Icon(Icons.delete_sweep),
              onPressed: () => _showClearDatabaseDialog(context, ref, activityPage, isLoading),
              tooltip: 'Clear all activities',
            ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: isLoading.value || activityPage.value == null
                  ? Center(child: UIUtils.loader)
                  : activityPage.value!.list.isEmpty
                      ? Center(
                          child: Container(
                            height: 200,
                            padding: const EdgeInsets.all(16),
                            margin: const EdgeInsets.symmetric(horizontal: 16),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(12),
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.grey.withValues(alpha: 0.1),
                                  spreadRadius: 1,
                                  blurRadius: 5,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: const Center(
                              child: Text('No activities on database'),
                            ),
                          ),
                        )
                      : RefreshIndicator(
                          onRefresh: () => _fetchActivities(ref, activityPage, isLoading),
                          child: ActivityList(
                            id: InfiniteScrollListEnum.myActivities.toString(),
                            activities: activityPage.value!.list,
                            total: activityPage.value!.total,
                            bottomListScrollFct: provider.fetchActivities,
                          ),
                        ),
            ),
          ],
        ),
      ),
    );
  }

  void _showClearDatabaseDialog(BuildContext context, WidgetRef ref, ValueNotifier<EntityPage<Activity>?> activityPage, ValueNotifier<bool> isLoading) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear Database'),
        content: const Text('Delete all activities from the database?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Clear All'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      final platform = const MethodChannel('com.beforbike.app/database');
      try {
        await platform.invokeMethod('clearDatabase');
        await _fetchActivities(ref, activityPage, isLoading);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Database cleared')),
          );
        }
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error clearing database: $e')),
          );
        }
      }
    }
  }

  void _showDeleteConfirmationDialog(BuildContext context, WidgetRef ref, int count, ValueNotifier<EntityPage<Activity>?> activityPage, ValueNotifier<bool> isLoading) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Activities'),
        content: Text('Delete $count selected activities?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      final provider = ref.read(activityListViewModelProvider.notifier);
      try {
        await provider.deleteSelectedActivities();
        await _fetchActivities(ref, activityPage, isLoading);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Activities deleted')),
          );
        }
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error deleting activities: $e')),
          );
        }
      }
    }
  }
}
