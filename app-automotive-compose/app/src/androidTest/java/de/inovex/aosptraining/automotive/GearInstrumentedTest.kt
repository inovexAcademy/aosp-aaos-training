package de.inovex.aosptraining.automotive

import android.car.VehicleGear
import android.car.VehiclePropertyIds
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GearInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCurrentVehicleGear() {
        val testGear = VehicleGear.GEAR_PARK

        injectGearChange(testGear)
        composeTestRule.waitUntilExactlyOneExists(hasText("Gear: ${VehicleGear.toString(testGear)}"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testGearChange() {
        val testGears = listOf(VehicleGear.GEAR_DRIVE, VehicleGear.GEAR_REVERSE)

        testGears.forEach { gear ->
            injectGearChange(gear)
            composeTestRule.waitUntilExactlyOneExists(hasText("Gear: ${VehicleGear.toString(gear)}"))
        }
    }

    private fun injectGearChange(gear: Int) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "dumpsys activity service com.android.car inject-vhal-event ${VehiclePropertyIds.CURRENT_GEAR} $gear"
        ).close()
    }
}