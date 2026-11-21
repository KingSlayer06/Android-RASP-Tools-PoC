package com.poc.freerasp

/**
 * Every knob freeRASP exposes, in one place.
 *
 * What is NOT here, because freeRASP does not offer it:
 *  - choosing which detection checks run (all of them always run),
 *  - the periodic re-check interval (randomised inside the SDK),
 *  - switching off telemetry to Talsec (always on in the free tier).
 */
object RaspConfig {
    /** Must be the hard-coded package name. Do not use Context.packageName. */
    const val EXPECTED_PACKAGE_NAME = "com.poc.freerasp"

    /**
     * Base64 SHA-256 of the release signing certificate(s).
     * Left empty for the PoC: RaspApplication then uses the hash of the running APK,
     * so onAppIntegrity stays quiet. Put the real release hash here for a real app.
     */
    val EXPECTED_SIGNING_CERT_HASHES: Array<String> = emptyArray()

    /** true: release behaviour (debug, simulator, integrity, store checks report). */
    val PROD: Boolean = BuildConfig.RASP_PROD

    /** Kill the app inside the SDK if an attacker hooks or modifies the callbacks. */
    const val KILL_ON_BYPASS = false

    /** Play Store and Huawei AppGallery are trusted out of the box; add others here. */
    val SUPPORTED_ALTERNATIVE_STORES: Array<String> = emptyArray()

    /** Address that receives Talsec security reports. */
    const val WATCHER_MAIL = "rasp-poc@example.com"

    // Per-activity protections (applied in RaspApplication's lifecycle callbacks)
    const val BLOCK_SCREEN_CAPTURE = false
    const val BLOCK_SCREEN_CONTROL = false
    const val BLOCK_OVERLAY_WINDOWS = false
    const val DETECT_SCREEN_EVENTS = true      // onScreenshot / onScreenRecording
    const val DETECT_ONGOING_CALL = true       // onOngoingCall

    // Optional listeners
    const val REGISTER_DEVICE_STATE = true     // passcode, dev mode, ADB, VPN, secure HW
    const val REGISTER_EXECUTION_STATE = true  // critical / fast / postponed checks finished
}
