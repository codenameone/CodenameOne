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
 * This is the Factory class to get a Location Manager instance.
 * The LocationManager is the main entry to retrieveLocation or to bind 
 * a LocationListener 
 */
public abstract class LocationManager {
    
    private static LocationListener listener;

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
     * @param listener a LocationListener or null to stop the current listener 
     * from getting updates
     */
    public void setLocationListener(final LocationListener l) {
        synchronized (this) {
            if (this.listener != null) {
                clearListener();
            }
            this.listener = l;
            if (l == null) {
                return;
            }
            bindListener();
        }
    }

    /**
     * Allows the implementation to notify the location listener of changes to location
     * @return location listener instance
     */
    protected LocationListener getLocationListener() {
        return listener;
    }
    
    /**
     * Allows the implementation to track events
     */
    protected abstract void bindListener();
    
    /**
     * Allows the implementation to track events
     */
    protected abstract void clearListener();
    
}
