/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import com.codename1.location.Location;
import java.io.IOException;

/**
 *
 * @author Chen
 */
public class AndroidLocationManager extends com.codename1.location.LocationManager implements android.location.LocationListener{

    private LocationManager locationManager;
    private String bestProvider;
    
   
    public AndroidLocationManager(Object ctx) {
        Context context = (Context) ctx;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        bestProvider = locationManager.getBestProvider(criteria, false);
    }

    public Location getCurrentLocation() throws IOException {
        android.location.Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            return convert(location);
        }
        return null;
    }

    public void bindListener() {
        locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
    }

    public void clearListener() {
        locationManager.removeUpdates(this);
    }

    private Location convert(android.location.Location loc) {
        Location retVal = new Location();
        retVal.setAccuracy(loc.getAccuracy());
        retVal.setAltitude(loc.getAltitude());
        retVal.setLatitude(loc.getLatitude());
        retVal.setLongtitude(loc.getLongitude());
        retVal.setTimeStamp(loc.getTime());
        retVal.setVelocity(loc.getSpeed());
        retVal.setDirection(loc.getBearing());
        return retVal;
    }

    public void onLocationChanged(android.location.Location loc) {
        com.codename1.location.LocationListener l = getLocationListener();
        l.locationUpdated(convert(loc));
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {        
        com.codename1.location.LocationListener l = getLocationListener();
        int s = convertStatus(status);
        setStatus(s);
        l.providerStateChanged(s);
    }

    private int convertStatus(int status){
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
    
}
