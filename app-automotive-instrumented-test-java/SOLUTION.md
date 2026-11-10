# Solution

To change the value of the vehicle property `CURRENT_GEAR` one can call `dumpsys activity service com.android.car inject-vhal-event 289408001 2`

In the `MainViewModel` we need to subscribe to changes of the property. First add two fields that keep the manager and the callback, so the subscription can be removed again in `onCleared()`:

        private CarPropertyManager carPropertyManager;

        private final CarPropertyManager.CarPropertyEventCallback gearCallback =
                new CarPropertyManager.CarPropertyEventCallback() {
                    @Override
                    public void onChangeEvent(CarPropertyValue gear) {
                        if (gear != null) {
                            Log.d(TAG, "CURRENT_GEAR value received: " + gear.getValue());

                            currentGear.setValue(VehicleGear.toString((Integer) gear.getValue()));
                        }
                    }

                    @Override
                    public void onErrorEvent(int propertyId, int areaId) {
                        Log.d(TAG, "Error for property " + propertyId + " in area " + areaId);
                    }
                };

Then subscribe in `observeCurrentGear`:

        this.carPropertyManager = carPropertyManager;

        carPropertyManager.subscribePropertyEvents(
                VehiclePropertyIds.CURRENT_GEAR,
                gearCallback
        );

And unsubscribe in `onCleared()` before the car gets disconnected:

        if (carPropertyManager != null) {
            try {
                carPropertyManager.unsubscribePropertyEvents(
                        VehiclePropertyIds.CURRENT_GEAR,
                        gearCallback
                );
            } catch (RuntimeException error) {
                Log.w(TAG, "Failed to unsubscribe property " + VehiclePropertyIds.CURRENT_GEAR
                        + ": " + error.getMessage());
            }

            Log.d(TAG, "Finished observing CURRENT_GEAR changes");
        }

In the already existing instrumented test class `GearInstrumentedTest` the first necessary step is to add the adb command to the helper function:

        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "dumpsys activity service com.android.car inject-vhal-event "
                        + VehiclePropertyIds.CURRENT_GEAR + " " + gear
        ).close();

The connection to the CarService is established asynchronously and the emulator is sometimes slow, so the expected text is not necessarily displayed right after the activity has been launched or the gear has been injected. Therefore a small helper is added that retries the Espresso check until the expected text is displayed or a timeout is reached:

        private void waitUntilTextIsDisplayed(String text) {
            long timeout = System.currentTimeMillis() + 5_000;

            while (true) {
                try {
                    onView(withText(text)).check(matches(isDisplayed()));
                    return;
                } catch (NoMatchingViewException | AssertionError error) {
                    if (System.currentTimeMillis() >= timeout) {
                        throw error;
                    }
                    SystemClock.sleep(100);
                }
            }
        }

The simple test to just check if the injected gear is identical to the one displayed in the app:

        waitUntilTextIsDisplayed("Gear: " + VehicleGear.toString(testGear));

To test property changes, one needs to inject a value that differs from the previous one during the test case execution (the test method needs to declare `throws IOException`):

        for (int gear : testGears) {
            injectGearChange(gear);
            waitUntilTextIsDisplayed("Gear: " + VehicleGear.toString(gear));
        }

To run all instrumented tests via command line, call `gradlew app:connectedAndroidTest` or `adb shell am instrument -w de.inovex.aosptraining.automotive/androidx.test.runner.AndroidJUnitRunner`

To run only one test case at a time: `./gradlew app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.inovex.aosptraining.automotive#testGearChange` `adb shell am instrument -w -e class de.inovex.aosptraining.automotive#testGearChange de.inovex.aosptraining.automotive/androidx.test.runner.AndroidJUnitRunner`

Note that the test package needs to be build and installed on the device before when using the adb shell am instrument commands: `./gradlew :app:assembleAndroidTest` & `adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
