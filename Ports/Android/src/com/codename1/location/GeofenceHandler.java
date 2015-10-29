/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.location;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.GeofencingEvent;

/**
 *
 * @author Chen
 */
public class GeofenceHandler extends IntentService {

    public GeofenceHandler() {
        super("com.codename1.location.GeofenceHandler");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String className = intent.getStringExtra("geofenceClass");
        String id = intent.getStringExtra("geofenceID");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        try {
            GeofenceListener l = (GeofenceListener) Class.forName(className).newInstance();
            if (geofencingEvent.getGeofenceTransition() == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER) {
                l.onEntered(id);
            } else if (geofencingEvent.getGeofenceTransition() == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT) {
                l.onExit(id);
            }
        } catch (Exception e) {
            Log.e("Codename One", "geofence error", e);
        }
    }
}
