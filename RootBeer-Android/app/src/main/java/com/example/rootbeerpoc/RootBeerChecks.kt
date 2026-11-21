package com.example.rootbeerpoc

import com.scottyab.rootbeer.RootBeer

/**
 * Every RootBeer check that can be called on its own. Each entry maps to one public method on
 * [RootBeer], so any subset can be run, in any order, as often as the app wants.
 */
enum class RootCheck(val label: String, val method: String) {
    ROOT_MANAGEMENT_APPS("Root management apps", "detectRootManagementApps"),
    POTENTIALLY_DANGEROUS_APPS("Potentially dangerous apps", "detectPotentiallyDangerousApps"),
    ROOT_CLOAKING_APPS("Root cloaking apps", "detectRootCloakingApps"),
    TEST_KEYS("Test keys", "detectTestKeys"),
    DANGEROUS_PROPS("Dangerous props", "checkForDangerousProps"),
    BUSYBOX_BINARY("BusyBox binary", "checkForBusyBoxBinary"),
    SU_BINARY("su binary", "checkForSuBinary"),
    SU_EXISTS("su exists (which su)", "checkSuExists"),
    RW_SYSTEM("RW system paths", "checkForRWPaths"),
    NATIVE_SU("Native su check (JNI)", "checkForRootNative"),
    MAGISK_BINARY("Magisk binary", "checkForMagiskBinary"),
}

/**
 * What the PoC can configure. RootBeer itself has no config object; configuration means choosing
 * which methods to call and passing extra package names to the three package-list checks.
 */
data class RootBeerConfig(
    val checks: Set<RootCheck>,
    val extraRootManagementApps: List<String> = emptyList(),
    val extraDangerousApps: List<String> = emptyList(),
    val extraCloakingApps: List<String> = emptyList(),
    val logging: Boolean = false,
) {
    companion object {
        /** The exact checks RootBeer.isRooted() chains together in 0.1.2 (no BusyBox, no cloaking). */
        val IS_ROOTED_EQUIVALENT: Set<RootCheck> = setOf(
            RootCheck.ROOT_MANAGEMENT_APPS,
            RootCheck.POTENTIALLY_DANGEROUS_APPS,
            RootCheck.SU_BINARY,
            RootCheck.DANGEROUS_PROPS,
            RootCheck.RW_SYSTEM,
            RootCheck.TEST_KEYS,
            RootCheck.SU_EXISTS,
            RootCheck.NATIVE_SU,
            RootCheck.MAGISK_BINARY,
        )

        /** isRootedWithBusyBoxCheck() = isRooted() + BusyBox binary. */
        val WITH_BUSYBOX_EQUIVALENT: Set<RootCheck> = IS_ROOTED_EQUIVALENT + RootCheck.BUSYBOX_BINARY

        val ALL: Set<RootCheck> = RootCheck.entries.toSet()
    }
}

data class CheckResult(val name: String, val detected: Boolean, val micros: Long, val error: String? = null)

data class RootBeerReport(
    val perCheck: List<CheckResult>,
    val isRooted: CheckResult,
    val isRootedWithBusyBox: CheckResult,
) {
    val anyDetected: Boolean get() = perCheck.any { it.detected }
}

class RootBeerRunner(private val rootBeer: RootBeer) {

    fun run(config: RootBeerConfig): RootBeerReport {
        rootBeer.setLogging(config.logging)
        val perCheck = RootCheck.entries
            .filter { it in config.checks }
            .map { check -> timed(check.method) { invoke(check, config) } }
        return RootBeerReport(
            perCheck = perCheck,
            isRooted = timed("isRooted") { rootBeer.isRooted },
            isRootedWithBusyBox = timed("isRootedWithBusyBoxCheck") { rootBeer.isRootedWithBusyBoxCheck },
        )
    }

    private fun invoke(check: RootCheck, config: RootBeerConfig): Boolean = when (check) {
        RootCheck.ROOT_MANAGEMENT_APPS ->
            rootBeer.detectRootManagementApps(config.extraRootManagementApps.toTypedArray())
        RootCheck.POTENTIALLY_DANGEROUS_APPS ->
            rootBeer.detectPotentiallyDangerousApps(config.extraDangerousApps.toTypedArray())
        RootCheck.ROOT_CLOAKING_APPS ->
            rootBeer.detectRootCloakingApps(config.extraCloakingApps.toTypedArray())
        RootCheck.TEST_KEYS -> rootBeer.detectTestKeys()
        RootCheck.DANGEROUS_PROPS -> rootBeer.checkForDangerousProps()
        RootCheck.BUSYBOX_BINARY -> rootBeer.checkForBusyBoxBinary()
        RootCheck.SU_BINARY -> rootBeer.checkForSuBinary()
        RootCheck.SU_EXISTS -> rootBeer.checkSuExists()
        RootCheck.RW_SYSTEM -> rootBeer.checkForRWPaths()
        RootCheck.NATIVE_SU -> rootBeer.checkForRootNative()
        RootCheck.MAGISK_BINARY -> rootBeer.checkForMagiskBinary()
    }

    private inline fun timed(name: String, block: () -> Boolean): CheckResult {
        val start = System.nanoTime()
        return try {
            val detected = block()
            CheckResult(name, detected, (System.nanoTime() - start) / 1_000)
        } catch (t: Throwable) {
            CheckResult(name, false, (System.nanoTime() - start) / 1_000, t.toString())
        }
    }
}
