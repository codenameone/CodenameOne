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
package com.codename1.impl.midp;

import com.codename1.location.Location;
import com.codename1.location.LocationManager;
import java.io.IOException;
import javax.microedition.location.Coordinates;
import javax.microedition.location.Criteria;
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
            Criteria c = new Criteria();
            c.setSpeedAndCourseRequired(true);
            c.setAltitudeRequired(true);
            LocationProvider provider = LocationProvider.getInstance(c);
            int status = converState(provider.getState());
            setStatus(status);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.getStatus();
    }
        
    public Location getCurrentLocation() throws IOException {

        try {
            Criteria c = new Criteria();
            c.setSpeedAndCourseRequired(true);
            c.setAltitudeRequired(true);
            LocationProvider provider = LocationProvider.getInstance(c);
            return convert(provider.getLocation(-1));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    protected void bindListener() {
        try {
            Criteria c = new Criteria();
            c.setSpeedAndCourseRequired(true);
            c.setAltitudeRequired(true);
            LocationProvider provider = LocationProvider.getInstance(c);
            provider.setLocationListener(this, -1, -1, -1);
        } catch (LocationException ex) {
            ex.printStackTrace();
        }

    }

    protected void clearListener() {
        try {
            Criteria c = new Criteria();
            c.setSpeedAndCourseRequired(true);
            c.setAltitudeRequired(true);
            LocationProvider provider = LocationProvider.getInstance(c);
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
        retVal.setLongitude(coor.getLongitude());
        retVal.setTimeStamp(loc.getTimestamp());
        retVal.setVelocity(loc.getSpeed());

        currentCoordinates = coor;
        return retVal;
    }
    
    public Location getLastKnownLocation(){        
        javax.microedition.location.Location loc = LocationProvider.getLastKnownLocation();
        if(loc != null){
            return convert(loc);
        }
        return null;
    }
    
}
