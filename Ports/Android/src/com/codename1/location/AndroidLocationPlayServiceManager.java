/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.LifecycleListener;
import com.codename1.ui.Display;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.ArrayList;

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
        if (l == null) {
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
                while (!mGoogleApiClient.isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        LocationRequest r = locationRequest;

                        com.codename1.location.LocationRequest request = getRequest();
                        if (request != null) {
                            LocationRequest lr = LocationRequest.create();
                            if (request.getPriority() == com.codename1.location.LocationRequest.PRIORITY_HIGH_ACCUARCY) {
                                lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            } else if (request.getPriority() == com.codename1.location.LocationRequest.PRIORITY_MEDIUM_ACCUARCY) {
                                lr.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                            } else {
                                lr.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                            }
                            lr.setInterval(request.getInterval());
                            r = lr;
                        }
                        LocationServices.FusedLocationApi.requestLocationUpdates(
                                mGoogleApiClient, r, AndroidLocationPlayServiceManager.this);
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
                while (!mGoogleApiClient.isConnected()) {
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
    protected void bindBackgroundListener() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                while (!mGoogleApiClient.isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        //don't be too aggressive for location updates in the background
                        LocationRequest req = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                                .setFastestInterval(5000L)
                                .setInterval(10000L)
                                .setSmallestDisplacement(50);

                        Context context = AndroidNativeUtil.getActivity();

                        Intent intent = new Intent(context, BackgroundLocationHandler.class);
                        intent.putExtra("backgroundClass", getBackgroundLocationListener().getName());
                        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, pendingIntent);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void clearBackgroundListener() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //mGoogleApiClient must be connected
                while (!mGoogleApiClient.isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        Context context = AndroidNativeUtil.getActivity();
                        Intent intent = new Intent(context, BackgroundLocationHandler.class);
                        intent.putExtra("backgroundClass", getBackgroundLocationListener().getName());
                        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, pendingIntent);
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

    public static Location convert(android.location.Location loc) {
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

    @Override
    public void addGeoFencing(final Class GeofenceListenerClass, final com.codename1.location.Geofence gf) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                while (!mGoogleApiClient.isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        Context context = AndroidNativeUtil.getActivity();

                        Intent intent = new Intent(context, GeofenceHandler.class);
                        intent.putExtra("geofenceClass", GeofenceListenerClass.getName());
                        intent.putExtra("geofenceID", gf.getId());
                        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                        ArrayList<Geofence> geofences = new ArrayList<Geofence>();
                        geofences.add(new Geofence.Builder()
                                .setRequestId(gf.getId())
                                .setCircularRegion(gf.getLoc().getLatitude(), gf.getLoc().getLongitude(), gf.getRadius())
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                                .setExpirationDuration(gf.getExpiration())
                                .build());
                        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofences, pendingIntent);
                    }
                });
            }
        }).start();
    }

    /**
     * Stop tracking a Geofence if isGeofenceSupported() returns false this
     * method does nothing
     *
     * @param id a Geofence id to stop tracking
     */
    public void removeGeoFencing(final String id) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                while (!mGoogleApiClient.isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {

                        ArrayList<String> ids = new ArrayList<String>();
                        ids.add(id);
                        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, ids);
                    }
                });
            }
        }).start();

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

    @Override
    public boolean isGPSDetectionSupported() {
        return true;
    }

    @Override
    public boolean isBackgroundLocationSupported() {
        return true;
    }

    @Override
    public boolean isGeofenceSupported() {
        return true;
    }

    @Override
    public boolean isGPSEnabled() {
        Context context = AndroidNativeUtil.getActivity();
        android.location.LocationManager locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

}
