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

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.LifecycleListener;
import com.codename1.impl.android.PlayServices;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

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



    static class ParcelableUtil {
        public static byte[] marshall(Parcelable parceable) {
            Parcel parcel = Parcel.obtain();
            parceable.writeToParcel(parcel, 0);
            byte[] bytes = parcel.marshall();
            parcel.recycle();
            return bytes;
        }

        public static Parcel unmarshall(byte[] bytes) {
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // This is extremely important!
            return parcel;
        }

        public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
            Parcel parcel = unmarshall(bytes);
            T result = creator.createFromParcel(parcel);
            parcel.recycle();
            return result;
        }
    }

    public final LocationCallback callback = new LocationCallback() {
        public void onLocationResult(LocationResult var1) {
            AndroidLocationPlayServiceManager.this.onLocationChanged(var1.getLastLocation());
        }

        public void onLocationAvailability(LocationAvailability var1) {

        }
    };

    public static AndroidLocationPlayServiceManager inMemoryBackgroundLocationListener;

    private GoogleApiClient mGoogleApiClient;
    private GeofencingClient geofencingClient;

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
        while (!getmGoogleApiClient().isConnected()) {
            try {
                Thread.sleep(300);
            } catch (Exception ex) {
            }
        }
        //android.location.Location location = LocationServices.FusedLocationApi.getLastLocation(getmGoogleApiClient());
        android.location.Location location = PlayServices.getInstance().getLastKnownLocation(getmGoogleApiClient());
        if (location != null) {
            return AndroidLocationManager.convert(location);
        }
        return null;
    }

    @Override
    protected void bindListener() {
        final Class bgListenerClass = getBackgroundLocationListener();
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                while (!getmGoogleApiClient().isConnected()) {
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
                        if (AndroidImplementation.getActivity() == null) {
                            // we are in the background
                            // Sometimes using regular locations in the background causes a crash
                            // so we need to use the pending intent version.
                            Context context = AndroidNativeUtil.getContext();

                            Intent intent = new Intent(context, BackgroundLocationHandler.class);
                            //there is an bug that causes this to not to workhttps://code.google.com/p/android/issues/detail?id=81812
                            //intent.putExtra("backgroundClass", getBackgroundLocationListener().getName());
                            //an ugly workaround to the putExtra bug 
                            if (bgListenerClass != null) {
                                intent.setData(Uri.parse("http://codenameone.com/a?" + bgListenerClass.getName()));
                            }
                            PendingIntent pendingIntent = AndroidImplementation.getPendingIntent(context, 0,
                                    intent);
                            inMemoryBackgroundLocationListener = AndroidLocationPlayServiceManager.this;


                            //LocationServices.FusedLocationApi.requestLocationUpdates(getmGoogleApiClient(), r, pendingIntent);
                            requestLocationUpdates(context, r, pendingIntent);
                        } else {
                            //LocationServices.FusedLocationApi.requestLocationUpdates(getmGoogleApiClient(), r, AndroidLocationPlayServiceManager.this);
                            requestLocationUpdates(AndroidNativeUtil.getContext(), r, AndroidLocationPlayServiceManager.this);
                        }
                    }
                });
            }
        });
        t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
        t.start();
    }

    private void requestLocationUpdates(Context context, LocationRequest req, PendingIntent pendingIntent) {
        PlayServices.getInstance().requestLocationUpdates(getmGoogleApiClient(), context, req, pendingIntent);
    }


    private PendingIntent createBackgroundPendingIntent() {
        return createBackgroundPendingIntent(false);
    }

    private PendingIntent createBackgroundPendingIntent(boolean forceService) {
        Context context = AndroidNativeUtil.getContext().getApplicationContext();
        final Class bgListenerClass = getBackgroundLocationListener();
        if (bgListenerClass == null) {
            return null;
        }
        if (!forceService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, BackgroundLocationBroadcastReceiver.class);
            intent.setData(Uri.parse("http://codenameone.com/a?" + bgListenerClass.getName()));
            intent.setAction(BackgroundLocationBroadcastReceiver.ACTION_PROCESS_UPDATES);
            PendingIntent pendingIntent = AndroidImplementation.getBroadcastPendingIntent(context, 0, intent);
            return pendingIntent;
        } else {


            Intent intent = new Intent(context, BackgroundLocationHandler.class);
            intent.setData(Uri.parse("http://codenameone.com/a?" + bgListenerClass.getName()));
            PendingIntent pendingIntent = AndroidImplementation.getPendingIntent(context, 0,
                    intent);
            return pendingIntent;
        }
    }

    static android.location.Location extractLocationFromIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocationResult locationResult = LocationResult.extractResult(intent);
            if (locationResult == null) {
                return null;
            }
            return locationResult.getLastLocation();
        } else {
            return intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
        }
    }

    private void requestLocationUpdates(Context context, LocationRequest req, AndroidLocationPlayServiceManager mgr) {
        PlayServices.getInstance().requestLocationUpdates(getmGoogleApiClient(), context, req, mgr);
    }

    private void removeLocationUpdates(Context context, AndroidLocationPlayServiceManager mgr) {
        PlayServices.getInstance().removeLocationUpdates(getmGoogleApiClient(), context, mgr);
    }

    private void removeLocationUpdates(Context context, PendingIntent pendingIntent) {
        PlayServices.getInstance().removeLocationUpdates(getmGoogleApiClient(), context, pendingIntent);
    }

    @Override
    protected void clearListener() {
        final Class bgListenerClass = getBackgroundLocationListener();
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                //mGoogleApiClient must be connected
                while (!getmGoogleApiClient().isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        if (inMemoryBackgroundLocationListener != null) {
                            Context context = AndroidNativeUtil.getContext();
                            Intent intent = new Intent(context, BackgroundLocationHandler.class);
                            if (bgListenerClass != null) {
                                intent.putExtra("backgroundClass", bgListenerClass.getName());
                            }
                            PendingIntent pendingIntent = AndroidImplementation.getPendingIntent(context, 0,
                                    intent);

                            //LocationServices.FusedLocationApi.removeLocationUpdates(getmGoogleApiClient(), pendingIntent);
                            removeLocationUpdates(context, pendingIntent);
                            inMemoryBackgroundLocationListener = null;
                        } else {
                            //LocationServices.FusedLocationApi.removeLocationUpdates(getmGoogleApiClient(), AndroidLocationPlayServiceManager.this);
                            removeLocationUpdates(AndroidNativeUtil.getContext(), AndroidLocationPlayServiceManager.this);
                        }
                    }
                });
            }
        });
        t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
        t.start();
    }



    @Override
    protected void bindBackgroundListener() {
        if (!checkBackgroundLocationPermission()) {
            return;
        }

        final Class bgListenerClass = getBackgroundLocationListener();
        if (bgListenerClass == null) {
            return;
        }
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                while (!getmGoogleApiClient().isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        //don't be too aggressive for location updates in the background
                        LocationRequest req = LocationRequest.create();
                        setupBackgroundLocationRequest(req);
                        Context context = AndroidNativeUtil.getContext().getApplicationContext();
                        PendingIntent pendingIntent = createBackgroundPendingIntent();
                        requestLocationUpdates(context, req, pendingIntent);
                    }
                });
            }
        });
        t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
        t.start();
    }

    private void setupBackgroundLocationRequest(LocationRequest req) {
        Display d = Display.getInstance();
        String priorityStr = d.getProperty("android.backgroundLocation.priority", "PRIORITY_BALANCED_POWER_ACCURACY");
        String fastestIntervalStr = d.getProperty("android.backgroundLocation.fastestInterval", "5000");
        String intervalStr = d.getProperty("android.backgroundLocation.interval", "10000");
        String smallestDisplacementStr = d.getProperty("android.backgroundLocation.smallestDisplacement", "50");

        int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        if ("PRIORITY_HIGH_ACCURACY".equals(priorityStr)) {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
        } else if ("PRIORITY_LOW_POWER".equals(priorityStr)) {
            priority = LocationRequest.PRIORITY_LOW_POWER;
        } else if ("PRIORITY_NO_POWER".equals(priorityStr)) {
            priority = LocationRequest.PRIORITY_NO_POWER;
        }

        long interval = Long.parseLong(intervalStr);
        long fastestInterval = Long.parseLong(fastestIntervalStr);
        int smallestDisplacement = Integer.parseInt(smallestDisplacementStr);

        req.setPriority(priority)
                .setFastestInterval(fastestInterval)
                .setInterval(interval)
                .setSmallestDisplacement(smallestDisplacement);
    }

    @Override
    protected void clearBackgroundListener() {
        final Class bgListenerClass = getBackgroundLocationListener();
        if (bgListenerClass == null) {
            return;
        }
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                //mGoogleApiClient must be connected
                while (!getmGoogleApiClient().isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        Context context = AndroidNativeUtil.getContext().getApplicationContext();
                        PendingIntent pendingIntent = createBackgroundPendingIntent();
                        removeLocationUpdates(context, pendingIntent);
                    }
                });
            }
        });
        t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
        t.start();
    }

    @Override
    public void onLocationChanged(final android.location.Location loc) {
        synchronized (this) {
            final com.codename1.location.LocationListener l = getLocationListener();
            if (l != null) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        Location lastLocation = AndroidLocationManager.convert(loc);
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

    private PendingIntent geofencePendingIntent;

    private PendingIntent createGeofencePendingIntent(Class geofenceListenerClass, com.codename1.location.Geofence gf, boolean forceService) {
        Context context = AndroidNativeUtil.getContext().getApplicationContext();


        if (!forceService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (geofencePendingIntent != null) {
                return geofencePendingIntent;
            }
            Intent intent = new Intent(context, BackgroundLocationBroadcastReceiver.class);
            intent.setAction(BackgroundLocationBroadcastReceiver.ACTION_PROCESS_GEOFENCE_TRANSITIONS);
            intent.setData(Uri.parse("http://codenameone.com/a?" + geofenceListenerClass.getName()));
            //intent.setAction(BackgroundLocationBroadcastReceiver.ACTION_PROCESS_GEOFENCE_TRANSITIONS);
            geofencePendingIntent = AndroidImplementation.getBroadcastPendingIntent(AndroidNativeUtil.getContext().getApplicationContext(), 0, intent);
            return geofencePendingIntent;
        } else {

            Intent intent = new Intent(context, GeofenceHandler.class);
            intent.putExtra("geofenceClass", geofenceListenerClass.getName());
            intent.putExtra("geofenceID", gf.getId());
            PendingIntent pendingIntent = AndroidImplementation.getPendingIntent(context, 0,
                    intent);


            return pendingIntent;
        }
    }

    @RequiresApi(api = 29)
    private boolean checkBackgroundLocationPermission() {
        if (!AndroidNativeUtil.checkForPermission("android.permission.ACCESS_BACKGROUND_LOCATION", "This is required to get the location")) {
            return false;
        }
        return true;
    }

    @Override
    public void addGeoFencing(final Class GeofenceListenerClass, final com.codename1.location.Geofence gf) {
        //Display.getInstance().scheduleBackgroundTask(new Runnable() {
        boolean fineLocationAllowed = AndroidNativeUtil.checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, "This is required to get location");
        if (fineLocationAllowed && android.os.Build.VERSION.SDK_INT >= 29) {

            if (!checkBackgroundLocationPermission()) {
                return;
            }

        } else if (!fineLocationAllowed) {
            fineLocationAllowed = AndroidNativeUtil.checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, "This is required to get the location");
        }
        if (!fineLocationAllowed) {
            Log.e(new RuntimeException("Permission denied for geofence"));
            return;
        }
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                //com.codename1.io.Log.p("PLACES add "+gf.getId()+" 1");
                while (!getmGoogleApiClient().isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                //com.codename1.io.Log.p("PLACES add "+gf.getId()+" 2");
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {
                        Context context = AndroidNativeUtil.getContext().getApplicationContext();

                        PendingIntent pendingIntent = createGeofencePendingIntent(GeofenceListenerClass, gf, false);
                        final ArrayList<Geofence> geofences = new ArrayList<Geofence>();
                        geofences.add(new Geofence.Builder()
                                .setRequestId(gf.getId())
                                .setCircularRegion(gf.getLoc().getLatitude(), gf.getLoc().getLongitude(), gf.getRadius())
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                                .setExpirationDuration(gf.getExpiration() > 0 ? gf.getExpiration() : Geofence.NEVER_EXPIRE)
                                .build());

                        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                        builder.addGeofences(geofences);

                        if (!AndroidNativeUtil.checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, "Fine location permission required")) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            System.out.println("No permission");
                            return;
                        }
                        geofencingClient = geofencingClient == null ? LocationServices.getGeofencingClient(AndroidNativeUtil.getContext().getApplicationContext()) :
                                geofencingClient;
                        geofencingClient.addGeofences(builder.build(), pendingIntent)
                                .addOnSuccessListener(AndroidNativeUtil.getActivity(), new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Geofences added
                                        // ...

                                        //com.codename1.io.Log.p("Geofence added successfully " + geofences);
                                    }
                                })
                                .addOnFailureListener(AndroidNativeUtil.getActivity(), new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Failed to add geofences
                                        // ...
                                        com.codename1.io.Log.e(e);
                                    }
                                });
                    }
                });
            }
        });
        t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
        t.start();
    }

    /**
     * Stop tracking a Geofence if isGeofenceSupported() returns false this
     * method does nothing
     *
     * @param id a Geofence id to stop tracking
     */
    public void removeGeoFencing(final String id) {
        //Display.getInstance().scheduleBackgroundTask(new Runnable() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                //wait until the client is connected, otherwise the call to
                //requestLocationUpdates will fail
                //com.codename1.io.Log.p("PLACES remove "+id);
                while (!getmGoogleApiClient().isConnected()) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                    }
                }
                //com.codename1.io.Log.p("PLACES remove "+id+" 2");
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {

                    public void run() {

                        ArrayList<String> ids = new ArrayList<String>();
                        ids.add(id);
                        geofencingClient = geofencingClient == null ? LocationServices.getGeofencingClient(AndroidNativeUtil.getContext().getApplicationContext()) :
                                geofencingClient;
                        geofencingClient.removeGeofences(ids);
                    }
                });
            }
        });
        t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
        t.start();
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
        getmGoogleApiClient(); // This should initialize it if it isn't already.
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (mGoogleApiClient != null) {
            mGoogleApiClient = null;
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
        Context context = AndroidNativeUtil.getContext();
        android.location.LocationManager locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    /**
     * @return the mGoogleApiClient
     */
    private GoogleApiClient getmGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(AndroidNativeUtil.getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }

        return mGoogleApiClient;
    }

    /**
     * @param mGoogleApiClient the mGoogleApiClient to set
     */
    private void setmGoogleApiClient(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
    }

}