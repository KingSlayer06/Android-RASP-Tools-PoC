# Android RASP Tools PoC

Proof-of-concept integrations of five Android RASP (Runtime Application Self-Protection) tools. Each tool has its own minimal Android project. The goal was to answer two questions per tool:

1. **Does it work?** It builds, runs on a device, and its checks actually fire.
2. **Is it configurable?** Can you choose which checks run, tune them, and decide how the app reacts?

All projects were built on macOS with JDK 17 and run on a Pixel 9 emulator (Android 17 / API 37, arm64, not rooted) in November 2026.

## At a glance

| Tool | Project folder | What it protects against | Works? | Configurable? |
|---|---|---|---|---|
| [RootBeer](https://github.com/scottyab/rootbeer) 0.1.2 | `RootBeer-Android/` | Root only | Yes | **Yes.** 11 root checks callable individually. Root only. |
| [freeRASP](https://github.com/talsec/Free-RASP-Android) 20.0.1 | `freeRASP-Android/` | Root, hooks, debugger, emulator, tampering, store, screen capture, device state | Yes | **Partly.** You configure the reactions, not which checks run. Telemetry always on. Closed core. |
| [Android RASP](https://github.com/securevale/android-rasp) (securevale) 0.7.1 | `AndroidRASP-Android/` | Root, emulator, debugger | Yes | **Yes, fully.** Per category plus 27 individual checks via `checkOnlyFor`. |
| MobShield (`io.github.inforaam`) 1.0.10 | `MobShield-Android/` | Root, hooks, debugger, environment, integrity | Yes | **Yes, the most tunable.** Modules, signal weights, thresholds, scan interval, response policy. |
| [dpt-shell](https://github.com/luoyesiqiu/dpt-shell) 2.22.1 (patched) | `DptShell-Android/` | DEX hollowing plus anti-debug, Frida, libc CRC, signature verify | Yes, after patching | **Yes.** Each runtime check is an independent pack-time flag. |

Each project folder has its own results (README, run logs, screenshots). dpt-shell's full write-up is in `DptShell-Android/dpt-shell-PoC-report.md`.

## Folder layout

```
Android RASP Tools PoC/
├── README.md               ← this file
├── .gitignore              ← shared rules for all five projects
├── RootBeer-Android/
├── freeRASP-Android/
├── AndroidRASP-Android/
├── MobShield-Android/
└── DptShell-Android/       ← demo app + patched packer scripts + report
```

Open any project folder directly in Android Studio. Set the Gradle JDK to **17** (Android Studio's bundled JDK 25 is rejected by some of the Gradle versions used here).

---

# Integrating each tool into an existing project

## 1. RootBeer (root detection)

**Requirements:** minSdk 21+, `mavenCentral()` in your repositories.

**Step 1. Add the dependency** in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.scottyab:rootbeer-lib:0.1.2")
}
```

**Step 2. Declare the root-app packages** in `AndroidManifest.xml`. Without this, the app-list checks always return "clean" on Android 11+ because of package visibility rules. The full list is in `RootBeer-Android/app/src/main/AndroidManifest.xml`.

```xml
<manifest ...>
    <queries>
        <!-- root management apps -->
        <package android:name="com.topjohnwu.magisk" />
        <package android:name="eu.chainfire.supersu" />
        <package android:name="com.noshufou.android.su" />
        <package android:name="com.koushikdutta.superuser" />
        <!-- ...plus the dangerous / cloaking apps from the PoC manifest -->
    </queries>
    ...
</manifest>
```

**Step 3. Run the check off the UI thread** (some checks touch the file system and JNI):

```kotlin
val rootBeer = RootBeer(context)

// Simple: the library's own bundle of checks
val rooted = rootBeer.isRooted            // or isRootedWithBusyBoxCheck

// Or pick exactly the checks you want
val suFound     = rootBeer.checkForSuBinary()
val magisk      = rootBeer.checkForMagiskBinary()
val rootApps    = rootBeer.detectRootManagementApps(arrayOf("com.my.extra.rootapp"))
val testKeys    = rootBeer.detectTestKeys()
val rwSystem    = rootBeer.checkForRWPaths()
val nativeSu    = rootBeer.checkForRootNative()
```

All 11 individual checks: `detectRootManagementApps`, `detectPotentiallyDangerousApps`, `detectRootCloakingApps`, `detectTestKeys`, `checkForDangerousProps`, `checkForBusyBoxBinary`, `checkForSuBinary`, `checkSuExists`, `checkForRWPaths`, `checkForRootNative`, `checkForMagiskBinary`. The PoC wraps them in `RootBeerChecks.kt`.

**Step 4. Decide the response.** RootBeer only returns booleans. Blocking, warning or reporting is up to your app.

**Configurability: yes.** No config object, but every check is a public method you can call on its own, and the three package-list checks accept extra package names. It only covers root.

---

## 2. freeRASP (broad RASP by Talsec)

**Requirements:** minSdk 23+, internet access during the build (the Talsec plugin downloads the SDK).

**Step 1. Add the Talsec repositories** in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google(); mavenCentral(); gradlePluginPortal()
        maven { url = uri("https://europe-west3-maven.pkg.dev/talsec-artifact-repository/plugin") }
    }
}
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven { url = uri("https://europe-west3-maven.pkg.dev/talsec-artifact-repository/common") }
        maven { url = uri("https://europe-west3-maven.pkg.dev/talsec-artifact-repository/freerasp") }
    }
}
```

**Step 2. Apply the plugin** in `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("app.talsec.plugin") version "1.1.0"
}

android {
    defaultConfig { minSdk = 23 }
    talsec {
        sdkVersion = "20.0.1"
    }
}
```

**Step 3. Get your release signing hash.** Run `./gradlew signingReport`, take the release SHA-256, and convert it to Base64 (freeRASP expects Base64, not hex).

**Step 4. Start freeRASP in your `Application`:**

```kotlin
class MyApp : Application() {
    private val threats = object : ThreatListener.ThreatDetected() {
        override fun onPrivilegedAccess() { /* rooted */ }
        override fun onDebug() { }
        override fun onSimulator() { }
        override fun onAppIntegrity() { /* repackaged / re-signed */ }
        override fun onUnofficialStore() { }
        override fun onHooks() { /* Frida, Xposed */ }
        override fun onDeviceBinding() { }
        override fun onObfuscationIssues() { }
        override fun onMalware(suspiciousApps: List<SuspiciousAppInfo>) { }
        override fun onScreenshot() { }
        override fun onScreenRecording() { }
        // ...and the rest: onAutomation, onMultiInstance, onUnsecureWifi,
        // onTimeSpoofing, onLocationSpoofing, onBootloader, onOngoingCall
    }

    override fun onCreate() {
        super.onCreate()
        val config = TalsecConfig.Builder(
                "com.your.package",                       // hard-code it, don't use packageName
                arrayOf("BASE64_SHA256_OF_RELEASE_CERT"))
            .watcherMail("security@yourcompany.com")
            .supportedAlternativeStores(emptyArray())
            .prod(true)                                   // false in debug builds
            .killOnBypass(true)
            .build()

        ThreatListener(threats, /* deviceState = */ null, /* executionState = */ null)
            .registerListener(this)
        Talsec.start(this, config)
    }
}
```

Register the class with `android:name=".MyApp"` in the manifest. Optional per-activity protections: `Talsec.blockScreenCapture(activity, true)`, `Talsec.blockScreenControl(...)`, `Talsec.blockOverlayWindows(...)` (API 31+), `ScreenProtector.registerScreenCallbacks(activity)`.

**Step 5. Handle each callback** (log, warn, block, or exit), then **test a release build**. With `prod(false)` the debug, simulator, integrity and store checks stay quiet. The full PoC is in `freeRASP-Android/app/src/main/java/com/poc/freerasp/`.

**Configurability: only partly.** You can set `prod`, `killOnBypass`, trusted stores, expected package and signing hashes, screen protections, and how the app reacts. You can't choose which checks run (all ~20 always run), change the re-check interval, or turn off telemetry to Talsec. The detection core is closed source.

---

## 3. Android RASP (securevale)

**Requirements:** minSdk 24+, `mavenCentral()`.

**Step 1. Add the dependency:**

```kotlin
dependencies {
    implementation("com.securevale:rasp-android:0.7.1")
}
```

**Step 2. Initialise once** in your `Application` (loads the Rust native library):

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureApp.init()
    }
}
```

**Step 3. Add a `<queries>` block** for the root/emulator app packages the native checks look up (the library doesn't declare it, and Android 11+ hides those apps otherwise). The 60-package list is in `AndroidRASP-Android/app/src/main/AndroidManifest.xml`.

**Step 4. Build a checker and run it off the main thread:**

```kotlin
val checker = SecureAppChecker.Builder(
        context,
        checkEmulator = true,
        checkDebugger = true,
        checkRoot = true)
    .build()                         // add .debug() for the library's own Logcat output

// One-shot: a single Result (Secure, EmulatorFound, DebuggerEnabled, Rooted)
val result = checker.check()

// Or every result, per sub-check, restricted to the checks you choose
checker.subscribe(
    granular = true,
    checkOnlyFor = arrayOf(RootChecks.SuUser, RootChecks.RootApps, RootChecks.WritablePaths)
) { r -> Log.i("RASP", "${r.checkType} vulnerable=${r.vulnerable}") }

// Or positives only
checker.subscribeVulnerabilitiesOnly(granular = true) { r -> /* react */ }
```

Note that `check()` ignores `checkOnlyFor`; only the subscribe modes accept it.

**Step 5 (optional).** Request `READ_PHONE_STATE` at runtime to unlock the extra emulator checks.

**Step 6. Test a release build.** R8 doesn't break the JNI layer, and the debugger checks go clean in release.

**Configurability: yes, fully at the API level.** Three category switches, 27 individually selectable checks (3 debugger, 17 emulator, 7 root), and three run modes. Weights and thresholds are fixed, the lists are compiled into the native code, and there's no built-in reaction. It doesn't detect Frida, hooking or tampering.

---

## 4. MobShield (modular RASP)

**Requirements:** minSdk 24+, compileSdk 34+, JDK 17, `mavenCentral()`.

**Step 1. Add the core and the detector modules you want:**

```kotlin
val mobshieldVersion = "1.0.10"

dependencies {
    implementation("io.github.inforaam:mobshield-core:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-root:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-hooks:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-debugger:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-environment:$mobshieldVersion")
    implementation("io.github.inforaam:mobshield-detect-integrity:$mobshieldVersion")
}
```

**Step 2. Build a `MobShieldConfig`:**

```kotlin
val config = MobShieldConfig.builder()
    .expectedPackageId(context.packageName)
    .detectOnly(true)
    .terminationPolicy(TerminationPolicy.NONE)        // or EXIT_ON_BYPASS / EXIT_ON_CRITICAL
    .periodicIntervalSec(15)                          // rescan every 15 s
    .detectionTuning(mapOf(
        "android.debug.app_debuggable" to SignalTuning(weight = 0, confidence = 0)))
    .thresholds(DefaultThreatThresholds.map +
        (ThreatType.EMULATOR to ThreatThreshold(warning = 10, critical = 40)))
    .allowDeveloperSignals(false)
    .build()
```

**Step 3. Register each module directly.** The README's `XxxDetectionRegistrar` helpers aren't in the published Maven artifacts, so register the module classes yourself:

```kotlin
val app = context.applicationContext
ModuleRegistry.register(RootDetectionModule(app, config.detectionTuning))
ModuleRegistry.register(HookDetectionModule(app, config.detectionTuning))
ModuleRegistry.register(DebugDetectionModule(app, config.detectionTuning))
ModuleRegistry.register(EmulatorDetectionModule(app, config.detectionTuning))
ModuleRegistry.register(IntegrityDetectionModule(app, config))
```

**Step 4. Start it with a listener:**

```kotlin
MobShield.start(app, config, object : MobShieldListener {
    override fun onThreat(event: ThreatEvent) {
        // event.type, event.severity, event.score, event.signals
    }
    override fun onAllChecksFinished(events: List<ThreatEvent>) { }
})
```

**Step 5. Handle threats off the UI thread.** Do any heavy reaction work in the background and use `runOnUiThread` to update views. Call `MobShield.stop()` when done.

**Step 6. If you minify**, add to `proguard-rules.pro`:

```
-keep class io.mobshield.** { *; }
```

**Configurability: yes, the most tunable of the five.** You pick the modules, per-signal weight and confidence, per-threat thresholds, scan interval, termination policy, developer-signal handling, and expected signers, package, APK hash and installers.

---

## 5. dpt-shell (post-build packer, no source changes)

dpt-shell works on a finished APK. It hollows out your DEX methods, stores them encrypted, and injects a native shell that restores them at runtime and runs anti-debug, Frida, CRC and (optionally) signature checks.

**Step 1. Build your release APK** as usual (`./gradlew assembleRelease`).

**Step 2. Use the patched `dpt.jar`.** Stock v2.22.1 has three issues we hit and fixed:
- a DEX-merge crash (`AssertionError`) on APKs built with modern AGP;
- a `StackOverflowError` whenever a `-c` config is passed;
- an invalid default shell package name that makes the packed APK fail to install when `-c` is used without `shellPkgName`.

`DptShell-Android/build-patched-dpt.sh` clones the source, applies the code fixes and rebuilds the jar into `packer/executable/`. Keep the release's `shell-files/` folder beside `dpt.jar`.

**Step 3. Create `protect-config.json`** with your release keystore and a dotted shell package name:

```json
{
  "shellPkgName": "com.your.package.shell",
  "signature": {
    "keystore": "/path/to/release.jks",
    "alias": "release",
    "storepass": "********",
    "keypass": "********"
  }
}
```

Don't commit this file (it's covered by `.gitignore`). `DptShell-Android/protect-config.debug.json` is the debug-key version used by the PoC.

**Step 4. Pack:**

```bash
java -jar dpt.jar -f app-release.apk -o out/ -c protect-config.json
```

**Step 5. Choose protections with flags.** Everything is on by default:

| Flag | Effect |
|---|---|
| `--disable-anti-debug` | Turn off anti-debug |
| `--disable-frida-detect` | Turn off Frida detection |
| `--disable-crc-detect` | Turn off the libc `.text` CRC check |
| `-vs` | Turn on runtime signature verification (needs `-c` with a real keystore) |
| `-r rules.txt` / `-K` / `-S` / `-e x86,x86_64` | Exclude classes, keep some classes unprotected, smaller output, exclude ABIs |

`DptShell-Android/run-dpt-pack.sh` packs all five flag combinations and `pack-vs.sh` does the signature-verify build.

**Step 6. Install and test.** If a build signed with a different key is already installed, run `adb uninstall <package>` first, then `adb install out/<apk>` and test the app thoroughly.

**Trade-offs:** the APK grows by about 30 MB (native shell for four ABIs), startup gets slower, and the tool is lightly tested on modern toolchains.

**Configurability: yes.** Anti-debug, Frida detection, CRC and signature verification are independent pack-time flags. We confirmed this from the `risk_check_flags` bitmask dpt writes into the shell config (0 = all on, 1 = Frida off, 2 = CRC off, 4 = anti-debug off, 7 = all off).

---

## Choosing between them

- **Only need root detection:** RootBeer.
- **Want broad coverage with the least code and don't mind telemetry or a closed core:** freeRASP.
- **Want open source with precise control over which checks run:** Android RASP (but no hook or tamper detection).
- **Want the most tuning (weights, thresholds, scheduling, response):** MobShield.
- **Want code protection plus runtime checks without touching source:** dpt-shell, combined with one of the in-app libraries above.
