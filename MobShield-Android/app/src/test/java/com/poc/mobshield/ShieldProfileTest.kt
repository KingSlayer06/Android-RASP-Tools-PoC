package com.poc.mobshield

import io.mobshield.core.MobShieldConfig
import io.mobshield.core.Severity
import io.mobshield.core.Signal
import io.mobshield.core.SignalAggregator
import io.mobshield.core.TerminationPolicy
import io.mobshield.core.ThreatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests: config is accepted, stays detect-only, and tuning/thresholds change the aggregated result. */
class ShieldProfileTest {
    private val pkg = "com.poc.mobshield"

    @Test
    fun everyProfileIsDetectOnly() {
        ShieldProfile.entries.forEach {
            val c = it.config(pkg)
            assertTrue(c.detectOnly)
            assertEquals(TerminationPolicy.NONE, c.terminationPolicy)
        }
    }

    @Test
    fun tunedProfileSetsScheduleWeightsAndThresholds() {
        val c = ShieldProfile.TUNED_DEBUG_ENV.config(pkg)
        assertEquals(15, c.periodicIntervalSec)
        assertEquals(0, c.detectionTuning.getValue("android.debug.app_debuggable").weight)
        assertEquals(10, c.thresholds.getValue(ThreatType.EMULATOR).warning)
    }

    @Test(expected = IllegalArgumentException::class)
    fun detectOnlyRejectsTermination() {
        MobShieldConfig.builder().terminationPolicy(TerminationPolicy.EXIT_ON_CRITICAL).build()
    }

    @Test
    fun loweredThresholdRaisesSeverityOfSameSignal() {
        // weight 40 * confidence 50 / 100 = score 20. Default EMULATOR threshold is 25/55 -> MEDIUM;
        // the tuned threshold is 10/40 -> HIGH.
        val weak = listOf(Signal(name = "android.env.sensor_count", weight = 40, confidence = 50))
        val defaults = SignalAggregator(MobShieldConfig()).aggregate(weak).single()
        val tuned = SignalAggregator(ShieldProfile.TUNED_DEBUG_ENV.config(pkg)).aggregate(weak).single()
        assertEquals(ThreatType.EMULATOR, defaults.type)
        assertEquals(20, defaults.score)
        assertEquals(Severity.MEDIUM, defaults.severity)
        assertEquals(Severity.HIGH, tuned.severity)
    }

    @Test
    fun zeroWeightSignalProducesNoEvent() {
        val muted = listOf(Signal(name = "android.debug.app_debuggable", weight = 0, confidence = 0))
        assertTrue(SignalAggregator(MobShieldConfig()).aggregate(muted).isEmpty())
    }
}
