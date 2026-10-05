# Hands-On: Instrumentation test with Compose

This simple app written in Kotlin and Compose shows the name of the Vehicle model and the current gear, backed by the [VehiclePropertyId CURRENT_GEAR](https://developer.android.com/reference/android/car/VehiclePropertyIds#CURRENT_GEAR). Goal is to write two instrumentation tests that ensure that the expected value for the gear is shown in the user interface.


# Step 1: Subscribe to the VehicleProperty

Before the app can be build and the test cases can be written, implement the part that subscribes to the VehiclePropertyId `CURRENT_GEAR`.

# Step 2: Compile and install the App

Compile the App via Android Studio or in the command line by running `./gradlew app:assembleDebug`. To compile and install on a connected device (e.g. a running cuttlefish emulator), the command `./gradlew app:installDebug` can also be used.

# Step 3: Check VehicleProperty subscription manually

To make sure that the subscription to the `CURRENT_GEAR` VehicleProperty is working as expected, research how one can set a the value of a VehicleProperty through adb and use this command to check if the gear value is changing in the user interface while the app is opened.

# Step 4: Add instrumentation tests

In the code base an instrumented test class named `GearInstrumentedTest` containing two test cases already exists. Use this as a base to add the test code. To avoid repition there is also already an empty helper method that should be filled with the adb command to set the vehicle property value, so this `injectGearChange` method can be used in the test cases to manipulate the current gear and check if the user interface reflects the expected value later on.

Start with the `testCurrentVehicleGear` test. It should check if the expected gear "3" is shown in the text Composable. Some basic examples of testing Compose elements can be found [here](https://developer.android.com/develop/ui/compose/testing/apis). The page also includes a link to the [Compose Testing cheat sheet](https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet), which can be quite useful. Information about how to run the test can be found in the next step. 

The `testGearChange` is aiming at also checking changes of the value while the app is running. Thus, it should contain at least two calls of `injectGearChange` with different values, followed by a UI check each time.

# Step 5: Execute tests

Instrumented tests can either be compiled and executed via Android Studio or via command line. Information about how to run via Android Studio can be found [here](https://developer.android.com/studio/test/test-in-android-studio#run-tests). If running in the console, there are two options to execute instrumentation tests: use gradle or fire up an adb command. Both options are explained in the [documentation](https://developer.android.com/studio/test/command-line). Note that the page also contains information about running unit tests. Since we are working on instrumentation tests, you can skip this information for now.
