import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'l10n/app_localizations.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:stack_trace/stack_trace.dart' as stack_trace;

import 'l10n/support_locale.dart';
import 'presentation/common/core/services/text_to_speech_service.dart';
import 'presentation/common/core/utils/color_utils.dart';
import 'core/utils/audio_service.dart';
import 'presentation/home/screens/home_screen.dart';
import 'presentation/my_activities/screens/activity_list_screen.dart';
import 'presentation/new_activity/screens/sum_up_screen.dart';

/// Global navigator key to access the navigator from anywhere in the app.
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  try {
    WidgetsFlutterBinding.ensureInitialized();
    await SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
    ]);

    // Initialize audio service for button sounds
    await AudioService().initialize();

    runApp(
      const ProviderScope(child: MyApp()),
    );

    FlutterError.demangleStackTrace = (StackTrace stack) {
      if (stack is stack_trace.Trace) return stack.vmTrace;
      if (stack is stack_trace.Chain) return stack.toTrace().vmTrace;
      return stack;
    };
  } catch (e, stackTrace) {
    debugPrint('Error during app initialization: $e');
    debugPrint('Stack trace: $stackTrace');
    // Fallback: run app with minimal configuration
    runApp(
      const MaterialApp(
        home: Scaffold(
          body: Center(
            child: Text('Error initializing app. Please try again.'),
          ),
        ),
      ),
    );
  }
}

/// Provider for the theme mode.
final themeModeProvider = StateProvider<ThemeMode>((ref) {
  return ThemeMode.light;
});

/// Provider for the MyAppViewModel.
final myAppProvider = Provider((ref) {
  return MyAppViewModel(ref);
});

/// ViewModel for the main app.
class MyAppViewModel {
  final Ref ref;

  MyAppViewModel(this.ref);

  /// Initializes the app, e.g., initializes services.
  void init() async {
    ref.read(textToSpeechService).init();
    await ref.read(permissionService).requestAllPermissions();
  }

  /// Retrieves the localized configuration based on the current locale.
  Future<AppLocalizations> getLocalizedConf() async {
    final lang = ui.PlatformDispatcher.instance.locale.languageCode;
    final country = ui.PlatformDispatcher.instance.locale.countryCode;
    return await AppLocalizations.delegate.load(Locale(lang, country));
  }
}

/// The main app widget.
class MyApp extends HookConsumerWidget {
  const MyApp({super.key});

  /// Builds the MaterialApp with the provided home widget.
  MaterialApp buildMaterialApp(Widget home, ThemeMode themeMode) {
    return MaterialApp(
      initialRoute: '/',
      routes: {
        '/sumup': (context) => const SumUpScreen(),
        '/activity_list': (context) => ActivityListScreen()
      },
      navigatorKey: navigatorKey,
      title: 'Be for Bike',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        textSelectionTheme: TextSelectionThemeData(
          cursorColor: ColorUtils.main,
          selectionColor: ColorUtils.main,
          selectionHandleColor: ColorUtils.main,
        ),
        primaryColor: ColorUtils.main,
        splashColor: ColorUtils.blueGreyDarker,
        bottomSheetTheme:
            BottomSheetThemeData(backgroundColor: ColorUtils.transparent),
      ),
      darkTheme: ThemeData.dark().copyWith(
        // VS Code Dark Theme colors
        scaffoldBackgroundColor: const Color(0xFF1e1e1e),
        cardColor: const Color(0xFF252526),
        canvasColor: const Color(0xFF1e1e1e),

        // Dialog theme
        dialogTheme: const DialogThemeData(
          backgroundColor: Color(0xFF252526),
        ),

        // App bar and navigation
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF323233),
          foregroundColor: Color(0xFFcccccc),
          elevation: 0,
        ),

        // Cards and surfaces
        cardTheme: CardThemeData(
          color: const Color(0xFF252526),
          elevation: 2,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          shadowColor: Colors.black.withValues(alpha: 0.3),
        ),

        // Text colors
        textTheme: const TextTheme(
          bodyLarge: TextStyle(color: Color(0xFFcccccc)),
          bodyMedium: TextStyle(color: Color(0xFFcccccc)),
          titleMedium: TextStyle(color: Color(0xFFcccccc)),
          titleSmall: TextStyle(color: Color(0xFFcccccc)),
          labelLarge: TextStyle(color: Color(0xFFcccccc)),
        ),

        // Input and selection
        textSelectionTheme: TextSelectionThemeData(
          cursorColor: ColorUtils.main,
          selectionColor: ColorUtils.main.withValues(alpha: 0.3),
          selectionHandleColor: ColorUtils.main,
        ),

        // Buttons and interactions
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF0e639c),
            foregroundColor: const Color(0xFFcccccc),
          ),
        ),

        // Icons
        iconTheme: const IconThemeData(
          color: Color(0xFFcccccc),
        ),

        // Dividers and borders
        dividerColor: const Color(0xFF454545),

        // Expansion tiles
        expansionTileTheme: const ExpansionTileThemeData(
          backgroundColor: Color(0xFF252526),
          collapsedBackgroundColor: Color(0xFF252526),
          textColor: Color(0xFFcccccc),
          collapsedTextColor: Color(0xFFcccccc),
          iconColor: Color(0xFFcccccc),
          collapsedIconColor: Color(0xFFcccccc),
        ),

        primaryColor: ColorUtils.main,
        splashColor: ColorUtils.blueGreyDarker,
        bottomSheetTheme:
            const BottomSheetThemeData(backgroundColor: Color(0xFF252526)),
      ),
      themeMode: themeMode,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: L10n.support,
      locale: const Locale('en'), // Force English language
      home: home,
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = ref.read(myAppProvider);
    final themeMode = ref.watch(themeModeProvider);

    provider.init();

    return buildMaterialApp(const HomeScreen(), themeMode);
  }
}
