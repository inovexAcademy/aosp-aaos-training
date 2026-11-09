package de.inovex.aosptraining.automotive;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.car.VehicleGear;
import android.car.VehiclePropertyIds;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GearInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testCurrentVehicleGear() {
        int testGear = VehicleGear.GEAR_PARK;

        // Add the test code to check if the expected gear is displayed correctly in our app

    }

    @Test
    public void testGearChange() {
        List<Integer> testGears = List.of(VehicleGear.GEAR_DRIVE, VehicleGear.GEAR_REVERSE);

        // Write a test that checks if the value changes as expected while the app is running and
        // gear changes are getting executed in the background through this testing code

    }

    private void injectGearChange(int gear) throws IOException {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(

                // Add the adb command to change the value of the VehiclePropertyIds.CURRENT_GEAR

        ).close();
    }
}
