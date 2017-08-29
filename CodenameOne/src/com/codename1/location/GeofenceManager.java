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

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.location.Geofence;
import com.codename1.location.GeofenceListener;
import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A utility class to simplify Geofencing in Codename One.  Using this class to manage
 * an app's Geofences works around the 20-region limit on iOS and 100-region limit on Android, so that 
 * your app can monitor an unlimited number of Geofences simulataneously.
 * 
 * <h3>How it Works</h3>
 * <p>GeofenceManager maintains a "bubble" region around the current device location.  You may register
 * as many regions as you like to be monitored with GeofenceManager, but it will only register the regions
 * that intersect the current "bubble" region.  When you exit the bubble region, the GeofenceManager will clear all of the
 * previously registered regions, create a new bubble, and then register only those regions that intersect this
 * new bubble.</p>
 * 
 * <p>GeofenceManager uses {@link Storage} to maintain its own active list of regions.</p>
 * 
 * <h3>Limitations</h3>
 * <p>GeofenceManager will only register 19 regions at a time, so if more than 19 regions intersect the current "bubble"
 * region, some of them won't make the cut.  You can set the radius of the "bubble" region using {@link #setBubbleRadius(int) } 
 * to increase or decrease the "bubble" region area, so that no regions are left behind.</p>
 * 
 * <p>Although you can set any positive radius value you like, a typical Android or iOS device has a minimum effective
 * radius of about 100m.</p>
 * 
 * <p>Note: If your app uses GeofenceManager, you shouldn't also add your own Geofences manually using {@link LocationManager#addGeoFencing(java.lang.Class, com.codename1.location.Geofence) }
 * as your manual regions may conflict.</p>
 * 
 * <h3>Usage</h3>
 * 
 * {@code
 * GeofenceManager mgr = GeofenceManager.getInstance();
 * mgr.setListenerClass(MyGeofenceListener.class);
 * mgr.add(geofence1, geofence2, geofence3);
 * mgr.update(10000);
 * }
 * 
 * And the MyGeofenceListener class should be an instance of Geofence.
 * 
 * <h3>Reloading Geofences Upon Exiting Bubble</h3>
 * 
 * <p>While there is no absolute limit on the number of regions that you can register in GeofenceManager
 * simulataneously, since it is actually storing the list of Geofences in Storage, there is a practical limit.  E.g.
 * It probably wouldn't perform well if you stored several thousand at a time.  If you want to monitor large quantifies
 * of regions (thousands, or millions), you can simply respond to the {@link GeofenceListener#onExit(java.lang.String) } event
 * for the "bubble" region, and "reload" the GeofenceManager with new regions related to the device's current location.
 * You might load the new locations from a web-service, for example.  Use the {@link #isBubble(java.lang.String)} method
 * to check if the {@literal id} parameter is for the bubble region, and act accordintly.</p>
 * 
 *
 * @author shannah
 */
public class GeofenceManager implements Iterable<Geofence> {
    //private GeoStreamerAsyncDataSource dataSource;
    private static final String STORAGE_KEY = "$AsyncGeoStreamer.geofences$";
    private static final String CURRENT_ACTIVE_KEY = "$AsyncGeoStreamer.currentActive";
    private static final String LISTENER_CLASS_KEY = "$AsyncGeoStreamer.listenerClass";
    private static final String BUBBLE_GEOFENCE_ID = "$AsyncGeoStreamer.bubble";
    
    /**
     * The radius of the bubble region (in metres)
     */
    private int bubbleRadius = 1000;
    
    /**
     * The bubble region expiraton time (duration).  Default -1 means no expiration.
     */
    private long bubbleExpiration = -1;
    
    /**
     * The Class that should be instantiated to handle Geofence events.
     */
    private Class<GeofenceListener> listenerClass;
    
    /**
     * Maintains list of currently registered geofence IDs.  Only the ones
     * that are actually currently registered with the OS.
     */
    private List<String> activeKeys;
    
    /**
     * Default timeout for getting location.
     */
    private static final int defaultTimeout = 10000;
    
    /**
     * Map of all currently registered fences.
     */
    private Map<String,Geofence> fences;
    
    private static GeofenceManager instance;
    
    /**
     * Obtains reference to the singleton GeofenceManager
     * @return 
     */
    public static GeofenceManager getInstance() {
        if (instance == null) {
            instance = new GeofenceManager();
        }
        return instance;
    }

    /**
     * Gets the radius of the "bubble" region, in metres.
     * @return the bubbleRadius
     */
    public int getBubbleRadius() {
        return bubbleRadius;
    }

    /**
     * Sets the radius of the "bubble" regin, in metres.  Default value is {@literal 1000}.
     * @param bubbleRadius the bubbleRadius to set
     */
    public void setBubbleRadius(int bubbleRadius) {
        this.bubbleRadius = bubbleRadius;
    }

    /**
     * Gets the expiration duration (in milliseconds) of the bubble region.
     * @return the bubbleExpiration
     */
    public long getBubbleExpiration() {
        return bubbleExpiration;
    }

    /**
     * Sets the expiration duration (in milliseconds) of the bubble region.  Default is {@literal -1}
     * meaning "No expiration".
     * @param bubbleExpiration the bubbleExpiration to set
     */
    public void setBubbleExpiration(long bubbleExpiration) {
        this.bubbleExpiration = bubbleExpiration;
    }
    
    /**
     * The Listener class that is registered to receive Geofence events.  This is 
     * used internally by {@link GeofenceManager}.  It is only public because
     * {@link GeofenceListener} classes must be public.
     * @deprecated For internal use only.
     */
    public static class Listener implements GeofenceListener, LocationListener  {

        @Override
        public void onExit(String id) {
            GeofenceManager.getInstance().onExit(id);
        }

        @Override
        public void onEntered(String id) {
            GeofenceManager.getInstance().onEntered(id);
        }

        @Override
        public void locationUpdated(Location location) {
            GeofenceManager.getInstance().locationUpdated(location);
        }

        @Override
        public void providerStateChanged(int newState) {
            //GeofenceManager.getInstance().providerStateChanged(newState);
        }
        
    }
    
    private GeofenceManager() {
        update(defaultTimeout);
    }
    
    /**
     * Checks if the given ID is for the "bubble" region.
     * @param id An ID to check.
     * @return True if {@literal id} is for the "bubble" region.
     */
    public boolean isBubble(String id) {
        return BUBBLE_GEOFENCE_ID.equals(id);
    }
    
    /**
     * Gets the currently registered Listener class.  
     * @return 
     */
    public synchronized Class<? extends GeofenceListener> getListenerClass() {
        if (listenerClass == null) {
            String className = (String)Storage.getInstance().readObject(LISTENER_CLASS_KEY);
            if (className != null) {
                try {
                    listenerClass = (Class)Class.forName(className);
                } catch (Throwable t){
                    Log.e(t);
                }
            }
        }
        return listenerClass;
    }
    
    /**
     * Sets the GeofenceListener class that should receive Geofence events.
     * @param c 
     */
    public synchronized void setListenerClass(Class<? extends GeofenceListener> c) {
        listenerClass = (Class)c;
        if (c == null) {
            Storage.getInstance().deleteStorageFile(LISTENER_CLASS_KEY);
        } else {
            Storage.getInstance().writeObject(LISTENER_CLASS_KEY, c.getCanonicalName());
        }
    }
    
    /**
     * 
     * @return 
     */
    private GeofenceListener getListener() {
        Class<? extends GeofenceListener> c = getListenerClass();
        if (c != null) {
            try {
                return (GeofenceListener)c.newInstance();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
        return null;
    }
    
    /**
     * Adds a set of regions to be monitored by GeofenceManager.
     * @param geofence 
     */
    public synchronized void add(Geofence... geofence) {
        Map<String, Geofence> fences = getFences(false);
        for (Geofence f : geofence) {
            fences.put(f.getId(), f);
        }
        saveFences();
    }
    
    /**
     * Adds a set of regions to be monitored by GeofenceManager.
     * @param geofences 
     */
    public synchronized void add(Collection<Geofence> geofences) {
        add(geofences.toArray(new Geofence[geofences.size()]));
    }
    
    private synchronized List<String> getActiveKeys(boolean reload) {
        if (reload || activeKeys == null) {
            activeKeys = (List<String>)Storage.getInstance().readObject(CURRENT_ACTIVE_KEY);
            if (activeKeys == null) {
                activeKeys = new ArrayList<String>();
            }
        }
        return activeKeys;
    }
    
    private synchronized void saveActiveKeys() {
        Storage.getInstance().writeObject(CURRENT_ACTIVE_KEY, getActiveKeys(false));
    }
    
    /**
     * Removes a set of regions (by ID) so that they will no longer be monitored.
     * @param ids 
     */
    public synchronized void remove(String... ids) {
        Map<String, Geofence> fences = getFences(false);
        for (String i : ids) {
            fences.remove(i);
        }
        saveFences();
    }
    
    /**
     * Removes all current regions.
     */
    public synchronized void clear() {
        Map<String, Geofence> fences = getFences(false);
        fences.clear();
        saveFences();
    }
    
    /**
     * Checks the number of regions that are currently being monitored.
     * @return 
     */
    public synchronized int size() {
        return getFences(false).size();
    }
    
    /**
     * Returns the Geofences as a Map.
     * @return 
     */
    public synchronized Map<String,Geofence> asMap() {
        return getFences(false);
    }
    
    /**
     * Returns the Geofences as a list.
     * @return 
     */
    public synchronized List<Geofence> asList() {
        return new ArrayList(getFences(false).values());
    }
    
    /**
     * Returns all Geofences sorted by distance from the current location.
     * @return 
     */
    public synchronized List<Geofence> asSortedList() {
        List<Geofence> l = asList();
        Location curr = LocationManager.getLocationManager().getLastKnownLocation();
        if (curr != null) {
            Collections.sort(l, Geofence.createDistanceComparator(curr));
        }
        return l;
    }
    
    /**
     * Reloads geofences from storage.
     */
    public synchronized void refresh() {
        getFences(true);
    }
    
    private Geofence fromMap(Map<String,Object> m) {
        double lng = (Double)m.get("lng");
        double lat = (Double)m.get("lat");
        String id = (String)m.get("id");
        int radius = (Integer)m.get("radius");
        Location l = new Location();
        l.setLatitude(lat);
        l.setLongitude(lng);
        return new Geofence(id, l, radius, 0);
    }
    
    private Map<String,Object> toMap(Geofence g) {
        double lng = g.getLoc().getLongitude();
        double lat = g.getLoc().getLatitude();
        int radius = g.getRadius();
        String id = g.getId();
        HashMap<String,Object> out = new HashMap<String,Object>();
        out.put("lng", lng);
        out.put("lat", lat);
        out.put("radius", radius);
        out.put("id", id);
        return out;
    }
    
    
    
    private synchronized Map<String, Geofence> getFences(boolean reload) {
        if (reload || fences == null) {
            fences = new HashMap<String,Geofence>();
            Map<String,Map> tmp = (Map)Storage.getInstance().readObject(STORAGE_KEY);
            if (tmp != null) {
                for (Map.Entry<String,Map> e : tmp.entrySet()) {
                    fences.put(e.getKey(), fromMap(e.getValue()));
                }
            }
        }
        return fences;
    }
    
    private synchronized void saveFences() {
        if (fences != null) {
            Map<String,Map> out = new HashMap<String,Map>();
            for (Map.Entry<String,Geofence> f : fences.entrySet()) {
                out.put(f.getValue().getId(), toMap(f.getValue()));
            }
            Storage.getInstance().writeObject(STORAGE_KEY, out);
        }
    }
    
    private boolean isWithinRadius(Location l1, Location l2, int radius) {
        return l1.getDistanceTo(l2) <= radius;
    }

    /**
     * Updates the active Geofences that are being monitored on the OS.  This should be called
     * after making changes to the set of Geofences you wish to monitor.
     * @param timeout Timeout (in milliseconds)
     */
    public synchronized void update(int timeout) {
        
        Location here = LocationManager.getLocationManager().getCurrentLocationSync(timeout);
        if (here == null) {
            LocationManager.getLocationManager().setBackgroundLocationListener(Listener.class);
            return;
        }
        LocationManager.getLocationManager().setBackgroundLocationListener(null);
        List<String> activeIds = new ArrayList<String>(getActiveKeys(false));
        List<String> activeKeys = getActiveKeys(false);
        for (String id : activeIds) {
            LocationManager.getLocationManager().removeGeoFencing(id);
            activeKeys.remove(id);
            saveActiveKeys();
        }
        
        for (Geofence g : this) {
            if (isWithinRadius(g.getLoc(), here, getBubbleRadius() + g.getRadius())) {
                if (activeKeys.size() >= 19) {
                    // only allowed 20 at a time.
                    break;
                }
                activeKeys.add(g.getId());
                LocationManager.getLocationManager().addGeoFencing(Listener.class, g);
                saveActiveKeys();
            }
        }
        Location hereCopy = new Location();
        hereCopy.setLatitude(here.getLatitude());
        hereCopy.setLongitude(here.getLongitude());
        Geofence bubble = new Geofence(BUBBLE_GEOFENCE_ID, hereCopy, getBubbleRadius(), getBubbleExpiration());
        LocationManager.getLocationManager().addGeoFencing(Listener.class, bubble);
    }

    private void onExit(String id) {
        GeofenceListener l = getListener();
        if (l != null) {
            l.onExit(id);
        }
        if (BUBBLE_GEOFENCE_ID.equals(id)) {
            // We are exiting our bubble
            update(defaultTimeout);
            
        }
    }

    private void onEntered(String id) {
        GeofenceListener l = getListener();
        if (l != null) {
            l.onEntered(id);
        }
    }

    /**
     * Iterates over all geofences that are being monitored.
     * @return 
     */
    @Override
    public Iterator<Geofence> iterator() {
        return getFences(false).values().iterator();
    }
    
    

    private  void locationUpdated(Location location) {
        update(defaultTimeout);
    }

}
