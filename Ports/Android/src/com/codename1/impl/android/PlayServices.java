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
package com.codename1.impl.android;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import com.codename1.location.AndroidLocationPlayServiceManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Base class for abstracting PlayServices functionality that may be version dependent. You should create a subclass
 * of this version for any *newer* version of PlayServices that may be required.  This base class should use only code
 * that is supported by play services version 8.3.0.  Subclasses should use the naming convention PlayServices_X_Y_Z where X_Y_Z
 * is the play services version number that it supports up to.  E.g. PlayServices_12_0_0 is a subclass that supports PlayServices
 * 12.0.0.
 * 
 * The build server will automatically choose the subclass for the latest supported play services
 * version, and will delete all of the other subclasses before compiling.
 * 
 * For example, If we have 2 subclasses, PlayServices_9_0_3 and PlayServices_12_0_0, and the user attempts a build
 * where the playServicesVersion is 10.0.  Then the build server would delete PlayServices_12_0_0.java before compiling,
 * and would use the PlayServices_9_0_3 class as the PlayServices instance, which is set
 * inside the {@link AndroidImplementation#init(java.lang.Object) } method using {@link #setInstance(com.codename1.impl.android.PlayServices) }.
 * @author shannah
 */
public class PlayServices {
    
    
    private static PlayServices instance = new PlayServices();
    public static PlayServices getInstance() {
        return instance;
    }
    
    static void setInstance(PlayServices ins) {
        instance = ins;
    }
    
    public Location getLastKnownLocation(GoogleApiClient apiClient) {
        return LocationServices.FusedLocationApi.getLastLocation(apiClient);
    }
    
    public void requestLocationUpdates(GoogleApiClient apiClient, Context context, LocationRequest req, PendingIntent pendingIntent) {
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, req, pendingIntent);
    }
    
    
    public void removeLocationUpdates(GoogleApiClient apiClient, Context context, PendingIntent pendingIntent) {
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, pendingIntent);
        
    }
    
    public void requestLocationUpdates(GoogleApiClient apiClient, Context context, LocationRequest req, AndroidLocationPlayServiceManager mgr) {
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, req, mgr);
    }

    public void removeLocationUpdates(GoogleApiClient apiClient, Context context, AndroidLocationPlayServiceManager mgr) {
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, mgr);
    }
}
