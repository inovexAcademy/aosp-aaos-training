# Solution

To change the value of the vehicle property `CURRENT_GEAR` one can call `dumpsys activity service com.android.car inject-vhal-event 289408001 2`

In the `MainViewModel` we need to subscribe to changes of the property:

        carPropertyManager.first().subscribePropertyEvents(
            VehiclePropertyIds.CURRENT_GEAR,
            object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(gear: CarPropertyValue<*>?) {
                    gear?.let {
                        Log.d(TAG, "CURRENT_GEAR value received: ${it.value}")

                        trySend(it.value as Int)
                    }
                }

                override fun onErrorEvent(propertyId: Int, areaId: Int) {
                    Log.d(TAG, "Error for property $propertyId in area $areaId")
                }
            }
        )

In the already existing instrumented test class `GearInstrumentedTest` the first necessary step is to add the adb command to the helper function:

        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "dumpsys activity service com.android.car inject-vhal-event $CURRENT_GEAR_PROPERTY_ID $gear"
        ).close()

The simple test to just check if the injected gear is identical to the one displayed in the app:

        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("Gear: 3"))
        }

To test property changes, one needs to inject a value that differs from the previous one during the test case execution:

        injectGearChange(4)

        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("Gear: 4"))

            injectGearChange(2)

            waitUntilExactlyOneExists(hasText("Gear: 2"))
        }
