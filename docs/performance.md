# Resource usage and Vector API assessment

Reviewed on 2026-11-06. Implementation changes are distinct from measured device savings.

## Changes in 1.1.6

- Load hooks only in Sony Launcher's main process, skipping package subprocesses.
- Separate task-state handling from `ActionRowLayout`. Previously, `refresh()` performed resource-name lookup, view lookup, icon synchronization, row styling and content measurement on every disabled/hidden/task callback. It now handles task availability, split selection and enabled/visible state only.
- Reuse the screenshot button reference for configuration and screenshot-drawable appearance updates. References stay on the injected view, without a global view cache.
- Keep the injected icon when its slot, dimensions and color-state list are unchanged; duplicate appearance callbacks do not create another drawable.
- Recalculate geometry when row/container bounds change and on appearance updates. The row listener handles visible-button changes. Rotation and empty/split transitions use existing events, without polling or posting tasks on every state callback.
- Keep backgrounds and layout parameters when unchanged. Repeated updates must settle without growing padding, recreating ripple backgrounds or requesting more layouts.
- Resolve glyph color on actual state-color changes. States that resolve to the same color, and unchanged alpha assignments, do not invalidate it.
- Remove unused AppCompat, Material and AndroidX Core runtime dependencies and their unused themes. Injected views use framework widgets and the Launcher's own context/style.

The existing one-shot 1.2-second click guard remains. It is posted only after a dismissal attempt, uses the view's main-thread queue and acquires no wake lock. There are no periodic timers, services, alarms, network calls or framework/system-server hooks.

## Why keep the Legacy API?

[Vector's official README](https://github.com/JingMatrix/Vector#developer-resources) supports both Legacy Xposed and modern libxposed APIs and advises matching modern API commits to the framework build.

The current [libxposed API](https://github.com/libxposed/api) is 102.0.0. Its [API reference](https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.html) documents process-scoped loading in API 101 and hot reload, lifecycle callback control and atomic hook replacement in API 102. Modules targeting API 102 cannot call the legacy `de.robv.android.xposed` API.

**Assessment:** these features may help future development, but the cited documentation does not establish a battery advantage for this module. Migration would change its entry/lifecycle, metadata, hook implementation and framework requirements. This release keeps the device-validated loading model and reduces avoidable callback work instead. No undocumented power-saving API is assumed.

Vector's [Legacy XposedHelpers source](https://github.com/JingMatrix/Vector/blob/master/legacy/src/main/java/de/robv/android/xposed/XposedHelpers.java) already caches reflected fields and methods. Repeated resource/view/layout work and drawable invalidations are more concrete targets here than a second general reflection cache.

## Validation and limits

Native-graphics Robolectric tests retain the single-line, wrapped-label and Chinese-label centering regressions. Additional checks cover stable repeated row updates, rotation/width limits, hiding/restoring a sibling, larger fonts, drawable invalidation/color/alpha and adaptive/themed icon resources.

Tests cannot establish actual Sony Hook timing or prove that all OEM configuration paths use the observed callbacks. After installing the update, check portrait/landscape, light/dark appearance, font size/language changes, empty/non-empty lists and split selection on the device.

APK sizes and checks are recorded in [BUILD_VALIDATION.txt](../BUILD_VALIDATION.txt). APK size is not PSS memory, CPU time or energy consumption. No measured RAM reduction or battery-saving percentage is claimed.

For a fair device comparison, keep the same phone, Launcher/framework build, brightness and task set. Compare Launcher CPU/frames and `dumpsys meminfo com.sonymobile.launcher` after repeated Recents entry, scrolling, rotation and dismissal; compare idle separately. Account for Vector's shared framework overhead separately from the module's incremental cost.

## Local APK comparison

Same local toolchain, decimal bytes; debug builds:

| Build | APK size |
| --- | ---: |
| Original 1.1.5 | 13,573,005 bytes |
| Updated 1.1.6 | 2,383,979 bytes |

The debug APK is **82.44% smaller**. The unsigned release is 2,053,017 bytes; no release baseline was measured. These numbers do not measure memory or power use.
