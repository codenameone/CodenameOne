/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.midp;

import com.codename1.location.Location;
import com.codename1.location.LocationManager;
import java.io.IOException;
import javax.microedition.location.Coordinates;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;

/**
 *
 * @author Chen
 */
class MIDPLocationManager extends  LocationManager implements javax.microedition.location.LocationListener{

    private Coordinates currentCoordinates;
    
    public MIDPLocationManager() {
    }

    public int getStatus() {
        try {
            LocationProvider provider = LocationProvider.getInstance(null);
            int status = converState(provider.getState());
            setStatus(status);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.getStatus();
    }
        
    public Location getCurrentLocation() throws IOException {

        try {
            LocationProvider provider = LocationProvider.getInstance(null);
            return convert(provider.getLocation(-1));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    protected void bindListener() {
        try {
            LocationProvider provider = LocationProvider.getInstance(null);
            provider.setLocationListener(this, -1, -1, -1);
        } catch (LocationException ex) {
            ex.printStackTrace();
        }

    }

    protected void clearListener() {
        try {
            LocationProvider provider = LocationProvider.getInstance(null);
            provider.setLocationListener(null, 1, 1, 1);
        } catch (LocationException ex) {
            ex.printStackTrace();
        }

    }

    public void locationUpdated(LocationProvider lp, javax.microedition.location.Location lctn) {
        com.codename1.location.LocationListener l = getLocationListener();
        if(lctn != null){
            l.locationUpdated(convert(lctn));
        }
    }

    public void providerStateChanged(LocationProvider lp, int newState) {
        com.codename1.location.LocationListener l = getLocationListener();
        l.providerStateChanged(converState(newState));
    }

    private int converState(int state){
        switch(state){
            case LocationProvider.AVAILABLE:
                    return LocationManager.AVAILABLE;
            case LocationProvider.OUT_OF_SERVICE:
                    return LocationManager.OUT_OF_SERVICE;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    return LocationManager.TEMPORARILY_UNAVAILABLE;
        }
        return LocationManager.OUT_OF_SERVICE;
    }
    
    
    private Location convert(javax.microedition.location.Location loc) {

        QualifiedCoordinates coor = loc.getQualifiedCoordinates();
        Location retVal = new Location();
        retVal.setAccuracy(coor.getHorizontalAccuracy());
        retVal.setAltitude(coor.getAltitude());

        if (currentCoordinates != null) {
            retVal.setDirection(coor.azimuthTo(currentCoordinates));
        }
        retVal.setLatitude(coor.getLatitude());
        retVal.setLongtitude(coor.getLongitude());
        retVal.setTimeStamp(loc.getTimestamp());
        retVal.setVelocity(loc.getSpeed());

        currentCoordinates = coor;
        return retVal;
    }
    
    public Location getLastKnownLocation(){        
        return convert(LocationProvider.getLastKnownLocation());
    }
    
}
