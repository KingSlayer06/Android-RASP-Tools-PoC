package com.example.rootbeerpoc

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scottyab.rootbeer.RootBeer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Runs every RootBeer check for real on the device/emulator and logs the outcome. */
@RunWith(AndroidJUnit4::class)
class RootBeerOnDeviceTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val runner = RootBeerRunner(RootBeer(ctx))

    @Test fun allChecksExecuteWithoutError() {
        val report = runner.run(RootBeerConfig(checks = RootBeerConfig.ALL))
        assertEquals(RootCheck.entries.size, report.perCheck.size)
        (report.perCheck + report.isRooted + report.isRootedWithBusyBox).forEach {
            Log.i("RootBeerPoC", "${it.name}=${it.detected} ${it.micros}us ${it.error ?: ""}")
            assertNull("${it.name} threw", it.error)
        }
    }

    @Test fun eachCheckRunsAlone() {
        RootCheck.entries.forEach { check ->
            val report = runner.run(RootBeerConfig(checks = setOf(check)))
            assertEquals(listOf(check.method), report.perCheck.map { it.name })
        }
    }

    @Test fun extraPackageNameIsHonoured() {
        // Our own package is installed, so adding it to the root-app list must flip the check to true.
        val report = runner.run(
            RootBeerConfig(checks = setOf(RootCheck.ROOT_MANAGEMENT_APPS), extraRootManagementApps = listOf(ctx.packageName))
        )
        assertEquals(true, report.perCheck.single().detected)
    }

    @Test fun nativeLibraryLoads() {
        assertEquals(true, RootBeer(ctx).canLoadNativeLibrary())
    }
}
