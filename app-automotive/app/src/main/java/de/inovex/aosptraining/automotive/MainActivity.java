package de.inovex.aosptraining.automotive;

import android.app.Activity;
import android.car.Car;
import android.car.hardware.property.CarPropertyManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.UiThread;

public class MainActivity extends Activity implements Car.CarServiceLifecycleListener {
    private final static String TAG = "MainActivity";

    private Handler handler;
    private HandlerThread handlerThread;
    private Car car;
    private CarPropertyManager carPropertyManager;
    private TextView textViewModel;
    private TextView textViewGear;

    private final NetworkRequestExample networkRequestExample = new NetworkRequestExample();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textViewModel = findViewById(R.id.textViewModel);
        textViewGear = findViewById(R.id.textViewGear);

        handlerThread = new HandlerThread("MyMainThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        car = Car.createCar(this, handler, 1_000, this );

        // Uncomment for the NetworkRequest exercise
        // networkRequestExample.requestNetwork(getApplicationContext());
        Log.i(TAG, "onCreate() done");
    }


    @Override
    public void onLifecycleChanged(Car car, boolean ready) {
        Log.i(TAG, "onLifecycleChanged: ready=" + ready);

        if (ready) {
            this.car = car;
            runOnUiThread(this::setupUI);
        }
    }

    @UiThread
    private void setupUI() {
        carPropertyManager = (CarPropertyManager) car.getCarManager(android.car.Car.PROPERTY_SERVICE);

        // Add your code here!
    }

    @Override
    public void onDestroy() {
        car.disconnect();
        // TODO: strop/halt/destroy handlerThread
        super.onDestroy();
    }
}
