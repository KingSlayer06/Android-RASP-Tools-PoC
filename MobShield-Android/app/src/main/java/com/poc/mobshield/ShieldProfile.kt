package com.poc.mobshield

import io.mobshield.core.MobShieldConfig
import io.mobshield.core.SignalTuning
import io.mobshield.core.TerminationPolicy
import io.mobshield.core.ThreatThreshold
import io.mobshield.core.ThreatType
import io.mobshield.core.DefaultThreatThresholds

/** The five MobShield detector modules, each shipped as its own Maven artifact. */
enum class Detector { ROOT, HOOKS, DEBUGGER, ENVIRONMENT, INTEGRITY }

/**
 * Per-module configurations used to show what MobShield lets you configure.
 * Every profile stays detect-only (TerminationPolicy.NONE): MobShield reports, it never kills the app.
 */
enum class ShieldProfile(
    val label: String,
    val detectors: Set<Detector>,
) {
    /** All five detectors, library defaults for weights, thresholds and schedule (one scan at start). */
    ALL_DEFAULTS("All modules, defaults", Detector.entries.toSet()),

    /** A subset: only the root and hook detectors are registered. */
    ROOT_AND_HOOKS("Root + hooks only", setOf(Detector.ROOT, Detector.HOOKS)),

    /** Debugger + environment only, with tuned weights, thresholds and a 15 s rescan schedule. */
    TUNED_DEBUG_ENV("Debugger + environment, tuned", setOf(Detector.DEBUGGER, Detector.ENVIRONMENT)),
    ;

    fun config(packageName: String): MobShieldConfig =
        when (this) {
            ALL_DEFAULTS ->
                MobShieldConfig.builder()
                    .expectedPackageId(packageName)
                    .build()
            ROOT_AND_HOOKS ->
                MobShieldConfig.builder()
                    .expectedPackageId(packageName)
                    .build()
            TUNED_DEBUG_ENV ->
                MobShieldConfig.builder()
                    .detectOnly(true)
                    .terminationPolicy(TerminationPolicy.NONE)
                    // Scan schedule: rescan every 15 s instead of a single scan at start.
                    .periodicIntervalSec(TUNED_INTERVAL_SEC)
                    // Signal weights: silence the "app is debuggable" signal (this is a debug build)
                    // and make the QEMU property signal count fully.
                    .detectionTuning(TUNED_SIGNALS)
                    // Thresholds: report EMULATOR earlier than the default 25/55.
                    .thresholds(DefaultThreatThresholds.map + (ThreatType.EMULATOR to TUNED_EMULATOR_THRESHOLD))
                    // Don't emit DEVELOPER_MODE / ADB_ENABLED events.
                    .allowDeveloperSignals(false)
                    .expectedPackageId(packageName)
                    .build()
        }

    companion object {
        const val TUNED_INTERVAL_SEC = 15
        val TUNED_EMULATOR_THRESHOLD = ThreatThreshold(warning = 10, critical = 40)
        val TUNED_SIGNALS: Map<String, SignalTuning> =
            mapOf(
                "android.debug.app_debuggable" to SignalTuning(weight = 0, confidence = 0),
                "android.env.qemu_props" to SignalTuning(weight = 100, confidence = 100),
            )
    }
}
