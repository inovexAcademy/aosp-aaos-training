package de.inovex.aosptraining.automotiveservice;

import android.app.Service;
import android.car.Car;
import android.car.VehicleAreaType;
import android.car.VehicleGear;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Custom bound service that monitors the vehicle's CURRENT_GEAR property via CarPropertyManager
 * and exposes whether the car is currently in reverse gear over AIDL.
 */
public class GearService extends Service {

    private static final String TAG = "GearService";

    private Car mCar;
    private CarPropertyManager mCarPropertyManager;
    private volatile int mCurrentGear = VehicleGear.GEAR_UNKNOWN;

    private final CarPropertyManager.CarPropertyEventCallback mGearEventCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    if (value != null && value.getPropertyId() == VehiclePropertyIds.CURRENT_GEAR) {
                        Object rawValue = value.getValue();
                        if (rawValue instanceof Integer) {
                            mCurrentGear = (Integer) rawValue;
                            Log.d(TAG, "CURRENT_GEAR changed to: " + mCurrentGear);
                        }
                    }
                }

                @Override
                public void onErrorEvent(int propId, int zone) {
                    Log.w(TAG, "Error event received for propertyId=" + propId + ", zone=" + zone);
                }
            };

    private final IGearService.Stub mBinder = new IGearService.Stub() {
        @Override
        public boolean isReverseGear() {
            return mCurrentGear == VehicleGear.GEAR_REVERSE;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initCarPropertyManager();
    }

    private void initCarPropertyManager() {
        mCar = Car.createCar(this);
        if (mCar != null) {
            mCarPropertyManager = (CarPropertyManager) mCar.getCarManager(Car.PROPERTY_SERVICE);
            if (mCarPropertyManager != null) {
                try {
                    mCurrentGear = mCarPropertyManager.getIntProperty(
                            VehiclePropertyIds.CURRENT_GEAR,
                            VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
                    );
                    Log.d(TAG, "Initial CURRENT_GEAR value: " + mCurrentGear);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to read initial CURRENT_GEAR value", e);
                }

                mCarPropertyManager.registerCallback(
                        mGearEventCallback,
                        VehiclePropertyIds.CURRENT_GEAR,
                        CarPropertyManager.SENSOR_RATE_ONCHANGE
                );
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (mCarPropertyManager != null) {
            mCarPropertyManager.unregisterCallback(mGearEventCallback);
            mCarPropertyManager = null;
        }
        if (mCar != null && mCar.isConnected()) {
            mCar.disconnect();
            mCar = null;
        }
        super.onDestroy();
    }
}
