/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Intent;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Display;

/**
 * This is a utility class for common native usages
 * 
 * @author Chen
 */
public class AndroidNativeUtil {
    
    /**
     * Get the main activity
     */ 
    public static Activity getActivity(){
        return AndroidImplementation.activity;
    }
    
    /**
     * Start an intent for result
     */ 
    public static void startActivityForResult(Intent intent, final IntentResultListener listener){
        final CodenameOneActivity act = (CodenameOneActivity) getActivity();
        act.startActivityForResult(intent, 2000);
        act.setIntentResultListener(new IntentResultListener() {

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                listener.onActivityResult(requestCode, resultCode, data);
                act.restoreIntentResultListener();
            }
        });
        
        
    }
}
