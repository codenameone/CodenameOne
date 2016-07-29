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
            AndroidImplementation.performBackgroundFetch(true);
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
