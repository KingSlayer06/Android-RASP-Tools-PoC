# AndroidRASP-Android — Run Results

Run date: 2026-11-21 · Library: `com.securevale:rasp-android:0.7.1` (unchanged)

## Environment
- JDK: Azul Zulu 17.0.17 (`JAVA_HOME=$(/usr/libexec/java_home)`; the shell's JAVA_HOME was set to the invalid literal `/usr/libexec/java_home`)
- Android SDK: `~/Library/Android/sdk`, Gradle 8.11.1 wrapper
- Device: `emulator-5554` (AVD `Pixel_9`, shared) — model `sdk_gphone16k_arm64`, Android 17 / API 37, ABI `arm64-v8a`, 16 KB page size

## Changes made to the project
- Created `local.properties` with `sdk.dir=/Users/himanshu/Library/Android/sdk`
- `chmod +x gradlew`
- **No source or build-script changes**: the build compiled cleanly against 0.7.1.

## Build
`./gradlew --no-daemon :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest` → **BUILD SUCCESSFUL in 2m 40s** (116 tasks).

## Native libs in APKs
```
== app-debug.apk
   483792  01-01-1981 01:01   lib/arm64-v8a/libnative.so
   348612  01-01-1981 01:01   lib/armeabi-v7a/libnative.so
   517040  01-01-1981 01:01   lib/x86/libnative.so
   535944  01-01-1981 01:01   lib/x86_64/libnative.so
== app-release.apk (R8-minified)
   483792  01-01-1981 01:01   lib/arm64-v8a/libnative.so
   348612  01-01-1981 01:01   lib/armeabi-v7a/libnative.so
   517040  01-01-1981 01:01   lib/x86/libnative.so
   535944  01-01-1981 01:01   lib/x86_64/libnative.so
```
All four ABIs are present in both APKs. On device, `libnative.so` loaded from `base.apk!/lib/arm64-v8a` (uncompressed, so it loaded straight from the APK) and the 16 KB-page emulator accepted it.

## Instrumented tests (`:app:connectedDebugAndroidTest`): 11/11 passed, 0 failed, 0 skipped
| Test | Result | Time (s) |
|---|---|---|
| checkOnlyFor_emulatorSubset_onEmulator_findsAvd | PASS | 1.814 |
| emulatorOnly_onEmulator_reportsEmulatorFound | PASS | 0.849 |
| rootOnly_checkOnlyFor_runsExactlyTheRootChecks | PASS | 2.485 |
| checkOnlyFor_subset_overridesEnabledCategories | PASS | 1.107 |
| vulnerabilitiesOnly_returnsOnlyPositives | PASS | 7.064 |
| debuggerOnly_onDebuggableApk_reportsDebuggerEnabled | PASS | 0.042 |
| mergedSubscribe_emitsOneResultPerCategory | PASS | 1.464 |
| allCategoriesDisabled_isSecure | PASS | 0.001 |
| categoryToggle_emulatorOff_neverReturnsEmulatorFound | PASS | 0.001 |
| granularToggles_emitOnlyEnabledCategories | PASS | 0.221 |
| fullReport_runsWithoutCrashing | PASS | 4.601 |

## Logcat: Debug APK — READ_PHONE_STATE not granted
```
--------- beginning of main
11-21 11:16:17.430  7620  7620 I RASP-PoC: SecureApp.init() OK, native library loaded
11-21 11:16:17.690  7620  7638 I RASP-PoC: Device: Google sdk_gphone16k_arm64 (API 37, arm64-v8a)
11-21 11:16:17.690  7620  7638 I RASP-PoC: APK debuggable: true | READ_PHONE_STATE granted: false
11-21 11:16:17.715  7620  7638 I RASP-PoC: [1] check() one-shot, all categories (checkEmulator=true, checkDebugger=true, checkRoot=true): EmulatorFound (25ms)
11-21 11:16:17.716  7620  7638 I RASP-PoC: [2] check() one-shot, per-category toggles (checkEmulator=false, checkDebugger=true, checkRoot=true): DebuggerEnabled (1ms)
11-21 11:16:17.716  7620  7638 I RASP-PoC: [3] subscribe(granular=false) merged per category, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:16:17.875  7620  7638 I RASP-PoC:    Emulator: EmulatorCheck -> vulnerable=false
11-21 11:16:17.875  7620  7638 I RASP-PoC:    Debugger: DebuggerCheck -> vulnerable=true
11-21 11:16:17.875  7620  7638 I RASP-PoC:    Root: RootCheck -> vulnerable=false
11-21 11:16:17.875  7620  7638 I RASP-PoC: [4] subscribe(granular=true) every sub-check, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:16:18.261  7620  7638 I RASP-PoC:    Debugger: Debuggable -> vulnerable=true
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Debugger: DebugField -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Debugger: DebuggerConnected -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:16:18.263  7620  7638 I RASP-PoC: [5] Root-only: (checkEmulator=false, checkDebugger=false, checkRoot=true) + checkOnlyFor = 7 root checks:
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:16:18.612  7620  7638 I RASP-PoC: [6] All categories enabled, but checkOnlyFor = [SuUser, RootApps, WritablePaths]:
11-21 11:16:18.891  7620  7638 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:18.891  7620  7638 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:18.891  7620  7638 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:18.891  7620  7638 I RASP-PoC: [7] subscribeVulnerabilitiesOnly(granular=true), all categories (only positives):
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Emulator: AvdDevice -> vulnerable=true
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Emulator: AvdHardware -> vulnerable=true
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Emulator: GoogleEmulator -> vulnerable=true
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Emulator: Sensors -> vulnerable=true
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Emulator: RadioVersion -> vulnerable=true
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Emulator: SuspiciousProperties -> vulnerable=true
11-21 11:16:19.150  7620  7638 I RASP-PoC:    Debugger: Debuggable -> vulnerable=true
```

## Logcat: Debug APK — READ_PHONE_STATE granted
```
--------- beginning of main
11-21 11:16:32.264  8094  8094 I RASP-PoC: SecureApp.init() OK, native library loaded
11-21 11:16:32.550  8094  8111 I RASP-PoC: Device: Google sdk_gphone16k_arm64 (API 37, arm64-v8a)
11-21 11:16:32.550  8094  8111 I RASP-PoC: APK debuggable: true | READ_PHONE_STATE granted: true
11-21 11:16:32.565  8094  8111 I RASP-PoC: [1] check() one-shot, all categories (checkEmulator=true, checkDebugger=true, checkRoot=true): EmulatorFound (14ms)
11-21 11:16:32.565  8094  8111 I RASP-PoC: [2] check() one-shot, per-category toggles (checkEmulator=false, checkDebugger=true, checkRoot=true): DebuggerEnabled (0ms)
11-21 11:16:32.565  8094  8111 I RASP-PoC: [3] subscribe(granular=false) merged per category, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:16:32.708  8094  8111 I RASP-PoC:    Emulator: EmulatorCheck -> vulnerable=false
11-21 11:16:32.708  8094  8111 I RASP-PoC:    Debugger: DebuggerCheck -> vulnerable=true
11-21 11:16:32.708  8094  8111 I RASP-PoC:    Root: RootCheck -> vulnerable=false
11-21 11:16:32.708  8094  8111 I RASP-PoC: [4] subscribe(granular=true) every sub-check, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:16:32.730  8094  8111 I RASP-PoC:    Debugger: Debuggable -> vulnerable=true
11-21 11:16:32.731  8094  8111 I RASP-PoC:    Debugger: DebugField -> vulnerable=false
11-21 11:16:32.731  8094  8111 I RASP-PoC:    Debugger: DebuggerConnected -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:16:32.732  8094  8111 I RASP-PoC: [5] Root-only: (checkEmulator=false, checkDebugger=false, checkRoot=true) + checkOnlyFor = 7 root checks:
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:32.847  8094  8111 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:16:32.848  8094  8111 I RASP-PoC: [6] All categories enabled, but checkOnlyFor = [SuUser, RootApps, WritablePaths]:
11-21 11:16:32.858  8094  8111 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:32.858  8094  8111 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:32.858  8094  8111 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:32.858  8094  8111 I RASP-PoC: [7] subscribeVulnerabilitiesOnly(granular=true), all categories (only positives):
11-21 11:16:33.542  8094  8111 I RASP-PoC:    Emulator: AvdDevice -> vulnerable=true
11-21 11:16:33.542  8094  8111 I RASP-PoC:    Emulator: AvdHardware -> vulnerable=true
11-21 11:16:33.542  8094  8111 I RASP-PoC:    Emulator: GoogleEmulator -> vulnerable=true
11-21 11:16:33.552  8094  8111 I RASP-PoC:    Emulator: Sensors -> vulnerable=true
11-21 11:16:33.552  8094  8111 I RASP-PoC:    Emulator: RadioVersion -> vulnerable=true
11-21 11:16:33.552  8094  8111 I RASP-PoC:    Emulator: SuspiciousProperties -> vulnerable=true
11-21 11:16:33.552  8094  8111 I RASP-PoC:    Debugger: Debuggable -> vulnerable=true
```

## Logcat: Release APK (non-debuggable, R8) — not granted
```
--------- beginning of main
11-21 11:16:54.661  8351  8351 I RASP-PoC: SecureApp.init() OK, native library loaded
11-21 11:16:54.876  8351  8367 I RASP-PoC: Device: Google sdk_gphone16k_arm64 (API 37, arm64-v8a)
11-21 11:16:54.876  8351  8367 I RASP-PoC: APK debuggable: false | READ_PHONE_STATE granted: false
11-21 11:16:54.884  8351  8367 I RASP-PoC: [1] check() one-shot, all categories (checkEmulator=true, checkDebugger=true, checkRoot=true): EmulatorFound (7ms)
11-21 11:16:54.977  8351  8367 I RASP-PoC: [2] check() one-shot, per-category toggles (checkEmulator=false, checkDebugger=true, checkRoot=true): Secure (94ms)
11-21 11:16:54.977  8351  8367 I RASP-PoC: [3] subscribe(granular=false) merged per category, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:16:55.028  8351  8367 I RASP-PoC:    Emulator: EmulatorCheck -> vulnerable=false
11-21 11:16:55.028  8351  8367 I RASP-PoC:    Debugger: DebuggerCheck -> vulnerable=false
11-21 11:16:55.028  8351  8367 I RASP-PoC:    Root: RootCheck -> vulnerable=false
11-21 11:16:55.028  8351  8367 I RASP-PoC: [4] subscribe(granular=true) every sub-check, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Debugger: Debuggable -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Debugger: DebugField -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Debugger: DebuggerConnected -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:16:55.081  8351  8367 I RASP-PoC: [5] Root-only: (checkEmulator=false, checkDebugger=false, checkRoot=true) + checkOnlyFor = 7 root checks:
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:16:55.111  8351  8367 I RASP-PoC: [6] All categories enabled, but checkOnlyFor = [SuUser, RootApps, WritablePaths]:
11-21 11:16:55.145  8351  8367 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:16:55.145  8351  8367 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:16:55.145  8351  8367 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:16:55.145  8351  8367 I RASP-PoC: [7] subscribeVulnerabilitiesOnly(granular=true), all categories (only positives):
11-21 11:16:55.357  8351  8367 I RASP-PoC:    Emulator: AvdDevice -> vulnerable=true
11-21 11:16:55.357  8351  8367 I RASP-PoC:    Emulator: AvdHardware -> vulnerable=true
11-21 11:16:55.357  8351  8367 I RASP-PoC:    Emulator: GoogleEmulator -> vulnerable=true
11-21 11:16:55.357  8351  8367 I RASP-PoC:    Emulator: Sensors -> vulnerable=true
11-21 11:16:55.357  8351  8367 I RASP-PoC:    Emulator: RadioVersion -> vulnerable=true
11-21 11:16:55.357  8351  8367 I RASP-PoC:    Emulator: SuspiciousProperties -> vulnerable=true
```

## Logcat: Release APK (non-debuggable, R8) — granted
```
--------- beginning of main
11-21 11:17:09.143  8497  8497 I RASP-PoC: SecureApp.init() OK, native library loaded
11-21 11:17:09.259  8497  8512 I RASP-PoC: Device: Google sdk_gphone16k_arm64 (API 37, arm64-v8a)
11-21 11:17:09.259  8497  8512 I RASP-PoC: APK debuggable: false | READ_PHONE_STATE granted: true
11-21 11:17:09.262  8497  8512 I RASP-PoC: [1] check() one-shot, all categories (checkEmulator=true, checkDebugger=true, checkRoot=true): EmulatorFound (2ms)
11-21 11:17:09.430  8497  8512 I RASP-PoC: [2] check() one-shot, per-category toggles (checkEmulator=false, checkDebugger=true, checkRoot=true): Secure (169ms)
11-21 11:17:09.430  8497  8512 I RASP-PoC: [3] subscribe(granular=false) merged per category, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:17:09.461  8497  8512 I RASP-PoC:    Emulator: EmulatorCheck -> vulnerable=false
11-21 11:17:09.461  8497  8512 I RASP-PoC:    Debugger: DebuggerCheck -> vulnerable=false
11-21 11:17:09.461  8497  8512 I RASP-PoC:    Root: RootCheck -> vulnerable=false
11-21 11:17:09.461  8497  8512 I RASP-PoC: [4] subscribe(granular=true) every sub-check, toggles (checkEmulator=false, checkDebugger=true, checkRoot=true):
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Debugger: Debuggable -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Debugger: DebugField -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Debugger: DebuggerConnected -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:17:09.502  8497  8512 I RASP-PoC: [5] Root-only: (checkEmulator=false, checkDebugger=false, checkRoot=true) + checkOnlyFor = 7 root checks:
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: TestTags -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: EngBuild -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: RootCloakingApps -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC:    Root: SuspiciousProperties -> vulnerable=false
11-21 11:17:09.524  8497  8512 I RASP-PoC: [6] All categories enabled, but checkOnlyFor = [SuUser, RootApps, WritablePaths]:
11-21 11:17:09.540  8497  8512 I RASP-PoC:    Root: SuUser -> vulnerable=false
11-21 11:17:09.540  8497  8512 I RASP-PoC:    Root: RootApps -> vulnerable=false
11-21 11:17:09.540  8497  8512 I RASP-PoC:    Root: WritablePaths -> vulnerable=false
11-21 11:17:09.540  8497  8512 I RASP-PoC: [7] subscribeVulnerabilitiesOnly(granular=true), all categories (only positives):
11-21 11:17:09.955  8497  8512 I RASP-PoC:    Emulator: AvdDevice -> vulnerable=true
11-21 11:17:09.955  8497  8512 I RASP-PoC:    Emulator: AvdHardware -> vulnerable=true
11-21 11:17:09.955  8497  8512 I RASP-PoC:    Emulator: GoogleEmulator -> vulnerable=true
11-21 11:17:09.955  8497  8512 I RASP-PoC:    Emulator: Sensors -> vulnerable=true
11-21 11:17:09.955  8497  8512 I RASP-PoC:    Emulator: RadioVersion -> vulnerable=true
11-21 11:17:09.955  8497  8512 I RASP-PoC:    Emulator: SuspiciousProperties -> vulnerable=true
```

## Observations
- **Debugger checks work as expected.** In the debug build, `Debuggable` is the only debugger flag (`DebuggerEnabled` in [2], `DebuggerCheck=true` in [3]). The release build reports `Debuggable=false` and [2] returns `Secure`. `DebugField` and `DebuggerConnected` are false in both builds. Under R8, the JNI and native checks still ran with no crash or missing-method error, so the consumer keep rules work.
- **Emulator detection:** `EmulatorFound` via AvdDevice, AvdHardware, GoogleEmulator, Sensors, RadioVersion and SuspiciousProperties.
- **Granting READ_PHONE_STATE changed nothing.** RadioVersion is already positive without it, and no other result changed.
- **Root:** every root sub-check is false (Google APIs image, not rooted).
- **Config surfaces behave as expected:** category toggles (emulator off → no emulator results in [3]/[4]), `checkOnlyFor` limits the checks that run ([5], [6]), and `subscribeVulnerabilitiesOnly` returns only positive findings ([7]).
- Screenshots: `rasp-debug.png` (debug, granted) and `rasp-release.png` (release, granted).

## Process notes
- The first debug launch after install was captured too early. Its logcat came out empty, and the run spilled into the next capture. I re-captured both debug runs with a wait-until-the-report-finishes loop. The logs above are the clean re-runs.
- Captures used `adb logcat -c` on the shared emulator-5554 before the coordinator asked not to. The only app installed, uninstalled or touched was `com.poc.androidrasp`, which is still installed as the release build.
