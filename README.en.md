# Lockview

[日本語](README.md)

An Android app that displays a WebView in full screen and locks the device down,
turning it into a kiosk terminal. The goal is to restrict the device **without**
using ProfileOwner or DeviceOwner<sup>1</sup>.

<sup>1</sup> Both require the app to be configured during device setup, and the aim here is
to turn an existing device into a kiosk terminal as-is.

## How to

1. Create a dedicated user for this app using the multi-user feature available since Android 5.
2. Install the app and set it as the default home app and the default browser.
3. Enable "Lockview キオスクモード" under Settings > Accessibility.
   (Required to disable the status bar and the navigation bar.)
4. Launch the app. A setup screen appears where you enter the URL to display and an
   unlock password. Saving starts kiosk mode immediately; the setup screen is not
   shown on subsequent launches.

## Feature

- [x] WebView
- [x] Handles browser callbacks (by being registered as the default browser)
- [x] Runs as a home app (disables the home button and the recents button)
- [x] Disables the volume buttons and the back key
      (the power button could not be disabled - it should be blocked physically)
- [x] Camera permission (for use inside the WebView)
- [x] Immersive mode (possibly unnecessary once the above is in place)
- [x] Lock Task (same as above)
- [x] Keeps the screen on
- [x] Blocks screenshots
- [x] DOM Storage enabled, for frameworks such as Vue
- [x] `TYPE_ACCESSIBILITY_OVERLAY` to disable the status bar and the navigation bar
- [x] Unlock shortcut using the volume buttons
- [x] Setup screen for the URL and the unlock password, and password-based unlocking

## Setup and unlocking

### First launch

Enter the URL and an unlock password (at least 4 characters). If the URL has no scheme,
`https://` is added automatically; anything other than http/https is rejected.
The back key is disabled until the setup is complete.

### Leaving kiosk mode

Press the volume buttons in the order **up, down, up, down, up** within 5 seconds and a
password prompt appears. Entering the correct password releases everything: the status
bar / navigation bar overlays, Lock Task, the key blocking, the screenshot restriction,
and the always-on screen.

The "設定を変更" (Change settings) button in that dialog opens the screen for changing the
URL and the password. It requires the password as well.

Entering the same volume sequence again while unlocked returns the device to kiosk mode.

The sequence and the time limit can be changed in `MainActivity.UNLOCK_SEQUENCE` and
`UnlockSequenceDetector.DEFAULT_TIMEOUT_MILLIS`.

### If you forget the password

Clearing the app storage from Settings > Apps > Lockview resets it to the unconfigured
state. The password is stored as a salted PBKDF2 hash, so it cannot be recovered.

## Disabling the status bar and the navigation bar

`SystemBarBlockerService` runs as an `AccessibilityService` and places
`TYPE_ACCESSIBILITY_OVERLAY` windows over the status bar and the navigation bar to swallow
touches. Because this is how the status bar pull-down is blocked without DeviceOwner, the
service has to be enabled manually from the accessibility settings. If it is disabled, the
app shows a toast about it at startup.

Note that the areas at the top and bottom of the screen, matching the height of the system
bars, swallow touches. Avoid placing interactive elements there in the page you display.

## Build

- JDK 17 or later
- Android SDK Platform 36.1 / Build Tools 36.0.0

```
./gradlew assembleDebug          # build
./gradlew test                   # local unit tests
./gradlew connectedAndroidTest   # tests on a device / emulator
```

## Release

Pushing a tag starting with `v` runs the tests and builds a release APK on GitHub Actions,
then attaches `lockview-<tag>.apk` to a GitHub Release.

```
git tag v1.0.0 && git push origin v1.0.0
```

`versionName` is the tag without the leading `v`, and `versionCode` is the workflow run
number.

To sign the APK, register the following repository secrets. Without them the build still
succeeds, but the APK is unsigned and cannot be installed on a device.

| Secret | Description |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | The keystore, base64 encoded (`base64 -w0 release.jks`) |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
