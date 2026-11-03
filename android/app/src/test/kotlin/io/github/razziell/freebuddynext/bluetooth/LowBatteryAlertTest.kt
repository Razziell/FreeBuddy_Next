package io.github.razziell.freebuddynext.bluetooth

import io.github.razziell.freebuddynext.protocol.BatteryLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LowBatteryAlertTest {
    private val address = "AA:BB:CC:DD:EE:FF"
    private val enabledSettings = LowBatteryAlertSettings(
        enabled = true,
        leftEnabled = true,
        leftThresholdPercent = 20,
        rightEnabled = true,
        rightThresholdPercent = 30,
        caseEnabled = true,
        caseThresholdPercent = 25,
    )

    @Test
    fun `threshold normalization clamps and snaps to 5 percent steps`() {
        assertEquals(5, LowBatteryAlertSettings.normalizeThreshold(1))
        assertEquals(15, LowBatteryAlertSettings.normalizeThreshold(14))
        assertEquals(20, LowBatteryAlertSettings.normalizeThreshold(22))
        assertEquals(75, LowBatteryAlertSettings.normalizeThreshold(74))
        assertEquals(80, LowBatteryAlertSettings.normalizeThreshold(95))
    }

    @Test
    fun `triggers once when earbud drops to or below its own threshold and suppresses further drops`() {
        val initial = LowBatteryTrackerState()
        val first = LowBatteryAlertEvaluator.evaluate(
            previous = initial,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 20, right = 35, case = 80),
        )
        assertTrue(first.shouldNotify)
        assertFalse(first.shouldCancel)
        assertEquals(20, first.leftPercent)
        assertNull(first.rightPercent)
        assertNull(first.casePercent)

        val second = LowBatteryAlertEvaluator.evaluate(
            previous = first.state,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 15, right = 35, case = 80),
        )
        assertFalse(second.shouldNotify)
        assertFalse(second.shouldCancel)
        assertEquals(15, second.leftPercent)
    }

    @Test
    fun `notifies again when second earbud or case also drops below its own threshold`() {
        val first = LowBatteryAlertEvaluator.evaluate(
            previous = LowBatteryTrackerState(),
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 18, right = 50, case = 60),
        )
        assertTrue(first.shouldNotify)

        val second = LowBatteryAlertEvaluator.evaluate(
            previous = first.state,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 16, right = 30, case = 25),
        )
        assertTrue(second.shouldNotify)
        assertEquals(16, second.leftPercent)
        assertEquals(30, second.rightPercent)
        assertEquals(25, second.casePercent)
    }

    @Test
    fun `charging or recovery above threshold clears alert and allows future alert`() {
        val triggered = LowBatteryAlertEvaluator.evaluate(
            previous = LowBatteryTrackerState(),
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 15, right = 60, case = 60),
        )
        assertTrue(triggered.shouldNotify)

        val charging = LowBatteryAlertEvaluator.evaluate(
            previous = triggered.state,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 15, right = 60, case = 60, chargingLeft = true),
        )
        assertFalse(charging.shouldNotify)
        assertTrue(charging.shouldCancel)

        val droppedAgain = LowBatteryAlertEvaluator.evaluate(
            previous = charging.state,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 15, right = 60, case = 60, chargingLeft = false),
        )
        assertTrue(droppedAgain.shouldNotify)
    }

    @Test
    fun `closed case reporting null does not re-trigger alert on reopen`() {
        val caseLow = LowBatteryAlertEvaluator.evaluate(
            previous = LowBatteryTrackerState(),
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 80, right = 80, case = 20),
        )
        assertTrue(caseLow.shouldNotify)
        assertEquals(20, caseLow.casePercent)

        val caseClosed = LowBatteryAlertEvaluator.evaluate(
            previous = caseLow.state,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 78, right = 78, case = null),
        )
        assertFalse(caseClosed.shouldNotify)
        assertFalse(caseClosed.shouldCancel)
        assertTrue(caseClosed.state.caseAlerted)

        val caseReopened = LowBatteryAlertEvaluator.evaluate(
            previous = caseClosed.state,
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 76, right = 76, case = 19),
        )
        assertFalse(caseReopened.shouldNotify)
        assertEquals(19, caseReopened.casePercent)
    }

    @Test
    fun `disabling setting cancels active alert`() {
        val triggered = LowBatteryAlertEvaluator.evaluate(
            previous = LowBatteryTrackerState(),
            settings = enabledSettings,
            address = address,
            battery = BatteryLevels(left = 10, right = 50, case = 50),
        )
        assertTrue(triggered.shouldNotify)

        val disabled = LowBatteryAlertEvaluator.evaluate(
            previous = triggered.state,
            settings = enabledSettings.copy(enabled = false),
            address = address,
            battery = BatteryLevels(left = 10, right = 50, case = 50),
        )
        assertFalse(disabled.shouldNotify)
        assertTrue(disabled.shouldCancel)
    }
}
