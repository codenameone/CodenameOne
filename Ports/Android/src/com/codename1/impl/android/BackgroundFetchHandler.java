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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.codename1.background.BackgroundFetch;
import com.codename1.ui.Display;

/**
 *
 * @author shannah
 */
public class BackgroundFetchHandler extends IntentService {

    private boolean shouldStopContext=false;
    public BackgroundFetchHandler() {
        super("com.codename1.impl.android.BackgroundFetchHandler");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String[] params = intent.getDataString().split("[?]");
        if (!Display.isInitialized()) {
            shouldStopContext = true;
            AndroidImplementation.startContext(this);
            try {
                Class cls = Class.forName(params[1]);
                BackgroundFetch obj = (BackgroundFetch)cls.newInstance();
                AndroidImplementation.backgroundFetchListener = obj;
               
            } catch (Exception ex) {
                Log.d("CN1", "Failed to instantiate background fetch class "+params[1]);
                ex.printStackTrace();
                return;
            }

        } 

        try {
            AndroidImplementation.performBackgroundFetch(shouldStopContext);
        } catch (Exception e) {
            Log.e("Codename One", "background fetch error", e);
        }

        if (shouldStopContext) {
            AndroidImplementation.stopContext(this);
        }
    }


}
