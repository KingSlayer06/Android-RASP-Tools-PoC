package com.poc.androidrasp

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.poc.androidrasp.RaspRunner.Config
import com.securevale.rasp.android.api.result.CheckType
import com.securevale.rasp.android.api.result.DebuggerChecks
import com.securevale.rasp.android.api.result.EmulatorChecks
import com.securevale.rasp.android.api.result.Result
import com.securevale.rasp.android.api.result.RootChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the native checks execute on a real device/emulator and that each configuration
 * surface (category toggles, checkOnlyFor, run modes) changes what actually runs.
 * The debug test APK is debuggable, so debugger checks are expected to fire.
 */
@RunWith(AndroidJUnit4::class)
class RaspChecksTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val all = Config(emulator = true, debugger = true, root = true)
    private val none = Config(emulator = false, debugger = false, root = false)

    private fun isEmulator() = Build.FINGERPRINT.contains("generic") ||
        Build.HARDWARE.contains("ranchu") || Build.PRODUCT.contains("sdk")

    @Test
    fun fullReport_runsWithoutCrashing() {
        val report = RaspRunner.runAll(ctx, all)
        Log.i(RaspRunner.TAG, "TEST REPORT\n$report")
        assertTrue(report.contains("[7]"))
    }

    @Test
    fun allCategoriesDisabled_isSecure() {
        assertEquals(Result.Secure, RaspRunner.oneShot(ctx, none))
        assertTrue(RaspRunner.subscribe(ctx, none, granular = true).isEmpty())
    }

    @Test
    fun debuggerOnly_onDebuggableApk_reportsDebuggerEnabled() {
        val r = RaspRunner.oneShot(ctx, Config(emulator = false, debugger = true, root = false))
        assertEquals(Result.DebuggerEnabled, r)
    }

    @Test
    fun emulatorOnly_onEmulator_reportsEmulatorFound() {
        assumeTrue("not running on an emulator", isEmulator())
        assertEquals(Result.EmulatorFound, RaspRunner.oneShot(ctx, Config(true, false, false)))
    }

    @Test
    fun categoryToggle_emulatorOff_neverReturnsEmulatorFound() {
        val r = RaspRunner.oneShot(ctx, Config(emulator = false, debugger = true, root = true))
        assertTrue("got $r", r != Result.EmulatorFound)
    }

    @Test
    fun granularToggles_emitOnlyEnabledCategories() {
        val types = RaspRunner.subscribe(ctx, Config(false, true, false), granular = true).map { it.checkType }
        assertEquals(RaspRunner.ALL_DEBUGGER_CHECKS.toSet(), types.toSet())
    }

    @Test
    fun rootOnly_checkOnlyFor_runsExactlyTheRootChecks() {
        val rs = RaspRunner.subscribe(ctx, Config(false, false, true), granular = true,
            checkOnlyFor = RaspRunner.ALL_ROOT_CHECKS)
        rs.forEach { Log.i(RaspRunner.TAG, "root-only: ${it.checkType} vulnerable=${it.vulnerable}") }
        assertEquals(RaspRunner.ALL_ROOT_CHECKS.toList(), rs.map { it.checkType })
        assertTrue(rs.all { it.checkType is RootChecks })
    }

    @Test
    fun checkOnlyFor_subset_overridesEnabledCategories() {
        val subset = arrayOf<CheckType>(RootChecks.SuUser, RootChecks.WritablePaths)
        val rs = RaspRunner.subscribe(ctx, all, granular = true, checkOnlyFor = subset)
        assertEquals(subset.toList(), rs.map { it.checkType })
    }

    @Test
    fun checkOnlyFor_emulatorSubset_onEmulator_findsAvd() {
        assumeTrue("not running on an emulator", isEmulator())
        val subset = arrayOf<CheckType>(EmulatorChecks.AvdDevice, EmulatorChecks.AvdHardware,
            EmulatorChecks.GoogleEmulator, EmulatorChecks.Fingerprint)
        val rs = RaspRunner.subscribe(ctx, Config(true, false, false), granular = true, checkOnlyFor = subset)
        assertEquals(subset.toList(), rs.map { it.checkType })
        assertTrue("no AVD indicator fired: $rs", rs.any { it.vulnerable })
    }

    @Test
    fun mergedSubscribe_emitsOneResultPerCategory() {
        val rs = RaspRunner.subscribe(ctx, all, granular = false)
        assertEquals(
            listOf<CheckType>(EmulatorChecks.EmulatorCheck, DebuggerChecks.DebuggerCheck, RootChecks.RootCheck),
            rs.map { it.checkType }
        )
    }

    @Test
    fun vulnerabilitiesOnly_returnsOnlyPositives() {
        val rs = RaspRunner.vulnerabilitiesOnly(ctx, all)
        assertTrue(rs.isNotEmpty()) // at least the debuggable flag fires on a debug APK
        assertTrue(rs.all { it.vulnerable })
    }
}
