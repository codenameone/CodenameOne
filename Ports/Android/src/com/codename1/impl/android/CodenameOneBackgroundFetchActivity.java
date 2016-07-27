/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import android.os.Bundle;
import android.util.Log;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.ui.Display;

/**
 * Activity for Background Fetch
 * @author Steve
 */
public class CodenameOneBackgroundFetchActivity extends CodenameOneActivity{
    private boolean shouldDeinit;
    public CodenameOneBackgroundFetchActivity() {
    }

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        Log.d("CN1", "start CodenameOneBackgroundFetchActivity");

    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if(!Display.isInitialized()) {
            Display.init(this);
            shouldDeinit = true;
        }
        try {
            AndroidImplementation.performBackgroundFetch();
        } catch (Exception e) {
            Log.e("Codename One", "Background fetch error", e);
        } finally {
            finish();
        }


    }

    protected void onDestroy() {
        Log.d("CN1", "end CodenameOneBackgroundFetchActivity");
        super.onDestroy();
        if (shouldDeinit) {
            Display.getInstance().callSerially(new Runnable() { public void run() { Display.deinitialize();} });
        }
    }

    public boolean hasUI(){
        return false;
    }

    public boolean isBackground() {
        return true;
    }
    
}
