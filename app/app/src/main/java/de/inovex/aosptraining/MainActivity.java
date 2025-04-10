package de.inovex.aosptraining;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            Log.d(TAG, "WiFi is enabled");
        } else {
            Log.d(TAG, "WiFi is disabled");
        }

        Log.i(TAG, "onCreate() done");
    }
}
