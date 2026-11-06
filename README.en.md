<p align="center"><img src="branding/aurora-root/icon-circle.png" width="112" alt="Xperia Clear All # root edition icon"></p>

<h1 align="center">Xperia Clear All #</h1>

<p align="center"><a href="README.md">繁體中文</a> · <strong>English</strong></p>

<p align="center">Add Clear all directly to Xperia's stock Recents action row through Vector / LSPosed.</p>

<p align="center">
  <a href="https://github.com/justinlin099/Xperia_Clear_All_Vector/releases">Download APK</a> ·
  <a href="https://github.com/justinlin099/Xperia_Clear_All_Vector/issues">Report an issue</a> ·
  <a href="https://github.com/justinlin099/Xperia_Clear_All">Looking for the non-root Shizuku edition?</a>
</p>

## Choose an edition

| | This project: Vector / LSPosed | [Non-root / Shizuku](https://github.com/justinlin099/Xperia_Clear_All) |
| --- | --- | --- |
| Requirements | Root and active Vector / LSPosed | Shizuku in wireless debugging (ADB) mode |
| Button | Inside Sony Launcher's Recents action row | Overlay with appearance and position controls |
| Setup | Enable the module and Xperia Home scope | Connect to Shizuku and grant permissions in the app |
| Icon | Aurora bars with a `#` root badge | Aurora bars |

Enable one edition at a time to avoid duplicate buttons.

## Features

- Calls Sony Launcher's native `dismissAllTasks(View)`, preserving its animations and task removal flow.
- Shares the screenshot/multi-window row, with consistent heights, centered labels/icons and rounded touch feedback.
- Hides when the task list is empty or split-screen selection is active. Guards against repeated taps during scrolling, transitions and dismissal.
- Uses the Launcher's translated label and follows appearance updates.
- Hook-only module: no settings Activity, persistent service, overlay or polling.

## Requirements and compatibility

- A rooted Sony Xperia with stock Xperia Home and [Vector](https://github.com/JingMatrix/Vector) or compatible LSPosed enabled.
- **Android 16 (API 36)** or later is required to install the APK.
- Hooks target **Xperia Home 17, versionCode 37** internals. Minimum Android version is an installation requirement, not a guarantee for every later OS/Launcher. Other versions and third-party launchers are unverified.
- Uses Legacy Xposed API with declared minimum framework API 93. libxposed API 102 is not required.

The author confirmed the previous implementation works on a physical device. Version 1.1.6's cleanup and optimizations have separate automated regression checks; see [BUILD_VALIDATION.txt](BUILD_VALIDATION.txt) for coverage and device-validation status.

## Installation

1. Download a published APK from [Releases](https://github.com/justinlin099/Xperia_Clear_All_Vector/releases), or build it below.
2. Enable **Xperia Clear All #** in Vector / LSPosed.
3. Select only **Xperia Home / `com.sonymobile.launcher`** as the scope.
4. Reboot so the Launcher loads the module.
5. Open a few apps, then enter Recents to find Clear all.

There is no launcher entry; manage the module through your framework. Shizuku, overlay permission and extra background execution permissions are not needed.

## Resource usage

The module runs only in Sony Launcher's main process, reacting to existing events. Task-state callbacks no longer restyle the row, look up resources by name or synchronize the icon. Layout and appearance events handle those tasks. Icon state changes only request a redraw when the resolved color changes.

Unused AppCompat, Material and AndroidX Core runtime dependencies were removed to reduce APK size. CPU, PSS memory and battery savings still require device measurements; APK reduction is not a battery-saving percentage. See [performance notes](docs/performance.md) for the Vector / libxposed API assessment and limits.

## Troubleshooting

**No app in the drawer:** Expected; there is no Activity. The icon appears in system app information and framework module lists that support icons.

**No button:** Check the module and Xperia Home scope, reboot, leave split selection and ensure Recents contains tasks. Search framework logs for `XPERIA_RECENTS:`. Include your Xperia Home version if the log says `unsupported Launcher`.

**Disable:** Turn the module off or uninstall it, then reboot.

## Build

Use JDK 21 or 22 and Android SDK Platform 36. Dependency downloads require network access. Gradle 8.13 Wrapper is included.

```powershell
# Windows PowerShell, with JAVA_HOME and ANDROID_HOME configured
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

```sh
# macOS / Linux
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Debug builds use the local debug key; replacing an installed APK requires a matching signature. Use your own fixed key for distribution.

See [branding/README.md](branding/README.md) for editable icon assets and regeneration.

## GitHub Actions and releases

Pushes to `main` and pull requests run tests, lint and a debug build. A manual run on `main` produces a signed APK artifact. Pushing a `v` tag matching the complete `versionName` creates a GitHub Release after checks and signature verification.

Configure the four signing Secrets in the **Vector repository** first. See the [release setup guide (Traditional Chinese)](docs/RELEASING.md) for instructions and the current tag example. The `-standalone` edition suffix alone does not mark a release as a preview.

## Reporting issues

Include the device model, Android, Xperia Home, framework and module versions, reproduction steps and relevant `XPERIA_RECENTS:` logs in an [issue](https://github.com/justinlin099/Xperia_Clear_All_Vector/issues).

## License and credits

[GNU GPLv3](LICENSE). Thanks to [Vector](https://github.com/JingMatrix/Vector), [LSPosed](https://github.com/LSPosed/LSPosed) and [XposedBridge](https://github.com/rovo89/XposedBridge). Aurora comes from the same author's [non-root project](https://github.com/justinlin099/Xperia_Clear_All).

This is a community project, not an official Sony application.
