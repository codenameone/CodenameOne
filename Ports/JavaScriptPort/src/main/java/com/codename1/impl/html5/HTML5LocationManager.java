/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.impl.html5.JSOImplementations.ErrorCallback;
import com.codename1.impl.html5.JSOImplementations.Geolocation;
import com.codename1.impl.html5.JSOImplementations.Geolocation.PositionOptions;
import com.codename1.impl.html5.JSOImplementations.PositionCallback;
import com.codename1.impl.html5.JSOImplementations.WindowExt;
import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.ui.Display;
import java.io.IOException;
import org.teavm.interop.Async;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;
import org.teavm.interop.AsyncCallback;

/**
 *
 * @author shannah
 */
public class HTML5LocationManager extends LocationManager {
    Location lastKnownLocation;
    int listenerId;
    
    public HTML5LocationManager(){
        
    }
    
    @Override
    public Location getCurrentLocation() throws IOException {
        lastKnownLocation = nativeGetCurrentLocation();
        return lastKnownLocation;
    }
    
    @JSBody(params={"o"}, script="console.log(o)")
    private static native void log(JSObject o);
    
    private static void log(String str) {
        log(JSString.valueOf(str));
    }
    
    @Async
    private static native Location nativeGetCurrentLocation() throws IOException;
    
    private static void nativeGetCurrentLocation(final AsyncCallback<Location> callback){
        //log("In nativeGetCurrentLocation");
        PositionOptions opts = (PositionOptions)((WindowExt)Window.current()).createEmptyObject();
        opts.setTimeout(5000);
        Geolocation geo = ((WindowExt)Window.current()).getNavigator().getGeolocation();
        PositionCallback onPosition = new PositionCallback() {

            @Override
            public void onLocation(final Geolocation.Position position) {
                //log("IN onLocation callback with position");
                //log(position);
                //new Thread(){
                //    public void run(){
                        Location loc = new Location();
                        loc.setLatitude(position.getCoords().getLatitude());
                        loc.setLongitude(position.getCoords().getLongitude());
                        loc.setAccuracy((float)position.getCoords().getAccuracy());
                        loc.setAltitude(position.getCoords().getAltitude());
                        loc.setDirection((float)position.getCoords().getHeading());
                        loc.setTimeStamp(position.getTimestamp());
                        loc.setVelocity((float)position.getCoords().getSpeed());
                        //log("Completing callback");
                        callback.complete(loc);
                //    }
                //}.start();
                
            }
        };
        
        ErrorCallback onError = new ErrorCallback() {

            @Override
            public void onError(JSOImplementations.JSError error) {
                //log("Error getting location");
                //log(error);
                callback.error(new IOException(error.getMessage()));
            }
        
        };
        
        geo.getCurrentPosition(onPosition, onError, opts);
    }

    @Override
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    @Override
    protected void bindListener() {
        if (listenerId==0){
            PositionOptions opts = (PositionOptions)((WindowExt)Window.current()).createEmptyObject();
            
            Geolocation geo = ((WindowExt)Window.current()).getNavigator().getGeolocation();
            
            PositionCallback onLocation = new PositionCallback(){

                @Override
                public void onLocation(final Geolocation.Position position) {
                    new Thread(){
                        public void run(){
                            final LocationListener l = HTML5LocationManager.this.getLocationListener();
                            int oldStatus = HTML5LocationManager.this.getStatus();
                            HTML5LocationManager.this.setStatus(LocationManager.AVAILABLE);
                            
                            if (oldStatus!=LocationManager.AVAILABLE && l!=null){
                                Display.getInstance().callSerially(new Runnable() {

                                    @Override
                                    public void run() {
                                        l.providerStateChanged(LocationManager.AVAILABLE);
                                    }
                                    
                                });
                            }
                            
                            Location loc = new Location();
                            loc.setLatitude(position.getCoords().getLatitude());
                            loc.setLongitude(position.getCoords().getLongitude());
                            loc.setAccuracy((float)position.getCoords().getAccuracy());
                            loc.setAltitude(position.getCoords().getAltitude());
                            loc.setDirection((float)position.getCoords().getHeading());
                            loc.setTimeStamp(position.getTimestamp());
                            loc.setVelocity((float)position.getCoords().getSpeed());
                            lastKnownLocation=loc;
                            
                            if (l != null){
                                Display.getInstance().callSerially(new Runnable(){

                                    @Override
                                    public void run() {
                                        l.locationUpdated(lastKnownLocation);
                                    }
                                    
                                });
                            }
                            
                            
                        }
                    }.start();
                    
                }
                
            };
            
            ErrorCallback onError = new ErrorCallback(){

                @Override
                public void onError(JSOImplementations.JSError error) {
                    int oldStatus = HTML5LocationManager.this.getStatus();
                    switch (error.getCode()){
                        case 1: // Permission Denied
                            HTML5LocationManager.this.setStatus(LocationManager.OUT_OF_SERVICE);
                            break;
                        case 2: // Unavailable
                            HTML5LocationManager.this.setStatus(LocationManager.TEMPORARILY_UNAVAILABLE);
                            break;
                        case 3: // TIMEOUT
                            HTML5LocationManager.this.setStatus(LocationManager.TEMPORARILY_UNAVAILABLE);
                            break;
                    }
                    
                    if (oldStatus != HTML5LocationManager.this.getStatus()) {
                        final LocationListener l = HTML5LocationManager.this.getLocationListener();
                        if (l!=null) {
                            new Thread(){
                                public void run() {
                                    Display.getInstance().callSerially(new Runnable() {

                                        @Override
                                        public void run() {
                                            l.providerStateChanged(HTML5LocationManager.this.getStatus());
                                        }
                                    });
                                }
                            }.start();
                        }
                    }
                }
                
            };
            
            geo.getCurrentPosition(onLocation, onError, opts);
        }
    }

    @Override
    protected void clearListener() {
        if (listenerId!=0) {
            Geolocation geo = ((WindowExt)Window.current()).getNavigator().getGeolocation();
            geo.clearWatch(listenerId);
        }
    }
    
}
