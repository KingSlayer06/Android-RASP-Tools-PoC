package com.poc.mobshield

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mobshield.core.MobShield
import io.mobshield.core.MobShieldListener
import io.mobshield.core.ThreatEvent
import io.mobshield.core.ThreatType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** On-device: each profile registers exactly its modules, and a full scan runs to completion. */
@RunWith(AndroidJUnit4::class)
class MobShieldRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() = MobShield.stop()

    private fun scan(profile: ShieldProfile): List<ThreatEvent> {
        val done = CountDownLatch(1)
        var result: List<ThreatEvent> = emptyList()
        ShieldRunner.start(
            context,
            profile,
            object : MobShieldListener {
                override fun onThreat(event: ThreatEvent) {}

                override fun onAllChecksFinished(events: List<ThreatEvent>) {
                    result = events
                    done.countDown()
                }
            },
        )
        assertTrue("scan did not finish for $profile", done.await(30, TimeUnit.SECONDS))
        Log.i("MobShieldPoC", "test $profile -> ${result.map { "${it.type}:${it.score}:${it.signals}" }}")
        return result
    }

    @Test
    fun nativeCoreLoads() {
        assertTrue(MobShield.getVersion().isNotBlank())
    }

    @Test
    fun allModulesScan() {
        scan(ShieldProfile.ALL_DEFAULTS)
        assertEquals(5, ShieldRunner.registeredModuleNames().size)
        assertTrue(MobShield.getState().lastScanMs > 0)
    }

    @Test
    fun subsetRegistersOnlyRootAndHooks() {
        scan(ShieldProfile.ROOT_AND_HOOKS)
        assertEquals(setOf("mobshield-detect-root", "hooks"), ShieldRunner.registeredModuleNames().toSet())
    }

    @Test
    fun tunedProfileSuppressesDebuggableAndDeveloperSignals() {
        val events = scan(ShieldProfile.TUNED_DEBUG_ENV)
        assertEquals(setOf("debug", "environment"), ShieldRunner.registeredModuleNames().toSet())
        // app_debuggable is tuned to weight 0, so it can't raise a DEBUGGER event on its own.
        assertTrue(events.none { it.type == ThreatType.DEBUGGER && it.signals == listOf("android.debug.app_debuggable") })
        // allowDeveloperSignals(false): no DEVELOPER_MODE / ADB_ENABLED events.
        assertTrue(events.none { it.type == ThreatType.DEVELOPER_MODE || it.type == ThreatType.ADB_ENABLED })
    }
}
