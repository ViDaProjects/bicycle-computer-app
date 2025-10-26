import 'dart:ui' as ui;

import 'package:flutter/material.dart';

/// Utility class for color-related operations.
class ColorUtils {
  static Color main = Colors.teal.shade800;
  static Color mainDarker = Colors.teal.shade900;
  static Color mainMedium = Colors.teal.shade700;
  static Color mainLight = Colors.teal.shade100;

  static Color error = Colors.red.shade600;
  static Color errorDarker = Colors.red.shade800;
  static Color errorLight = Colors.red.shade100;

  static Color warning = Colors.orange;

  static Color white = Colors.white;
  static Color black = Colors.black;
  static Color red = Colors.red;
  static Color green = Colors.green;
  static Color greenDarker = Colors.green.shade700;
  static Color transparent = Colors.transparent;
  static Color grey = Colors.grey.shade600;
  static Color greyDarker = Colors.grey.shade700;
  static Color greyLight = Colors.grey.shade300;
  static Color blueGrey = Colors.blueGrey;
  static Color blueGreyDarker = Colors.blueGrey.shade900;
  static Color backgroundLight = Colors.white;

  /// List of colors used for generating color tuples.
  static List<Color> colorList = [
    Colors.teal,
    Colors.orange,
    Colors.blueGrey,
    Colors.red
  ];

  /// Generates a darker color based on the given [baseColor].
  ///
  /// The [baseColor] is used as the reference color for generating the darker color.
  /// The darker color is determined based on the luminance of the base color.
  static Color generateDarkColor(Color baseColor) {
    final luminance = baseColor.computeLuminance();
    final darkColor =
        luminance > 0.5 ? baseColor.withValues(alpha: 0.8) : baseColor.darker();
    return darkColor;
  }

  /// Generates a lighter color based on the given [baseColor].
  ///
  /// The [baseColor] is used as the reference color for generating the lighter color.
  /// The lighter color is determined based on the luminance of the base color.
  static Color generateLightColor(Color baseColor) {
    final luminance = baseColor.computeLuminance();
    final lightColor =
        luminance > 0.5 ? baseColor.lighter() : baseColor.withValues(alpha: 0.8);
    return lightColor;
  }

  /// Generates a color tuple from the [colorList] based on the given [index].
  ///
  /// The [index] is used to select the base color from the color list.
  /// The base color is then used to generate a dark color and a light color,
  /// forming a color tuple of length 2.
  static List<Color> generateColorTupleFromIndex(int index) {
    final baseColor = colorList[index % colorList.length];
    final darkColor = generateDarkColor(baseColor);
    final lightColor = generateLightColor(baseColor);
    return [darkColor, lightColor];
  }

  /// Generates a color based on calories burned (orange to yellow gradient).
  ///
  /// Lower calories = more orange, higher calories = more yellow.
  static Color generateColorFromCalories(double calories) {
    // Normalize calories (assuming 0-1000 range, adjust as needed)
    final normalized = (calories / 1000).clamp(0.0, 1.0);

    // Interpolate between orange and yellow
    const orange = Colors.orange;
    const yellow = Color(0xFFFFD600); // Colors.yellow.shade600

    return Color.lerp(orange, yellow, normalized)!;
  }

  /// Generates a gradient based on calories burned with smooth transition from light blue to light orange.
  ///
  /// Returns a list of colors for gradient: [startColor, endColor]
  static List<Color> generateGradientFromCalories(double calories) {
    // Use wider range for more variation (0-2000 kcal)
    final normalized = (calories / 2000).clamp(0.0, 1.0);

    // Very light colors with smooth transition from light blue to light orange
    final lightBlue = Colors.blue.shade50.withValues(alpha: 0.25); // Very light blue for very low calories
    final lightBlueMedium = Colors.blue.shade100.withValues(alpha: 0.3);
    final lightCyan = Colors.cyan.shade50.withValues(alpha: 0.25); // Light cyan for low-medium calories
    final lightCyanMedium = Colors.cyan.shade100.withValues(alpha: 0.3);
    final lightGreen = Colors.lightGreen.shade50.withValues(alpha: 0.25); // Light green for medium calories
    final lightGreenMedium = Colors.lightGreen.shade100.withValues(alpha: 0.3);
    final lightYellow = Colors.yellow.shade50.withValues(alpha: 0.25); // Light yellow for medium-high calories
    final lightYellowMedium = Colors.yellow.shade100.withValues(alpha: 0.3);
    final lightOrange = Colors.orange.shade50.withValues(alpha: 0.25); // Light orange for high calories
    final lightOrangeMedium = Colors.orange.shade100.withValues(alpha: 0.3);

    // Create smooth gradient transitions based on calorie ranges
    Color startColor, endColor;
    if (normalized < 0.2) {
      // Very low calories: very light blue
      startColor = lightBlue;
      endColor = lightBlueMedium;
    } else if (normalized < 0.4) {
      // Low calories: light blue to light cyan
      startColor = Color.lerp(lightBlue, lightCyan, (normalized - 0.2) / 0.2)!;
      endColor = Color.lerp(lightBlueMedium, lightCyanMedium, (normalized - 0.2) / 0.2)!;
    } else if (normalized < 0.6) {
      // Medium-low calories: light cyan to light green
      startColor = Color.lerp(lightCyan, lightGreen, (normalized - 0.4) / 0.2)!;
      endColor = Color.lerp(lightCyanMedium, lightGreenMedium, (normalized - 0.4) / 0.2)!;
    } else if (normalized < 0.8) {
      // Medium-high calories: light green to light yellow
      startColor = Color.lerp(lightGreen, lightYellow, (normalized - 0.6) / 0.2)!;
      endColor = Color.lerp(lightGreenMedium, lightYellowMedium, (normalized - 0.6) / 0.2)!;
    } else {
      // High calories: light yellow to light orange
      startColor = Color.lerp(lightYellow, lightOrange, (normalized - 0.8) / 0.2)!;
      endColor = Color.lerp(lightYellowMedium, lightOrangeMedium, (normalized - 0.8) / 0.2)!;
    }

    return [startColor, endColor];
  }

  static Future<ImageProvider<Object>?> colorToImageProvider(Color color,
      {double width = 32.0, double height = 32.0}) async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    final paint = Paint()..color = color;
    canvas.drawRect(Rect.fromLTRB(0, 0, width, height), paint);
    final picture = recorder.endRecording();
    final img = await picture.toImage(width.toInt(), height.toInt());
    final byteData = await img.toByteData(format: ui.ImageByteFormat.png);
    if (byteData != null) {
      return MemoryImage(byteData.buffer.asUint8List());
    } else {
      return null;
    }
  }
}

/// Extension methods for the [Color] class.
extension ColorExtension on Color {
  /// Returns a darker shade of the color.
  ///
  /// The [factor] determines the darkness of the shade.
  /// A factor of 0.0 represents the same color, while a factor of 1.0 represents a fully dark color.
  Color darker([double factor = 0.1]) {
    return Color.fromARGB(
      (a * 255.0).round() & 0xff,
      (r * 255.0).round() & 0xff,
      (g * 255.0).round() & 0xff,
      (b * 255.0).round() & 0xff,
    );
  }

  /// Returns a lighter shade of the color.
  ///
  /// The [factor] determines the lightness of the shade.
  /// A factor of 0.0 represents the same color, while a factor of 1.0 represents a fully light color.
  Color lighter([double factor = 0.1]) {
    return Color.fromARGB(
      (a * 255.0).round() & 0xff,
      (r * 255.0).round() & 0xff,
      (g * 255.0).round() & 0xff,
      (b * 255.0).round() & 0xff,
    );
  }
}
