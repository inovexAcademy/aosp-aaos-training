package de.inovex.aosptraining.automotiveservice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.car.VehicleGear;
import android.car.VehiclePropertyIds;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;


@RunWith(AndroidJUnit4.class)
public class GearServiceInstrumentedTest {

    private static final String TAG = "GearServiceTest";
    private static final long EVENT_PROPAGATION_TIMEOUT_MS = 2000L;
    private static final long POLL_INTERVAL_MS = 50L;

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private IGearService mGearService;

    @Before
    public void setUp() throws TimeoutException {
        Context targetContext = instrumentation.getTargetContext();
        Intent serviceIntent = new Intent(targetContext, GearService.class);
        IBinder binder = mServiceRule.bindService(serviceIntent);
        mGearService = IGearService.Stub.asInterface(binder);
        assertNotNull("IGearService binder interface must not be null", mGearService);
    }

    @Test
    public void testIsReverseGear_returnsTrueOnlyWhenInReverseGear() throws Exception {
        // 1. Simulate PARK gear -> isReverseGear() should be false
        injectCurrentGear(VehicleGear.GEAR_PARK);
        assertTrue(
                "Expected isReverseGear() to be false when in GEAR_PARK",
                waitForReverseGearState(false)
        );
        assertFalse(mGearService.isReverseGear());

        // 2. Simulate REVERSE gear -> isReverseGear() should be true
        injectCurrentGear(VehicleGear.GEAR_REVERSE);
        assertTrue(
                "Expected isReverseGear() to be true when in GEAR_REVERSE",
                waitForReverseGearState(true)
        );
        assertTrue(mGearService.isReverseGear());

        // 3. Simulate DRIVE gear -> isReverseGear() should return to false
        injectCurrentGear(VehicleGear.GEAR_DRIVE);
        assertTrue(
                "Expected isReverseGear() to be false when in GEAR_DRIVE",
                waitForReverseGearState(false)
        );
        assertFalse(mGearService.isReverseGear());
    }

    private void injectCurrentGear(int gearValue) throws IOException {
        String command = String.format(
                "dumpsys activity service com.android.car inject-vhal-event %d %d",
                VehiclePropertyIds.CURRENT_GEAR,
                gearValue
        );
        Log.d(TAG, "Executing shell command: " + command);

        UiAutomation uiAutomation = instrumentation.getUiAutomation();
        uiAutomation.executeShellCommand(command).close();
    }

    private boolean waitForReverseGearState(boolean expectedState)
            throws RemoteException, InterruptedException {
        long deadline = System.currentTimeMillis() + EVENT_PROPAGATION_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (mGearService.isReverseGear() == expectedState) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return mGearService.isReverseGear() == expectedState;
    }
}
