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
package com.codename1.system;

import static com.codename1.ui.CN.addNetworkErrorListener;
import static com.codename1.ui.CN.getCurrentForm;
import static com.codename1.ui.CN.updateNetworkThreadCount;

import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/**
 * Optional helper class that implements the Codename One lifecycle methods with reasonable default
 * implementations to help keep sample code smaller.
 */
public class Lifecycle {
    private Form current;
    private Resources theme;

    /**
     * Invoked when the app is "cold launched", this acts like a constructor
     *
     * @param context some OSs might pass a native object representing platform internal information
     */
    public void init(Object context) {
        // use two network threads instead of one
        CN.updateNetworkThreadCount(getNetworkThreadCount());

        theme = UIManager.initFirstTheme(getThemeName());

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        bindCrashProtection();

        addNetworkErrorListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent err) {
                handleNetworkError(err);
            }
        });
    }

    /**
     * Callback that can be overriden to disable or modify crash protection
     */
    protected void bindCrashProtection() {
        // Pro only feature
        Log.bindCrashProtection(true);
    }

    /**
     * Returns the default number of network thread count
     * @return currently two threads
     */
    protected int getNetworkThreadCount() {
        return 2;
    }

    /**
     * Returns the name of the global theme file, by default it's "/theme". Can be overriden by subclasses to
     * load a different file name
     * @return "/theme"
     */
    protected String getThemeName() {
        return "/theme";
    }

    /**
     * The theme instance
     * @return the theme
     */
    public Resources getTheme() {
        return theme;
    }

    /**
     * Invoked on a network error callback
     * @param err the network error event
     */
    protected void handleNetworkError(NetworkEvent err) {
        // prevent the event from propagating
        err.consume();
        if (err.getError() != null) {
            Log.e(err.getError());
        }
        Log.sendLogAsync();
        Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
    }

    /**
     * Default start callback that's invoked on application resume
     */
    public void start() {
        if(current != null){
            current.show();
            return;
        }

        runApp();
    }

    /**
     * This method is invoked by start to show the first form of the application
     */
    public void runApp() {
        Form hello = new Form("Hello", BoxLayout.y());
        hello.add(new Label("You should override runApp() with your code"));
        hello.show();
    }


    /**
     * Callback when the app is suspended
     */
    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }

    /**
     * Callback when the app is destroyed
     */
    public void destroy() {
    }
}
