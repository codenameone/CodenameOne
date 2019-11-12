package com.codename1.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Display;
import com.google.android.gms.location.LocationResult;

import java.util.List;
/**
 * Starting with Android API 26, there are serious background execution limits.  We can 
 * no longer start a background service from the background.  Therefore, instead of 
 * using a service to receive the Background Location Notifications, we use a broadcast
 * receiver.
 * @author shannah
 */
public class BackgroundLocationBroadcastReceiver extends BroadcastReceiver {

    static final String ACTION_PROCESS_UPDATES =
            "com.codename1.location.backgroundlocationbroadcastreceiver.action" +
                    ".PROCESS_UPDATES";

    static final String INTENT = "com.codename1.location.background.locationbroadcastreceiver.INTENT";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                Location lastLocation = null;
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    for (Location loc : locations){
                        lastLocation = loc;
                    }
                } else {
                    return;
                }

                if (lastLocation == null) {
                    return;
                }
                String dataString = intent.getDataString();
                if (dataString == null) {
                    return;
                }
                String[] params = dataString.split("[?]");
                if (params.length < 2) {
                    return;
                }
                Class locationListenerClass;
                try {
                    locationListenerClass = Class.forName(params[1]);
                } catch (Throwable t) {
                    return;
                }
              
                
                boolean shouldStopWhenDone = false;
                if (!Display.isInitialized()) {
                    shouldStopWhenDone = true;
                    AndroidImplementation.startContext(context);
                }

                try {
                    //the 2nd parameter is the class name we need to create
                    LocationListener l = (LocationListener)locationListenerClass.newInstance();
                    l.locationUpdated(AndroidLocationManager.convert(lastLocation));
                } catch (Throwable e) {
                    Log.e("Codename One", "background location error", e);
                } finally {
                    if (shouldStopWhenDone) {
                        AndroidImplementation.stopContext(context);
                    }
                }
            }
        }
    }
}
