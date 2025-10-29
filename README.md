# Be for Bike 🚴‍♂️

A comprehensive cycling computer app that combines Flutter technology and native Android to provide an extensive cycling activity tracking experience.

## 📋 About the Project

This is a project developed as part of Integration Workshop 3, demonstrating the integration between Flutter (user interface) and native Android (background services and database). The app allows cyclists to track their activities in real-time, view detailed statistics, and analyze traveled routes.

## ✨ Features

### 🏃‍♂️ Real-Time Tracking
- **Real-time GPS**: Precise location tracking during cycling
- **Live metrics**: Speed, distance, cadence, and power
- **Intuitive interface**: Clean design that's easy to use during exercise

### 📊 Activity Analysis
- **Detailed statistics**: Duration, distance traveled, average/maximum speed
- **Interactive charts**: Visualization of speed, cadence, power, and altitude
- **Complete history**: Access to all previous activities

### 🗺️ Route Visualization
- **Interactive map**: Route visualization using OpenStreetMap
- **Elevation data**: Altitude analysis along the route
- **Route export**: Ability to save and share routes

### 🔧 Technical Features
- **BLE Integration**: Connectivity with Bluetooth sensors (speedometer, cadence meter, power meter)
- **Local database**: Efficient storage using SQLite on Android
- **Seed data**: Sample data for demonstration and testing
- **Cross-platform**: Android support (iOS disabled in this project)

## 🛠️ Technologies Used

### Frontend (Flutter)
- **Framework**: Flutter 3.35.7+
- **Language**: Dart 3.9.2+
- **State Management**: Riverpod + Hooks
- **UI Components**:
  - `fl_chart`: Interactive charts
  - `flutter_map`: Maps with OpenStreetMap
  - `google_nav_bar`: Bottom navigation bar
- **Utilities**:
  - `geolocator`: Location services
  - `permission_handler`: Permission management
  - `shared_preferences`: Local storage
  - `image_picker/cropper`: Image manipulation

### Backend (Native Android)
- **Language**: Kotlin 9.1.0+
- **Java**: JDK 25+
- **Database**: SQLite with Room
- **BLE**: Bluetooth device communication
- **Services**: Background processing and statistics calculations

### Integration
- **MethodChannel**: Flutter ↔ Android communication
- **Platform Channels**: Data exchange between platforms

## 🚀 How to Run

### Prerequisites
- Flutter SDK 3.35.7 or higher
- Dart SDK 3.9.2 or higher
- Android Studio with Android SDK
- Android device or emulator
- JDK 25 or higher

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/ViDaProjects/bicycle-computer-app.git
   cd bicycle-computer-app
   ```

2. **Install dependencies**:
   ```bash
   flutter pub get
   ```

3. **Configure Android environment**:
   - Open the project in Android Studio
   - Sync Gradle
   - Configure a virtual device or connect a physical device

4. **Run the application**:
   ```bash
   flutter run
   ```

### 🧪 Running Tests

```bash
# Run all tests
flutter test

# Run tests with coverage
flutter test --coverage

# Run code analysis
flutter analyze
```

### 📱 Production Build

```bash
# Build for Android APK
flutter build apk --release

# Build for Android AAB (Google Play)
flutter build appbundle --release
```

## 📁 Project Structure

```
bicycle-computer-app/
├── android/                    # Native Android code
│   └── app/src/main/kotlin/com/beforbike/app/
│       ├── database/          # SQLite and data models
│       ├── MainActivity.kt    # Android entry point
│       └── BleServerService.kt # BLE service
├── lib/                       # Flutter code
│   ├── core/                  # Utilities and configurations
│   ├── data/                  # Data layer (repositories, APIs)
│   ├── domain/                # Business rules (entities, repositories)
│   ├── l10n/                  # Internationalization
│   └── presentation/          # User interface
│       ├── common/           # Shared components
│       ├── statistics/       # Statistics screen
│       └── settings/         # Settings
├── assets/                    # Static resources
├── test/                      # Unit tests
└── pubspec.yaml              # Flutter dependencies
```

## 🔄 Architecture

The app follows a clean architecture with clear separation of responsibilities:

- **Presentation Layer**: Flutter widgets with Riverpod for state management
- **Domain Layer**: Business entities and repository contracts
- **Data Layer**: Repository implementations and Android communication
- **Platform Layer**: Native Android code for heavy services

### Data Flow
1. **Collection**: BLE sensors → Android (SQLite)
2. **Processing**: Statistics calculations on Android
3. **Presentation**: Flutter reads data via MethodChannel
4. **Visualization**: Responsive interface with charts and maps

## 🤝 How to Contribute

1. Fork the project
2. Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 📝 Code Standards
- Follow the [Flutter Guidelines](https://flutter.dev/docs/development/tools/formatting)
- Use `flutter analyze` to check code quality
- Maintain test coverage above 80%
- Document new features in the README

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## 👥 Authors

- **Be for Bike Team** - Initial development
- **Lucas** - Maintenance and improvements

## 🙏 Acknowledgments

- Integration Workshop 3 for the opportunity
- Flutter community for exceptional documentation
- Android ecosystem contributors

---

**Note**: This project was developed as part of an integration workshop and serves as a technical demonstration of the possibilities of combining Flutter and native Android development.
