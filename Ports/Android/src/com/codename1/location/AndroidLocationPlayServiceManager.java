/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.location;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.LifecycleListener;
import com.codename1.ui.Display;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;

/**
 *
 * @author Chen
 */
public class AndroidLocationPlayServiceManager extends com.codename1.location.LocationManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        LifecycleListener {

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest locationRequest;

    private static AndroidLocationPlayServiceManager instance = new AndroidLocationPlayServiceManager();

    public AndroidLocationPlayServiceManager() {
    }

    public static AndroidLocationPlayServiceManager getInstance() {
        return instance;
    }

    @Override
    public Location getCurrentLocation() throws IOException {
        Location l = getLastKnownLocation();
        if(l == null){
            throw new IOException("cannot retrieve location try later");
        }
        return l;
    }

    @Override
    public Location getLastKnownLocation() {
        android.location.Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            return convert(location);
        }
        return null;
    }

    @Override
    protected void bindListener() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                while(!mGoogleApiClient.isConnected()){
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {                        
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        LocationServices.FusedLocationApi.requestLocationUpdates(
                                mGoogleApiClient, locationRequest, AndroidLocationPlayServiceManager.this);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void clearListener() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //mGoogleApiClient must be connected
                while(!mGoogleApiClient.isConnected()){
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {                        
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, AndroidLocationPlayServiceManager.this);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onLocationChanged(final android.location.Location loc) {
        synchronized (this) {
            final com.codename1.location.LocationListener l = getLocationListener();
            if (l != null) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        Location lastLocation = convert(loc);
                        l.locationUpdated(lastLocation);
                    }
                });
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // Update location every second

        setLocationManagerStatus(AVAILABLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        setLocationManagerStatus(TEMPORARILY_UNAVAILABLE);
    }

    @Override
    public void onConnectionFailed(ConnectionResult cr) {
        setLocationManagerStatus(OUT_OF_SERVICE);
    }

    private Location convert(android.location.Location loc) {
        Location retVal = new Location();
        retVal.setAccuracy(loc.getAccuracy());
        retVal.setAltitude(loc.getAltitude());
        retVal.setLatitude(loc.getLatitude());
        retVal.setLongitude(loc.getLongitude());
        retVal.setTimeStamp(loc.getTime());
        retVal.setVelocity(loc.getSpeed());
        retVal.setDirection(loc.getBearing());
        return retVal;
    }

    private void setLocationManagerStatus(final int status) {

        int current = getStatus();
        if (current != status) {
            setStatus(status);
            synchronized (this) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        com.codename1.location.LocationListener l = getLocationListener();
                        if (l != null) {
                            l.providerStateChanged(status);
                        }
                    }
                });
            }

        }
    }

    /**
     * LifeCycle methods
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    }

    @Override
    public void onResume() {
        Context context = AndroidNativeUtil.getActivity();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }        
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
    }

    @Override
    public void onLowMemory() {
    }

}
