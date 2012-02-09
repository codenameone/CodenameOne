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
    
    private LocationListener listener;

    public static int AVAILABLE = 0;
    
    public static int OUT_OF_SERVICE = 1;
    
    public static int TEMPORARILY_UNAVAILABLE = 2;
    
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

    protected void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the current Location of the device, in most cases this uses the GPS
     * @return a Location Object
     * @throws IOException if Location cannot be retrieve from the device
     */
    public abstract Location getCurrentLocation() throws IOException;

    /**
     * Sets a LocationListener on the device, use this method if you need to be
     * updated on the device Locations rather then calling getCurrentLocation.
     * @param listener a LocationListener
     */
    public void setLocationListener(final LocationListener l) {
        if(this.listener != null){
            clearListener();
        }
        this.listener = l;
        if(l == null){
            return;
        }
        bindListener();
    }

    protected LocationListener getLocationListener() {
        return listener;
    }
    
    protected abstract void bindListener();
    
    protected abstract void clearListener();
    
}
