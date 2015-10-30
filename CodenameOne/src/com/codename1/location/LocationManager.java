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

import com.codename1.ui.Display;
import java.io.IOException;

/**
 * The LocationManager is the main entry to retrieveLocation or to bind  a LocationListener, important: in 
 * order to use location on iOS you will need to define the build argument ios.locationUsageDescription.
 * This build argument should be used to describe to Apple &amp; the users why you need to use the location 
 * functionality.
 */
public abstract class LocationManager {
    
    private static LocationListener listener;
    
    private static LocationRequest request;

    private static Class backgroundlistener;
    
    public static final int AVAILABLE = 0;
    
    public static final int OUT_OF_SERVICE = 1;
    
    public static final int TEMPORARILY_UNAVAILABLE = 2;
    
    private int status = TEMPORARILY_UNAVAILABLE;
    
    /**
     * Gets the LocationManager instance
     * @return 
     */
    public static LocationManager getLocationManager(){
        return Display.getInstance().getLocationManager();
    }
    
    /**
     * Gets the Manager status: AVAILABLE, OUT_OF_SERVICE or TEMPORARILY_UNAVAILABLE
     * @return the status of the LoactionManager
     */
    public int getStatus(){
        return status;
    }

    /**
     * Allows the implentation to set the status of the location
     * @param status the new status
     */
    protected void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the current Location of the device, in most cases this uses the GPS. Notice! This method
     * will only return a valid value after the location listener callback returns
     * @return a Location Object
     * @throws IOException if Location cannot be retrieve from the device
     */
    public abstract Location getCurrentLocation() throws IOException;

    class LL implements Runnable, LocationListener {
        Location result;
        boolean finished;
        long timeout;
        
        public void bind() {
            setLocationListener(this);
            Display.getInstance().invokeAndBlock(this);
        }
        
        public void locationUpdated(Location location) {
            result = location;
            finished = true;
            setLocationListener(null);
        }

        public void providerStateChanged(int newState) {
            if(newState == AVAILABLE) {
                try {
                    result = getCurrentLocation();
                } catch(IOException err) {
                    err.printStackTrace();
                    result = null;
                }
            } else {
                result = null;
            }
            finished = true;
            setLocationListener(null);
        }
        
        public void run() {
            long start = System.currentTimeMillis();
            while(!finished) {
                try {
                    Thread.sleep(20);
                } catch(InterruptedException er) {}
                if(timeout > -1 && System.currentTimeMillis() - start > timeout) {
                    break;
                }
            }
        }
    }
    /**
     * Returns the current location synchronously, this is useful if you just want
     * to know the location NOW and don't care about tracking location. Notice that
     * this method will block until a result is returned so you might want to use something
     * like InfiniteProgress while this is running
     * 
     * @return the current location or null in case of an error
     */
    public Location getCurrentLocationSync() {
        return getCurrentLocationSync(-1);
    }
    
    /**
     * Returns the current location synchronously, this is useful if you just want
     * to know the location NOW and don't care about tracking location. Notice that
     * this method will block until a result is returned so you might want to use something
     * like InfiniteProgress while this is running
     * 
     * @param timeout timeout in milliseconds or -1 to never timeout
     * @return the current location or null in case of an error
     */
    public Location getCurrentLocationSync(long timeout) {
        try {
            if(getStatus() != AVAILABLE) {
                LL l = new LL();
                l.timeout = timeout;
                l.bind();
                return l.result;
            }
            return getCurrentLocation();
        } catch(IOException err) {
            err.printStackTrace();
            return null;
        }
    }
    
    /**
     * Gets the last known Location of the device.
     * 
     * @return a Location Object
     */
    public abstract Location getLastKnownLocation();
    
    /**
     * Sets a LocationListener on the device, use this method if you need to be
     * updated on the device Locations rather then calling getCurrentLocation.
     * @param l a LocationListener or null to stop the current listener 
     * from getting updates
     */
    public void setLocationListener(final LocationListener l) {
        synchronized (this) {
            if (listener != null) {
                clearListener();
                request = null;
            }
            listener = l;
            if (l == null) {
                return;
            }
            bindListener();
        }
    }
    
    /**
     * Sets a LocationListener on the device, use this method if you need to be
     * updated on the device Locations rather then calling getCurrentLocation.
     * @param l a LocationListener or null to stop the current listener 
     * from getting updates
     */
    public void setLocationListener(final LocationListener l, LocationRequest req) {
        setLocationListener(l);
        request = req;
    }

    /**
     * Use this method to track background location updates when the application 
     * is not running anymore.
     * Do not perform long operations here, iOS wake-up time is very short(around 10 seconds).
     * Notice this listener can sends events also when the app is in the foreground, therefore
     * it is recommended to check the app state before deciding how to process this event.
     * Use Display.isMinimized() to know if the app is currently running.
     * 
     * @param locationListener a class that implements the LocationListener interface
     * this class must have an empty constructor since the underlying implementation will
     * try to create an instance and invoke the locationUpdated method
     */
    public void setBackgroundLocationListener(Class locationListener) {
        synchronized (this) {
            if (backgroundlistener != null) {
                clearBackgroundListener();
            }
            backgroundlistener = locationListener;
            if (locationListener == null) {
                return;
            }
            bindBackgroundListener();
        }        
    }
    
    /**
     * Adds a geo fence listener to gets an event once the device is in/out of 
     * the Geofence range.
     * The GeoFence events can arrive in the background therefore it is 
     * recommended to check the app state before deciding how to process this event.
     * Use Display.isMinimized() to know if the app is currently running.
     * if isGeofenceSupported() returns false this method does nothing
     * 
     * <p><strong>NOTE:</strong> For iOS you must include the <code>ios.background_modes</code> build hint with a value that includes "location" for geofencing to work.</p>
     * 
     * @param listener a Class that implements the GeofenceListener interface 
     * this class must have an empty constructor
     * @param gf a Geofence to track
     */
    public void addGeoFencing(Class GeofenceListenerClass, Geofence gf) {
    }

    /**
     * Stop tracking a Geofence
     * if isGeofenceSupported() returns false this method does nothing
     * 
     * <p><strong>NOTE:</strong> For iOS you must include the <code>ios.background_modes</code> build hint with a value that includes "location" for geofencing to work.</p>
     * 
     * @param id a Geofence id to stop tracking
     */ 
    public void removeGeoFencing(String id) {
    }
    
    /**
     * Allows the implementation to notify the location listener of changes to location
     * @return location listener instance
     */
    protected LocationListener getLocationListener() {
        return listener;
    }

    /**
     * Gets the LocationRequest
     */ 
    protected LocationRequest getRequest() {
        return request;
    }    

    /**
     * Gets the LocationListener class that handles background location updates.
     * 
     * <p><strong>NOTE:</strong> For iOS you must include the
     * <code>ios.background_modes</code> build hint with a value that includes 
     * "location" for background locations to work.</p>
     */ 
    protected Class getBackgroundLocationListener() {
        return backgroundlistener;
    }
    
    /**
     * Bind the LocationListener to get events
     */
    protected abstract void bindListener();
    
    /**
     * Stop deliver events for the LocationListener
     */
    protected abstract void clearListener();

    
    /**
     * Bind the Background LocationListener to get events
     */
    protected void bindBackgroundListener(){
    }
    
    /**
     * Stop deliver events for the Background LocationListener
     */
    protected void clearBackgroundListener(){    
    }
    
    /**
     * Returns true if the platform is able to detect if the GPS is on or off.
     * see also isGPSEnabled()
     * @return true if platform is able to detect GPS on/off
     */ 
    public boolean isGPSDetectionSupported(){
        return false;
    }

    /**
     * Returns true if the platform is able to track background location.
     * 
     * <p><strong>NOTE:</strong> For iOS you must include the <code>ios.background_modes</code> build hint with a value that includes "location" for background locations to work.</p>
     * 
     * @return true if platform supports background location
     */ 
    public boolean isBackgroundLocationSupported(){
        return false;
    }

    /**
     * Returns true if the platform supports Geofence
     * 
     * <p><strong>NOTE:</strong> For iOS you must include the <code>ios.background_modes</code> build hint with a value that includes "location" for geofencing to work.</p>
     * 
     * @return true if platform supports Geofence
     */ 
    public boolean isGeofenceSupported(){
        return false;
    }
    
    /**
     * Returns GPS on/off state if isGPSDetectionSupported() returns true
     * @return true if GPS is on
     */ 
    public boolean isGPSEnabled(){
        throw new RuntimeException("GPS Detection is not supported");
    }

}
