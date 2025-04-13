package de.inovex.aosptraining.graphics;

import android.app.Activity;
import android.os.Bundle;
//import android.os.Trace;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SimpleUIUpdateActivity extends Activity {
    private final static String TAG = "SimpleUIUpdateActivity";

    private Timer timer;
    int counter = 0;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_simple_ui_update_activity);
        textView = findViewById(R.id.textView);

        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //Trace.beginSection("Timer::run");
                runOnUiThread(SimpleUIUpdateActivity.this::updateCounter);
                //Trace.endSection();
            }
        }, 0, 100);
        Log.i(TAG, "onCreate() done");
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations();
    }

    void updateCounter() {
        //Trace.beginSection("updateCounter");
        counter++;
        textView.setText("Counter: " + counter);
        //Trace.endSection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        timer.cancel();
        Log.i(TAG, "onDestroy() done");
    }
}
