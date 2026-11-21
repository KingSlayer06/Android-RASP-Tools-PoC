# AndroidRASP-Android

Proof of concept for **securevale Android RASP** (`com.securevale:rasp-android:0.7.1`, https://github.com/securevale/android-rasp).
Kotlin, minSdk 24, compile/target SDK 35, AGP 8.9.0, Gradle 8.11.1.

## What is wired up

| File | Purpose |
|---|---|
| `RaspPocApp.kt` | `SecureApp.init()` in `Application.onCreate()` (loads the Rust `libnative.so`) |
| `RaspRunner.kt` | Every config surface the library has, in one place, logged under the `RASP-PoC` tag |
| `MainActivity.kt` | Switches for `checkEmulator` / `checkDebugger` / `checkRoot`, a run button, a READ_PHONE_STATE button |
| `RaspChecksTest.kt` | 11 instrumented tests that prove each config option changes what runs |
| `AndroidManifest.xml` | `<queries>` block for the 60 package names the native checks look up (needed on Android 11+, the library doesn't declare it) |

Report sections (`adb logcat -s RASP-PoC`):

1. `check()` one-shot, all categories
2. `check()` one-shot, with the per-category switches
3. `subscribe(granular = false)`: one merged result per category
4. `subscribe(granular = true)`: one result per sub-check
5. Root only: `checkRoot = true` plus `checkOnlyFor` = all 7 root checks
6. All categories on, but `checkOnlyFor = [SuUser, RootApps, WritablePaths]`
7. `subscribeVulnerabilitiesOnly(granular = true)`: positives only

## Result (Pixel 9 AVD, API 37, arm64, 16 KB pages)

* Debug, release (R8) and androidTest APKs built on the first try. `libnative.so` is bundled for arm64-v8a, armeabi-v7a, x86 and x86_64.
* 11/11 instrumented tests pass.
* Debug APK: `check()` gives `EmulatorFound`. With emulator checks off it gives `DebuggerEnabled`. Six emulator sub-checks fire (AvdDevice, AvdHardware, GoogleEmulator, Sensors, RadioVersion, SuspiciousProperties). No root flags.
* Release APK: the debugger checks go clean (`Secure` with emulator checks off). R8 does not break the JNI layer.
* Granting READ_PHONE_STATE changed nothing on this AVD.

Full logs: `RUN_RESULTS.md`, `rasp-debug.png`, `rasp-release.png`.

## Is it configurable? Yes, at the code level.

**What you can configure**
* Turn whole categories on or off: `SecureAppChecker.Builder(ctx, checkEmulator, checkDebugger, checkRoot)`.
* Choose individual checks with `checkOnlyFor`, from 27 sub-checks: 3 debugger, 17 emulator and 7 root. This overrides the enabled categories (test 6).
* Pick a run mode: `check()` returns one `Result`, `subscribe()` returns every result, and `subscribeVulnerabilitiesOnly()` returns positives only. Each takes `granular` (per sub-check or per category).
* Turn on the library's own Logcat output with `.debug()`.
* Unlock the advanced emulator checks with the READ_PHONE_STATE runtime permission.

**What you can't configure**
* Detection weights and thresholds are hard-coded (root threshold is 10, and TestTags alone weighs 3). You can't tune sensitivity.
* `check()` ignores `checkOnlyFor`. Only the subscribe modes accept it.
* The package, file and property lists live in the Rust code. Changing them means rebuilding `libnative.so`.
* There is no built-in reaction (kill, block, report) and no remote or policy config. The app decides what to do with each result.
* Coverage is only emulator, debugger and root. There is no Frida or hooking detection, and no tamper, repackaging or signature checks. The one exception: Xposed/EdXposed installer apps are flagged as root apps by package name.
