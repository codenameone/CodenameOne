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
package com.codename1.impl.javase;

import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.ui.Display;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen
 */
class StubLocationManager extends LocationManager {

    private Timer timer;
    private TimerTask task;
    private Location loc = new Location();
    
    private static StubLocationManager instance = new StubLocationManager();
    
    private StubLocationManager() {
        //new york
        loc.setLongitude(-74.005973);
        loc.setLatitude(40.714353);
    }

    public static LocationManager getLocationManager(){
        return instance;
    }
    
    @Override
    public Location getCurrentLocation() throws IOException {       
        loc.setTimeStamp(System.currentTimeMillis());
        return loc;
    }
    
    @Override
    public Location getLastKnownLocation(){        
        loc.setTimeStamp(System.currentTimeMillis());
        return loc;
    }

    @Override
    protected void bindListener() {
        setStatus(AVAILABLE);
        final LocationListener l =  getLocationListener();
        task = new TimerTask() {

            @Override
            public void run() {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Location loc;
                        try {
                            loc = getCurrentLocation();
                            loc.setLongitude(loc.getLongitude() + 0.001);
                            loc.setLatitude(loc.getLatitude() + + 0.001);                    
                            l.locationUpdated(loc);
                        } catch (IOException ex) {
                            Logger.getLogger(StubLocationManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
        };
        timer = new Timer();
        timer.schedule(task, 3000, 3000);
    }

    @Override
    protected void clearListener() {
        task.cancel();
        timer.cancel();
        timer = null;
        task = null;
    }
    
}
