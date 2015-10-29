/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.location;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderApi;

/**
 *
 * @author Chen
 */
public class BackgroundLocationHandler extends IntentService {

    public BackgroundLocationHandler() {
        super("com.codename1.location.BackgroundLocationHandler");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String className = intent.getStringExtra("backgroundClass");
        final android.location.Location location = intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);

        try {
            LocationListener l = (LocationListener) Class.forName(className).newInstance();
            l.locationUpdated(AndroidLocationPlayServiceManager.convert(location));
        } catch (Exception e) {
            Log.e("Codename One", "background location error", e);
        }
    }
}
