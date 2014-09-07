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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a utility class for common native usages
 * 
 * @author Chen
 */
public class AndroidNativeUtil {
    private static ArrayList<LifecycleListener> listeners;
    private static Bundle activationBundle;

    /**
     * Allows us to get the bundle that was used to create this activity
     * @return the bundle instance
     */
    public static Bundle getActivationBundle() {
        return activationBundle;
    }
    
    /**
     * Binds a callback to lifecycle events
     * @param l listener
     */
    public static void addLifecycleListener(LifecycleListener l) {
        if(listeners == null) {
            listeners = new ArrayList<LifecycleListener>();
        }
        listeners.add(l);
    }
    
    /**
     * Releases the callback to lifecycle events
     * @param l listener
     */
    public static void removeLifecycleListener(LifecycleListener l) {
        if(listeners == null) {
            return;
        }
        listeners.remove(l);
        if(listeners.isEmpty()) {
            listeners = null;
        }
    }
    
    static void onCreate(Bundle savedInstanceState) {
        activationBundle = savedInstanceState;
        if(listeners != null) {
            for(LifecycleListener l : listeners) {
                l.onCreate(savedInstanceState);
            }
        }
    }
    
    static void onResume() {
        if(listeners != null) {
            for(LifecycleListener l : listeners) {
                l.onResume();
            }
        }
    }
    
    static void onPause() {
        if(listeners != null) {
            for(LifecycleListener l : listeners) {
                l.onPause();
            }
        }
    }
    
    static void onDestroy() {
        if(listeners != null) {
            for(LifecycleListener l : listeners) {
                l.onDestroy();
            }
        }
    }
    
    static void onSaveInstanceState(Bundle b) {
        if(listeners != null) {
            for(LifecycleListener l : listeners) {
                l.onSaveInstanceState(b);
            }
        }
    }
    
    static void onLowMemory() {
        if(listeners != null) {
            for(LifecycleListener l : listeners) {
                l.onLowMemory();
            }
        }
    }
            
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
    
    private static HashMap<Class, BitmapViewRenderer> viewRendererMap;
    
    public static Bitmap renderViewOnBitmap(final View v, int w, int h) {
        if(viewRendererMap != null) {
            BitmapViewRenderer br = viewRendererMap.get(v.getClass());
            if(br != null) {                
                return br.renderViewOnBitmap(v, w, h);
            }
        }
        if(w <= 0 || h <= 0) {
            return null;
        }
        final Bitmap nativeBuffer = Bitmap.createBitmap(
                        w, h, Bitmap.Config.ARGB_8888);
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            @Override
            public void run() {
                try {
                    Canvas canvas = new Canvas(nativeBuffer);
                    v.draw(canvas);
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        });
        return nativeBuffer;
    }
    
    public static void registerViewRenderer(Class viewClass, BitmapViewRenderer b) {
        if(viewRendererMap == null) {
            viewRendererMap = new HashMap<Class, BitmapViewRenderer>();
        }
        viewRendererMap.put(viewClass, b);
    }
    
    public static interface BitmapViewRenderer {
        public Bitmap renderViewOnBitmap(View v, int w, int h);
    }
}
