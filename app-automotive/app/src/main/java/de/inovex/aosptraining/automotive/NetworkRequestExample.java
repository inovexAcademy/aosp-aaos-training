package de.inovex.aosptraining.automotive;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

/**
 * The NetworkRequestExample will create a network request needed to bring up our custom
 * network interface form the NetworkInterface exercise.
 */
public class NetworkRequestExample extends ConnectivityManager.NetworkCallback {

    static String TAG = "NetworkRequestExample";

    public void requestNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Define the network request
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        // Request one of the NetworkCapabilities provided by our interface
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
        NetworkRequest networkRequest = builder.build();

        Log.i(TAG, "connectivityManager.requestNetwork() with " + networkRequest);
        connectivityManager.requestNetwork(networkRequest, this);
    }

    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        Log.i(TAG, "onLinkPropertiesChanged:" + network.toString() + "LinkProperties:" + linkProperties);
    }

    public void onAvailable(Network network) {
        Log.i(TAG, "onAvailable:" + network.toString());
    }

}
