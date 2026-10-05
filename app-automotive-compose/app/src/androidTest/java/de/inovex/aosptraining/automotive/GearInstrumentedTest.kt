package de.inovex.aosptraining.automotive

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
        injectGearChange(3)

        // Add the test code to check if the expected gear is displayed correctly in our app

    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testGearChange() {

        // Write a test that checks if the value changes as expected while the app is running and
        // gear changes are getting executed in the background through this testing code

    }

    private fun injectGearChange(gear: Int) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(

            // Add the adb command to change the value of the gear property

        ).close()
    }

    companion object {
        private const val CURRENT_GEAR_PROPERTY_ID = 289408001
    }
}