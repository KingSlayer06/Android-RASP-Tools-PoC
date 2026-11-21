package com.poc.androidrasp

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.securevale.rasp.android.api.SecureAppChecker
import com.securevale.rasp.android.api.result.CheckType
import com.securevale.rasp.android.api.result.DebuggerChecks
import com.securevale.rasp.android.api.result.EmulatorChecks
import com.securevale.rasp.android.api.result.ExtendedResult
import com.securevale.rasp.android.api.result.Result
import com.securevale.rasp.android.api.result.RootChecks

/**
 * Every configuration surface Android RASP exposes, wrapped so the UI and the
 * instrumented tests run exactly the same code. Every line is also logged to Logcat
 * under [TAG] so a run can be verified with `adb logcat -s RASP-PoC`.
 */
object RaspRunner {
    const val TAG = "RASP-PoC"

    /** All individual sub-checks, without the umbrella "XxxCheck" entries (those are only result labels). */
    val ALL_ROOT_CHECKS: Array<CheckType> =
        RootChecks.entries.filter { it != RootChecks.RootCheck }.toTypedArray()
    val ALL_EMULATOR_CHECKS: Array<CheckType> =
        EmulatorChecks.entries.filter { it != EmulatorChecks.EmulatorCheck }.toTypedArray()
    val ALL_DEBUGGER_CHECKS: Array<CheckType> =
        DebuggerChecks.entries.filter { it != DebuggerChecks.DebuggerCheck }.toTypedArray()

    data class Config(val emulator: Boolean, val debugger: Boolean, val root: Boolean) {
        override fun toString() = "checkEmulator=$emulator, checkDebugger=$debugger, checkRoot=$root"
    }

    private fun checker(ctx: Context, c: Config): SecureAppChecker =
        SecureAppChecker.Builder(
            ctx,
            checkEmulator = c.emulator,
            checkDebugger = c.debugger,
            checkRoot = c.root
        ).debug().build() // .debug() turns on the library's own Logcat output

    // ---- Run mode 1: one-shot check() -> single Result ----
    fun oneShot(ctx: Context, c: Config): Result = checker(ctx, c).check()

    // ---- Run mode 2: subscribe (granular or merged), optionally restricted with checkOnlyFor ----
    fun subscribe(
        ctx: Context,
        c: Config,
        granular: Boolean,
        checkOnlyFor: Array<CheckType> = emptyArray() // empty == CHECK_ALL
    ): List<ExtendedResult> {
        val out = mutableListOf<ExtendedResult>()
        checker(ctx, c).subscribe(granular = granular, checkOnlyFor = checkOnlyFor) { out += it }
        return out
    }

    // ---- Run mode 3: subscribeVulnerabilitiesOnly -> only positive findings ----
    fun vulnerabilitiesOnly(ctx: Context, c: Config, granular: Boolean = true): List<ExtendedResult> {
        val out = mutableListOf<ExtendedResult>()
        checker(ctx, c).subscribeVulnerabilitiesOnly(granular = granular) { out += it }
        return out
    }

    fun environment(ctx: Context): String {
        val debuggable = ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val phoneState = ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        return "Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT}, ${Build.SUPPORTED_ABIS.firstOrNull()})\n" +
            "APK debuggable: $debuggable | READ_PHONE_STATE granted: $phoneState"
    }

    /** Runs the full demo matrix and returns a printable report. */
    fun runAll(ctx: Context, toggles: Config): String = buildString {
        fun line(s: String = "") { appendLine(s); if (s.isNotEmpty()) Log.i(TAG, s) }
        fun results(rs: List<ExtendedResult>) {
            if (rs.isEmpty()) line("   (no results)")
            rs.forEach { line("   ${it.checkType} -> vulnerable=${it.vulnerable}") }
        }
        fun <T> timed(block: () -> T): Pair<T, Long> {
            val t = SystemClock.elapsedRealtime(); val r = block(); return r to (SystemClock.elapsedRealtime() - t)
        }

        line(environment(ctx))
        line()

        val all = Config(emulator = true, debugger = true, root = true)
        val (r1, ms1) = timed { oneShot(ctx, all) }
        line("[1] check() one-shot, all categories ($all): $r1 (${ms1}ms)")
        line()

        val (r2, ms2) = timed { oneShot(ctx, toggles) }
        line("[2] check() one-shot, per-category toggles ($toggles): $r2 (${ms2}ms)")
        line()

        line("[3] subscribe(granular=false) merged per category, toggles ($toggles):")
        results(subscribe(ctx, toggles, granular = false))
        line()

        line("[4] subscribe(granular=true) every sub-check, toggles ($toggles):")
        results(subscribe(ctx, toggles, granular = true))
        line()

        val rootOnly = Config(emulator = false, debugger = false, root = true)
        line("[5] Root-only: ($rootOnly) + checkOnlyFor = ${ALL_ROOT_CHECKS.size} root checks:")
        results(subscribe(ctx, rootOnly, granular = true, checkOnlyFor = ALL_ROOT_CHECKS))
        line()

        val subset = arrayOf<CheckType>(RootChecks.SuUser, RootChecks.RootApps, RootChecks.WritablePaths)
        line("[6] All categories enabled, but checkOnlyFor = [SuUser, RootApps, WritablePaths]:")
        results(subscribe(ctx, all, granular = true, checkOnlyFor = subset))
        line()

        line("[7] subscribeVulnerabilitiesOnly(granular=true), all categories (only positives):")
        results(vulnerabilitiesOnly(ctx, all))
    }
}
