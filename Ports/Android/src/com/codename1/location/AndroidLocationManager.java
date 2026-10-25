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
import android.net.Uri;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.io.Log;
import static com.codename1.location.LocationManager.AVAILABLE;
import static com.codename1.location.LocationManager.OUT_OF_SERVICE;
import static com.codename1.location.LocationManager.TEMPORARILY_UNAVAILABLE;
import com.codename1.ui.Display;
import java.io.IOException;
import java.util.List;

public class AndroidLocationManager extends com.codename1.location.LocationManager implements android.location.LocationListener {
    private LocationManager locationManager;
    private String bestProvider;
    private Context context;
    private boolean searchForProvider = false;
    private static AndroidLocationManager instance;
    private long lastLocationMillis;
    private Location lastLocation;

    private AndroidLocationManager(Context ctx) {
        this.context = ctx;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    
    private class GpsStatusListener0 implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(int event) {
            boolean isGPSFix = false;
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    if (lastLocation != null) {
                        isGPSFix = (SystemClock.elapsedRealtime() - lastLocationMillis) < 10000;
                    }
                    if (isGPSFix) { // A fix has been acquired.
                        setLocationManagerStatus(AVAILABLE);
                    } else { // The fix has been lost.
                        setLocationManagerStatus(TEMPORARILY_UNAVAILABLE);
                    }
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    setLocationManagerStatus(AVAILABLE);
                    break;
            }
        }

    }
    
    private class GnssStatusListener0 extends GnssStatus.Callback {

        @Override
        public void onFirstFix(int ttffMillis) {
            setLocationManagerStatus(AVAILABLE);
            
        }

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            
            boolean isGPSFix = false;
            if (lastLocation != null) {
                isGPSFix = (SystemClock.elapsedRealtime() - lastLocationMillis) < 10000;
            }
            if (isGPSFix) { // A fix has been acquired.
                setLocationManagerStatus(AVAILABLE);
            } else { // The fix has been lost.
                setLocationManagerStatus(TEMPORARILY_UNAVAILABLE);
            }
            
        }

        @Override
        public void onStarted() {
            
        }

        @Override
        public void onStopped() {
            setLocationManagerStatus(TEMPORARILY_UNAVAILABLE);
        }
        
        
        
        

    }
    
    private GpsStatusListener0 gpsStatusListener;
    private GpsStatusListener0 gpsStatusListener() {
        if (gpsStatusListener == null) {
            gpsStatusListener = new GpsStatusListener0();

        }
        return gpsStatusListener;
    }
    
    private GnssStatusListener0 gnssStatusListener;
    private GnssStatusListener0 gnssStatusListener() {
        if (gnssStatusListener == null) {
            gnssStatusListener = new GnssStatusListener0();
        }
        return gnssStatusListener;
    }

    public static synchronized AndroidLocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new AndroidLocationManager(context);
        }
        return instance;
    }

    private String findProvider(boolean includeNetwork) {
        String providerName = null;
        Criteria criteria = new Criteria();
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(true);

        LocationProvider provider;
        boolean enabled;

        if (includeNetwork) {
            provider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
            enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (provider != null && enabled) {
                providerName = provider.getName();
            } else {
                providerName = locationManager.getBestProvider(criteria, true);
            }
        }

        if (providerName == null) {
            // If GPS provider, then create and start GPS listener
            provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (provider != null && enabled) {
                providerName = provider.getName();
            }
        }
        return providerName;
    }

    public Location getCurrentLocation() throws IOException {
        if (lastLocation != null) {
            return lastLocation;
        }
        try {
            String provider = findProvider(true);
            android.location.Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                return convert(location);
            }
        } catch (Exception err) {
            Log.e(err);
            throw new IOException(err);
        }
        throw new IOException("cannot retrieve location try later");
    }

    public void bindListener() {
        bestProvider = findProvider(false);
        if (bestProvider != null) {
            startListenToGPS();
        } else {
            searchForProvider = true;
            setLocationManagerStatus(OUT_OF_SERVICE);
            Thread t = new Thread() {
                public void run() {
                    while (searchForProvider) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                        }
                        //keep try to get the gps provider, it is very likely
                        //that the app is requesting the user to turn on the gps
                        bestProvider = findProvider(false);
                        if (bestProvider != null) {
                            setLocationManagerStatus(AVAILABLE);
                            startListenToGPS();
                            return;
                        }
                    }
                }
            };
            t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
            t.start();
        }
    }

    private void startListenToGPS() {
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {

            public void run() {
                locationManager.requestLocationUpdates(bestProvider, 0, 0, AndroidLocationManager.this);
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    // Register on start and unregister on clear. The two were
                    // swapped, so the status callback was only ever registered
                    // after the listener had been cleared.
                    locationManager.registerGnssStatusCallback(gnssStatusListener());
                } else {
                    locationManager.addGpsStatusListener(gpsStatusListener());
                }
            }
        });
    }

    public void clearListener() {
        searchForProvider = false;
        final Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {

            public void run() {
                locationManager.removeUpdates(AndroidLocationManager.this);
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    locationManager.unregisterGnssStatusCallback(gnssStatusListener());
                } else {
                    locationManager.removeGpsStatusListener(gpsStatusListener());
                }
            }
        });
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

    public void onLocationChanged(final android.location.Location loc) {
        synchronized (this) {
            final com.codename1.location.LocationListener l = getLocationListener();
            if (l != null) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        lastLocation = convert(loc);
                        l.locationUpdated(lastLocation);
                        lastLocationMillis = SystemClock.elapsedRealtime();
                    }
                });
            }
        }
    }

    

    public void onStatusChanged(String provider, int status, Bundle extras) {
        int s = convertStatus(status);
        setLocationManagerStatus(s);
    }

    private int convertStatus(int status) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                return com.codename1.location.LocationManager.AVAILABLE;
            case LocationProvider.OUT_OF_SERVICE:
                return com.codename1.location.LocationManager.OUT_OF_SERVICE;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                return com.codename1.location.LocationManager.TEMPORARILY_UNAVAILABLE;
        }
        return com.codename1.location.LocationManager.OUT_OF_SERVICE;
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public Location getLastKnownLocation() {
        if (bestProvider == null) {
            String provider = locationManager.getBestProvider(new Criteria(), false);
            android.location.Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                return convert(location);
            }
        }
        try {
            return getCurrentLocation();

        } catch (Exception e) {
        }
        return null;
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

    public boolean isGPSDetectionSupported() {
        return true;
    }

    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // ------------------------------------------------------------------
    // Background location and geofences without Google Play services.
    //
    // The Play services manager is preferred where the service exists. This is
    // what a device without it gets -- GrapheneOS, LineageOS without GApps,
    // Huawei -- and also what an application gets when the builder was told to
    // leave the Play services location library out. Both are delivered to
    // FrameworkLocationReceiver, which names no Google type.
    // ------------------------------------------------------------------

    /// PendingIntent.FLAG_MUTABLE, API 31; absent from the SDK this port compiles
    /// against. The location service adds its result to the intent as an extra,
    /// which an immutable PendingIntent would refuse.
    private static final int FLAG_MUTABLE = 33554432;

    /// LocationManager.FUSED_PROVIDER, API 31: the platform's own fused provider,
    /// part of AOSP and so present without Play services.
    private static final String FUSED_PROVIDER = "fused";

    static final String ACTION_BACKGROUND = "com.codename1.location.FRAMEWORK_BACKGROUND_LOCATION";
    static final String ACTION_GEOFENCE = "com.codename1.location.FRAMEWORK_GEOFENCE";
    static final String EXTRA_LISTENER_CLASS = "cn1ListenerClass";
    static final String GEOFENCE_SCHEME = "cn1-geofence";
    private static final Uri BACKGROUND_DATA = Uri.parse("cn1-location:background");

    @Override
    public boolean isBackgroundLocationSupported() {
        return true;
    }

    @Override
    public boolean isGeofenceSupported() {
        return true;
    }

    private static int mutableFlags(int flags) {
        return android.os.Build.VERSION.SDK_INT >= 31 ? flags | FLAG_MUTABLE : flags;
    }

    private boolean checkBackgroundPermission() {
        // ACCESS_BACKGROUND_LOCATION exists from API 29; below it the foreground
        // grant covers the background too.
        return android.os.Build.VERSION.SDK_INT < 29
                || AndroidNativeUtil.checkForPermission("android.permission.ACCESS_BACKGROUND_LOCATION",
                "This is required to get the location");
    }

    /// The provider background updates come from: the platform fused provider
    /// where there is one, else network, else GPS. A de-Googled device often has
    /// no network provider at all -- that was Play services' -- so GPS is a real
    /// answer here, not only a last resort.
    private String backgroundProvider() {
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(FUSED_PROVIDER)) {
            return FUSED_PROVIDER;
        }
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            return LocationManager.PASSIVE_PROVIDER;
        }
        return null;
    }

    private PendingIntent backgroundIntent(Class listenerClass, int flags) {
        Intent intent = new Intent(context, FrameworkLocationReceiver.class);
        intent.setAction(ACTION_BACKGROUND);
        intent.setData(BACKGROUND_DATA);
        if (listenerClass != null) {
            intent.putExtra(EXTRA_LISTENER_CLASS, listenerClass.getName());
        }
        return PendingIntent.getBroadcast(context, 0, intent, mutableFlags(flags));
    }

    /// One PendingIntent per geofence id: the id is the data URI, which is what
    /// makes two intents distinct, so removeGeoFencing(id) finds it again without
    /// knowing the listener class, which travels as an extra.
    private PendingIntent geofenceIntent(String id, Class listenerClass, int flags) {
        Intent intent = new Intent(context, FrameworkLocationReceiver.class);
        intent.setAction(ACTION_GEOFENCE);
        intent.setData(Uri.fromParts(GEOFENCE_SCHEME, id, null));
        if (listenerClass != null) {
            intent.putExtra(EXTRA_LISTENER_CLASS, listenerClass.getName());
        }
        return PendingIntent.getBroadcast(context, 0, intent, mutableFlags(flags));
    }

    private static long longProperty(String name, long defaultValue) {
        try {
            return Long.parseLong(Display.getInstance().getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException err) {
            return defaultValue;
        }
    }

    @Override
    protected void bindBackgroundListener() {
        final Class listenerClass = getBackgroundLocationListener();
        if (listenerClass == null || !checkBackgroundPermission()) {
            return;
        }
        final String provider = backgroundProvider();
        if (provider == null) {
            Log.p("No location provider is enabled; background location updates were not started");
            return;
        }
        // The same build hints the Play services manager reads.
        final long interval = longProperty("android.backgroundLocation.interval", 10000);
        final long displacement = longProperty("android.backgroundLocation.smallestDisplacement", 50);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                try {
                    locationManager.requestLocationUpdates(provider, interval, displacement,
                            backgroundIntent(listenerClass, PendingIntent.FLAG_UPDATE_CURRENT));
                } catch (SecurityException denied) {
                    Log.e(denied);
                }
            }
        });
    }

    @Override
    protected void clearBackgroundListener() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                PendingIntent pending = backgroundIntent(null, PendingIntent.FLAG_NO_CREATE);
                if (pending != null) {
                    locationManager.removeUpdates(pending);
                    pending.cancel();
                }
            }
        });
    }

    @Override
    public void addGeoFencing(final Class geofenceListenerClass, final Geofence gf) {
        if (!AndroidNativeUtil.checkForPermission("android.permission.ACCESS_FINE_LOCATION",
                "This is required to get the location") || !checkBackgroundPermission()) {
            Log.e(new RuntimeException("Permission denied for geofence"));
            return;
        }
        // Same meaning as the Play services geofence: a duration in milliseconds,
        // with zero or less for one that never expires (-1 to the platform).
        final long expiration = gf.getExpiration() > 0 ? gf.getExpiration() : -1;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                try {
                    locationManager.addProximityAlert(gf.getLoc().getLatitude(),
                            gf.getLoc().getLongitude(), gf.getRadius(), expiration,
                            geofenceIntent(gf.getId(), geofenceListenerClass,
                                    PendingIntent.FLAG_UPDATE_CURRENT));
                } catch (SecurityException denied) {
                    Log.e(denied);
                }
            }
        });
    }

    @Override
    public void removeGeoFencing(final String id) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                PendingIntent pending = geofenceIntent(id, null, PendingIntent.FLAG_NO_CREATE);
                if (pending != null) {
                    locationManager.removeProximityAlert(pending);
                    pending.cancel();
                }
            }
        });
    }

}
