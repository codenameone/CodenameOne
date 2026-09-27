/*
 * Copyright (c) 2016, Codename One and/or its affiliates. All rights reserved.
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

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.codename1.ui.Display;

/**
 * DEPRECATED!  We no longer use activities for performing background functions.  These 
 * are now handled directly in services.
 * @deprecated
 * @see BackgroundLocationHandler
 */
public class CodenameOneBackgroundLocationActivity extends Activity {

    public CodenameOneBackgroundLocationActivity() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CN1", "start CodenameOneBackgroundLocationActivity");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!Display.isInitialized()) {
            //Display.init(this);
            // This should never happen because Android will load the main activity first
            // automatically when we call startActivity()... and that will initialize the display
            Log.d("CN1", "Display is not initialized.  Cannot deliver background location update");
            finish();
            
            return;
        }
        Bundle b = getIntent().getExtras();
        if(b != null){
            String locationClass = b.getString("backgroundLocation");
            Location location = b.getParcelable("Location");
            try {
                //the 2nd parameter is the class name we need to create
                LocationListener l = (LocationListener) Class.forName(locationClass).newInstance();
                l.locationUpdated(AndroidLocationManager.convert(location));
            } catch (Exception e) {
                Log.e("Codename One", "background location error", e);
            }
            
        }
        //finish this activity once the Location has been handled
        finish();
    }

    protected void onDestroy() {
        Log.d("CN1", "end CodenameOneBackgroundLocationActivity");
        super.onDestroy();
        //Display.getInstance().callSerially(new Runnable() { public void run() { Display.deinitialize();} });
    }

    public boolean hasUI(){
        return false;
    }
    
}
