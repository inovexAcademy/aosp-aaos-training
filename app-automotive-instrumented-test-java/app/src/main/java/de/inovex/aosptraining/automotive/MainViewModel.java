package de.inovex.aosptraining.automotive;

import android.app.Application;
import android.car.Car;
import android.car.VehicleGear;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "AAOS_example";

    private final Car car;

    private final MutableLiveData<String> vehicleModel = new MutableLiveData<>("Unknown");
    private final MutableLiveData<String> currentGear = new MutableLiveData<>("Unknown");

    public MainViewModel(@NonNull Application application) {
        super(application);

        car = Car.createCar(
                application.getApplicationContext(),
                null,
                1_000,
                (connectedCar, ready) -> {
                    if (ready) {
                        Log.d(TAG, "Connected to CarService");

                        CarPropertyManager carPropertyManager =
                                (CarPropertyManager) connectedCar.getCarManager(Car.PROPERTY_SERVICE);

                        readVehicleModel(carPropertyManager);
                        observeCurrentGear(carPropertyManager);
                    }
                }
        );
    }

    @NonNull
    public LiveData<String> getVehicleModel() {
        return vehicleModel;
    }

    @NonNull
    public LiveData<String> getCurrentGear() {
        return currentGear;
    }

    private void readVehicleModel(@NonNull CarPropertyManager carPropertyManager) {
        Log.d(TAG, "Getting INFO_MODEL");

        CarPropertyValue<String> model = carPropertyManager.getProperty(
                VehiclePropertyIds.INFO_MODEL, 0
        );

        Log.d(TAG, "Received INFO_MODEL: " + model.getValue());

        vehicleModel.setValue(model.getValue());
    }

    private void observeCurrentGear(@NonNull CarPropertyManager carPropertyManager) {
        Log.d(TAG, "Observing CURRENT_GEAR changes");

        // Add your code to receive gear changes immediately and display the received value on the UI

    }

    @Override
    protected void onCleared() {

        // Add your code to cleanly unsubscribe from the CURRENT_GEAR property before disconnecting

        if (car != null) { // createCar could return null in case of an error
            car.disconnect();
        }
        Log.d(TAG, "Connection to the CarService closed.");

        super.onCleared();
    }
}
