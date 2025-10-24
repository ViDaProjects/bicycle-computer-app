import 'package:be_for_bike/l10n/app_localizations.dart';

/// Enum representing different types of activities.
enum ActivityType { cycling }

/// Extension on ActivityType to provide translated names based on the given localization.
extension ActivityTypeExtension on ActivityType {
  /// Retrieves the translated name of the activity type based on the provided localization.
  String getTranslatedName(AppLocalizations localization) {
    return localization.cycling;
  }
}
