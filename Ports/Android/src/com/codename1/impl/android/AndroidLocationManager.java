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
package com.codename1.impl.android;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.codename1.location.Location;
import static com.codename1.location.LocationManager.AVAILABLE;
import static com.codename1.location.LocationManager.OUT_OF_SERVICE;
import static com.codename1.location.LocationManager.TEMPORARILY_UNAVAILABLE;
import com.codename1.ui.Display;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen
 */
public class AndroidLocationManager extends com.codename1.location.LocationManager implements android.location.LocationListener, GpsStatus.Listener {

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

    static AndroidLocationManager getInstance(Context context) {
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
            }else {
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
        if(lastLocation != null){
            return lastLocation;
        }
        String provider = findProvider(true);
        android.location.Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            return convert(location);
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
            new Thread() {
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
            }.start();
        }
    }

    private void startListenToGPS() {
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {

            public void run() {
                locationManager.requestLocationUpdates(bestProvider, 0, 0, AndroidLocationManager.this);
            }
        });
    }

    public void clearListener() {
        searchForProvider = false;
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {

            public void run() {
                locationManager.removeUpdates(AndroidLocationManager.this);
            }
        });
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
}
