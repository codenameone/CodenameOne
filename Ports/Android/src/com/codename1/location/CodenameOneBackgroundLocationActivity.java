/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author Chen
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
