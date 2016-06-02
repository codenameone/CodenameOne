/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.location;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.ui.Display;

/**
 *
 * @author Chen
 */
public class CodenameOneBackgroundLocationActivity extends CodenameOneActivity{

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
            Display.init(this);
        }
        Bundle b = getIntent().getExtras();
        if(b != null){
            String locationClass = b.getString("backgroundLocation");
            Location location = b.getParcelable("Location");
            try {
                //the 2nd parameter is the class name we need to create
                LocationListener l = (LocationListener) Class.forName(locationClass).newInstance();
                l.locationUpdated(AndroidLocationPlayServiceManager.convert(location));
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
        Display.getInstance().callSerially(new Runnable() { public void run() { Display.deinitialize();} });
    }

    public boolean hasUI(){
        return false;
    }
    
}
