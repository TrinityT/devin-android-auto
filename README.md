# Android Auto Hello World

A simple Android Auto Hello World application demonstrating the basic setup for an Android Auto car app.

## Project Structure

- `MainActivity.java` - Main Android activity for the phone app
- `HelloCarAppService.java` - Car App Service that handles Android Auto integration
- `HelloCarAppSession.java` - Session handler for the car app
- `HelloScreen.java` - Main screen displayed in Android Auto

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API Level 34
- Minimum SDK: API Level 23
- Target SDK: API Level 34

## Setup Instructions

1. **Open the Project**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `AndroidAutoHelloWorld` directory

2. **Sync Gradle**
   - Android Studio will automatically prompt to sync Gradle
   - Click "Sync Now" or wait for automatic sync

3. **Build the Project**
   - Click Build > Make Project
   - Or press Ctrl+F9 (Windows) or Cmd+F9 (Mac)

4. **Run on Device**
   - Connect an Android device with Android 6.0 (API 23) or higher
   - Enable USB Debugging on the device
   - Click Run > Run 'app'
   - Or press Shift+F10 (Windows) or Ctrl+R (Mac)

## Testing Android Auto

To test the Android Auto functionality:

1. **Desktop Head Unit (DHU)**
   - Download the Desktop Head Unit from [Android Auto Developer Site](https://developers.google.com/android/auto/desktop-head-unit)
   - Follow the setup instructions to connect your phone to DHU
   - The app will appear in the Android Auto launcher

2. **On a Real Car Display**
   - Connect your phone to a car with Android Auto support
   - The app should appear in the car's display

## Key Components

### Car App Service
The `HelloCarAppService` extends `CarAppService` and is the entry point for the Android Auto app. It creates a session when the app is launched.

### Session
The `HelloCarAppSession` manages the app session and creates screens for the car display.

### Screen
The `HelloScreen` displays a simple "Hello World!" message using the `MessageTemplate` with a button.

## Dependencies

- `androidx.car.app:app:1.4.0` - Core Android Auto library
- `androidx.car.app:app-projected:1.4.0` - Projected mode for phone screens

## Customization

To customize the app:
- Modify `HelloScreen.java` to change the displayed content
- Update `activity_main.xml` for the phone UI
- Add more screens to the session for navigation

## License

This is a sample project for educational purposes.
