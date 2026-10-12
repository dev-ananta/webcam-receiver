# LocalCam Android Receiver

Android companion app for [`dev-ananta/local-cam-menu-bar`](https://github.com/dev-ananta/local-cam-menu-bar). It connects to your Mac over local-network WebSocket and renders incoming JPEG webcam frames in real time.

## Prerequisites

- Android Studio (Ladybug or newer recommended)
- Android SDK Platform 35 + Build Tools 35.0.0
- JDK 17
- Android 8.0+ receiver device (API 26+)
- Mac and Android device on the same trusted LAN/Wi-Fi

## Streaming protocol compatibility

This app is implemented to match the current `local-cam-menu-bar` baseline protocol:

- Transport: `ws://` (WebSocket over local network)
- Default endpoint: `ws://<Mac-LAN-IP>:8765/`
- Message format: each **binary** WebSocket message is one complete JPEG frame
- Authentication/TLS: not provided by the current companion macOS starter app

If the macOS server protocol changes in the future, update the receiver accordingly.

## Build locally

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run and connect

1. Start streaming from the macOS menu bar app.
2. Open **LocalCam Receiver** on Android.
3. Enter the Mac LAN IP (or host) and port (default `8765`).
4. Tap **Connect**.
5. Use **Disconnect** to stop, or **Reconnect** for manual reconnect.

The app remembers your last-used host and port.

## Connection behavior

- States: disconnected, connecting, connected, reconnecting, failed
- Automatic retry: capped backoff retries after failures
- Manual reconnect supported at any time
- Bounded frame queue: stale frames are dropped to keep latency low
- Malformed/oversized frames are rejected with user-visible errors

## Troubleshooting

- Ensure the Mac app is actively streaming.
- Confirm both devices are on the same LAN and VPN is not isolating traffic.
- Verify host/port are correct and Mac firewall allows inbound TCP `8765`.
- If you see repeated reconnects, check Wi-Fi stability and whether the Mac app stopped streaming.
- If preview is blank but connected, verify the sender is transmitting JPEG binary frames.

## Security limitations

- Traffic is unencrypted `ws://` by current protocol baseline.
- No authentication is currently used by the companion starter server.
- Use only on trusted private networks.
- Do **not** expose or port-forward the stream endpoint to the public internet.

## GitHub Actions CI

Workflow: `.github/workflows/build.yml`

On every push, pull request, and manual run, CI:

1. Sets up JDK 17 + Android SDK
2. Runs `testDebugUnitTest`
3. Builds debug APK (`assembleDebug`)
4. Uploads debug APK artifact: **LocalCam-Android-debug-apk**

Optional signed release build is enabled when these repository secrets are set:

- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

When present, CI builds `assembleRelease` and uploads **LocalCam-Android-release-apk**.
