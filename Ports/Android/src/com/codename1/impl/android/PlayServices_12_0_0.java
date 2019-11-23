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
 * PlayServices implementation supporting playServicesVersion=12.0.0
 * 
 * @author shannah
 * @see PlayServices docs for description of how the BuildServer deals with these subclasses.
 */
public class PlayServices_12_0_0 extends PlayServices {

    @Override
    public Location getLastKnownLocation(GoogleApiClient apiClient) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return LocationServices.getFusedLocationProviderClient(AndroidNativeUtil.getContext()).getLastLocation().getResult();
            } catch (Throwable ex) {
                try {
                    return LocationServices.FusedLocationApi.getLastLocation(apiClient);
                } catch (Throwable t) {
                    return null;
                }
            }
        } else {
            return LocationServices.FusedLocationApi.getLastLocation(apiClient);
        }
    }

    @Override
    public void requestLocationUpdates(GoogleApiClient apiClient, Context context, LocationRequest req, PendingIntent pendingIntent) {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocationServices.getFusedLocationProviderClient(context).requestLocationUpdates(req, pendingIntent);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, req, pendingIntent);
        }
    
    }
    
    @Override
    public void removeLocationUpdates(GoogleApiClient apiClient, Context context, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(pendingIntent);
        } else {
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, pendingIntent);
        }
    }
    
    @Override
    public void requestLocationUpdates(GoogleApiClient apiClient, Context context, LocationRequest req, AndroidLocationPlayServiceManager mgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocationServices.getFusedLocationProviderClient(context).requestLocationUpdates(req, mgr.callback, Looper.getMainLooper());
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, req, mgr);
        }
    }

    @Override
    public void removeLocationUpdates(GoogleApiClient apiClient, Context context, AndroidLocationPlayServiceManager mgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(mgr.callback);
        } else {
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, mgr);
        }
    }
    
    
}
