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
import com.codename1.io.Log;
import com.codename1.location.Geofence;
import com.codename1.location.GeofenceListener;
import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.ui.Display;
import com.codename1.util.MathUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private Timer geofenceTimer;
    private TimerTask geofenceTask;
    
    private Timer timer;
    private TimerTask task;
    private Location loc = new Location();
    private boolean checked;
    private static StubLocationManager instance = new StubLocationManager();
    List<Geofence> geoFences = new ArrayList<Geofence>();
    List<String> insideFences = new ArrayList<String>();


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
    
    // Checks if location is inside geofence radius
    private boolean isInRegion(Location l, Geofence f) {
        return l.getDistanceTo(f.getLoc()) < f.getRadius();
    }

    @Override
    public void addGeoFencing(final Class GeofenceListenerClass, Geofence gf) {
        if (gf.getId() != null) {
            String id = gf.getId();
            int index = -1;
            for (Geofence f : geoFences) {
                if (id.equals(f.getId())) {
                    index = geoFences.indexOf(f);
                    break;
                }
            }
            if (index >= 0) {
                geoFences.remove(index);
            }
            if (gf.getRadius() < 0) {
                throw new IllegalArgumentException("Attempt to add geofence with negative radius");
            }
            
            if (gf.getRadius() < 100) {
                Log.p("Adding Geofence with a radius of "+gf.getRadius()+" metres.  On an actual device, the effective radius will vary.  Typical Android and iOS devices have a minimum geofence radius of approximately 100m");
            }
            
            long expires = gf.getExpiration();
            geoFences.add(gf);
            
            if (geofenceTimer == null) {
                geofenceTimer = new java.util.Timer();
                geofenceTask = new TimerTask() {
                    public void run() {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                Location loc;
                                try {
                                    loc = getCurrentLocation();
                                    if (JavaSEPort.locSimulation == null) {
                                        loc.setLongitude(loc.getLongitude() + 0.001);
                                        loc.setLatitude(loc.getLatitude() + +0.001);                                        
                                    } else {
                                        loc.setLongitude(JavaSEPort.locSimulation.getLongitude());
                                        loc.setLatitude(JavaSEPort.locSimulation.getLatitude());                                        
                                    } 
                                    
                                    // Do exits first
                                    for (final Geofence f : geoFences) {
                                        if (!isInRegion(loc, f) && insideFences.contains(f.getId())) {
                                            insideFences.remove(f.getId());
                                            try {
                                                final GeofenceListener l = (GeofenceListener) GeofenceListenerClass.newInstance();
                                                new Thread() {
                                                    public void run() {
                                                        // In a separate thread to simulate that
                                                        // this might not happen on EDT
                                                        l.onExit(f.getId());
                                                    }
                                                }.start();
                                                
                                            } catch (Throwable t) {
                                                Log.e(t);
                                            }
                                        }
                                    }
                                    
                                    // Do entrances next
                                    for (final Geofence f : geoFences) {
                                        if (isInRegion(loc, f) && !insideFences.contains(f.getId())) {
                                            insideFences.add(f.getId());
                                            try {
                                                final GeofenceListener l = (GeofenceListener) GeofenceListenerClass.newInstance();
                                                new Thread() {
                                                    public void run() {
                                                        // In a separate thread to simulate that
                                                        // this might not happen on EDT
                                                        l.onEntered(f.getId());
                                                    }
                                                }.start();
                                                
                                            } catch (Throwable t) {
                                                Log.e(t);
                                            }
                                        }
                                    }
                                    
                                } catch (IOException ex) {
                                    Logger.getLogger(StubLocationManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            }
                        });
                    }
                };
                geofenceTimer.schedule(geofenceTask, new Date(System.currentTimeMillis()+10000L), 10000L);
            }
            
        } else {
            Log.p("Attempt to add Geofence with null ID", Log.WARNING);
        }
        
    }

    @Override
    public void removeGeoFencing(String id) {
        int index = -1;
        for (Geofence gf : geoFences) {
            if (gf.getId() != null && gf.getId().equals(id)) {
                index = geoFences.indexOf(gf);
                break;
            }
        }
        if (index >= 0) {
            geoFences.remove(index);
        }
        if (geoFences.isEmpty()) {
            if (geofenceTimer != null) {
                geofenceTimer.cancel();
                geofenceTimer = null;
                geofenceTask = null;
            }
        }
        
    }

    @Override
    public boolean isGeofenceSupported() {
        return true;
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
