# LocalCam Android Companion

Android receiver app for the LocalCam macOS menu-bar webcam streamer. It connects to the Mac over a local-network WebSocket and displays JPEG frames received as binary messages.

## Build with GitHub Actions
1. Create a new GitHub repository and upload this project.
2. Push to `main` or run **Actions → Build Android APK → Run workflow**.
3. Download the `LocalCam-Android-APK` artifact from the completed workflow.
4. Install `app-debug.apk` on your Android device. You may need to allow installs from your browser or file manager.

The workflow uses a GitHub-hosted Ubuntu runner and Gradle to build the debug APK. This is suitable for testing; a production release should use a signed release APK or Android App Bundle with a protected signing key.

## Connect
1. Make sure the Android phone and Mac are on the same trusted Wi-Fi/LAN.
2. Start streaming in the LocalCam macOS menu-bar app.
3. Find the Mac's LAN IPv4 address in macOS System Settings → Network.
4. Enter the address in this app, e.g. `192.168.1.42`, then tap **Connect**.
5. The app connects to `ws://192.168.1.42:8765/` and displays incoming JPEG frames.

If it cannot connect, check that both devices are on the same network, the Mac is streaming, and the Mac firewall allows incoming connections on TCP port 8765.

## Compatibility / limitations
- Requires Android 8.0 (API 26) or newer.
- This receiver expects each WebSocket binary message to contain one JPEG image, matching the LocalCam macOS starter project.
- The connection uses unencrypted `ws://` and has no authentication. Use only on a trusted private LAN; do not port-forward the port. Add TLS and authenticated pairing before production use.
- The Android project has been generated but not built on a real Android SDK runner in this conversation. GitHub Actions is intended to perform the build and expose any compile issues.
