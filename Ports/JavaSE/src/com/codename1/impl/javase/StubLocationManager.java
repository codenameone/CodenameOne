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

import static com.codename1.impl.javase.JavaSEPort.locSimulation;
import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.ui.Display;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Chen
 */
class StubLocationManager extends LocationManager {

    private Timer timer;
    private TimerTask task;
    private Location loc = new Location();
    private boolean checked;
    private static StubLocationManager instance = new StubLocationManager();

    private StubLocationManager() {
        //new york
        Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
        double lat = p.getDouble("lastGoodLat", 40.714353);
        double lon = p.getDouble("lastGoodLon", -74.005973);
        loc.setLongitude(lon);
        loc.setLatitude(lat);
        loc.setAccuracy(p.getFloat("accuracy", 55));
        loc.setAltitude(p.getDouble("Altitude",1000));
        loc.setDirection(p.getFloat("direction", 0));
        loc.setVelocity(p.getFloat("velocity",50));
        loc.setStatus(p.getInt("state", AVAILABLE));
        if(locSimulation==null) {
                locSimulation = new LocationSimulation();
        }
        JavaSEPort.locSimulation.setMeasUnit(p.getInt("unit", LocationSimulation.E_MeasUnit_Metric));
        JavaSEPort.locSimulation.setLocation(loc);
    }

    private void checkLocationRegistration() {
        if(!checked) {
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.locationUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.locationUsageDescription", "Some functionality of the application depends on your location");
                }
            }
            checked = true;
        }
    }
    
    public static LocationManager getLocationManager() {
        return instance;
    }

    @Override
    public Location getCurrentLocation() throws IOException {
        checkLocationRegistration();
        if (JavaSEPort.locSimulation != null) {
            loc.setLatitude(JavaSEPort.locSimulation.getLatitude());
            loc.setLongitude(JavaSEPort.locSimulation.getLongitude());
            loc.setAccuracy(JavaSEPort.locSimulation.getAccuracy());
            loc.setAltitude(JavaSEPort.locSimulation.getAltitude());
            loc.setDirection(JavaSEPort.locSimulation.getDirection());
            loc.setVelocity(JavaSEPort.locSimulation.getVelocity());
            loc.setStatus(JavaSEPort.locSimulation.getState());
        }
        loc.setTimeStamp(System.currentTimeMillis());
        return loc;
    }

    @Override
    public Location getLastKnownLocation() {
        checkLocationRegistration();
        if (JavaSEPort.locSimulation != null) {
            loc.setLatitude(JavaSEPort.locSimulation.getLatitude());
            loc.setLongitude(JavaSEPort.locSimulation.getLongitude());
            loc.setAccuracy(JavaSEPort.locSimulation.getAccuracy());
            loc.setAltitude(JavaSEPort.locSimulation.getAltitude());
            loc.setDirection(JavaSEPort.locSimulation.getDirection());
            loc.setVelocity(JavaSEPort.locSimulation.getVelocity());
            loc.setStatus(JavaSEPort.locSimulation.getState());
        }
        loc.setTimeStamp(System.currentTimeMillis());
        return loc;
    }

    @Override
    protected void bindListener() {
        checkLocationRegistration();
        setStatus(AVAILABLE);
        final LocationListener l = getLocationListener();
        task = new TimerTask() {

            @Override
            public void run() {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Location loc;
                        try {
                            loc = getCurrentLocation();
                            if (JavaSEPort.locSimulation == null) {
                                loc.setLongitude(loc.getLongitude() + 0.001);
                                loc.setLatitude(loc.getLatitude() + +0.001);                                
                            }else{
                                int s = JavaSEPort.locSimulation.getState();
                                if(s != StubLocationManager.super.getStatus()){
                                    l.providerStateChanged(s);
                                    setStatus(s);
                                }
                            }
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

    @Override
    public int getStatus() {
        Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
        if (JavaSEPort.locSimulation != null) {
            int s = JavaSEPort.locSimulation.getState();
            setStatus(s);
            p.putInt("lastGoodLocationStat", s);
            return s;
        } else {
            return p.getInt("lastGoodLocationStat" ,super.getStatus());
        }
    }

}
