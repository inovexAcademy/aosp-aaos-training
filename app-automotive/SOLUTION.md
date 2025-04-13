# Solution

Here is the solution for the exercise.

Add to `AndroidManifest.xml`:

    <uses-permission android:name="android.car.permission.CAR_INFO" />
    <uses-permission android:name="android.car.permission.CAR_POWERTRAIN" />

Add to `MainActivity.java` in function `setupUI`:

    import android.car.VehicleAreaType;
    import android.car.VehiclePropertyIds;
    import android.car.hardware.CarPropertyValue;

    [...]

        CarPropertyValue<String> prop = carPropertyManager.getProperty(VehiclePropertyIds.INFO_MODEL, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
        Log.d(TAG, "model=" + prop.getValue());
        textViewModel.setText("Model: " + prop.getValue());

        int currentGear = carPropertyManager.getIntProperty(VehiclePropertyIds.CURRENT_GEAR, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
        Log.d(TAG, "currentGear=" + currentGear);
        carPropertyManager.subscribePropertyEvents(VehiclePropertyIds.CURRENT_GEAR, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,
                new CarPropertyManager.CarPropertyEventCallback() {
                    @Override
                    public void onChangeEvent(CarPropertyValue carPropertyValue) {
                        Log.d(TAG, "onChangeEvent: gear=" + carPropertyValue.getValue());

                        runOnUiThread(() -> textViewGear.setText("Gear: " + carPropertyValue.getValue()));
                    }
                    @Override
                    public void onErrorEvent(int propertyId, int areaId, int errorCode) {
                        Log.e(TAG, "onErrorEvent: propertyId=" + propertyId + " areaId=" + areaId + " errorCode=" + errorCode);
                    }

                    @Override
                    public void onErrorEvent(int propertyId, int areaId) {
                        Log.e(TAG, "onErrorEvent: propertyId=" + propertyId + " areaId=" + areaId);
                    }
                }
        );

