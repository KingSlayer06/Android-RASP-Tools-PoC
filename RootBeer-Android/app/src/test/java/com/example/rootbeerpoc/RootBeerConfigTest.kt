package com.example.rootbeerpoc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootBeerConfigTest {
    @Test fun everyCheckIsIndividuallySelectable() {
        assertEquals(11, RootCheck.entries.size)
        RootCheck.entries.forEach { assertEquals(setOf(it), RootBeerConfig(checks = setOf(it)).checks) }
    }

    @Test fun isRootedPresetMatchesLibrary() {
        assertEquals(9, RootBeerConfig.IS_ROOTED_EQUIVALENT.size)
        assertFalse(RootCheck.BUSYBOX_BINARY in RootBeerConfig.IS_ROOTED_EQUIVALENT)
        assertFalse(RootCheck.ROOT_CLOAKING_APPS in RootBeerConfig.IS_ROOTED_EQUIVALENT)
        assertTrue(RootCheck.BUSYBOX_BINARY in RootBeerConfig.WITH_BUSYBOX_EQUIVALENT)
    }
}
