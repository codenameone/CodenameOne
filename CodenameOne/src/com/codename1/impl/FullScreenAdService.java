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
package com.codename1.impl;

import com.codename1.components.InfiniteProgress;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Abstract class for fullscreen ads that appear before and possibly after application
 * execution as well as randomly between application screen transitions.
 *
 * @author Shai Almog
 */
public abstract class FullScreenAdService {    
    private boolean allowWithoutNetwork = true;
    private int timeout = 10000;
    private int adDisplayTime = 6000;
    private int timeForNext = -1;
    
    /**
     * Creates a new request for an ad
     * @return the network operation
     */
    protected abstract ConnectionRequest createAdRequest();

    /**
     * Component representing a given ad
     * @return the ad that is currently pending
     */
    protected abstract Component getPendingAd();
    
    /**
     * Just checks if an ad is already fetched
     * @return returns true if an ad is already waiting in the queue
     */
    protected abstract boolean hasPendingAd();
    
    /**
     * Removes the pending ad data so we can fetch a new ad
     */
    protected abstract void clearPendingAd();
    
    /**
     * Returns the URL for the ad
     * @return the ad URL
     */
    protected abstract String getAdDestination();
    
    /**
     * Returns true if the connection failed
     */
    protected abstract boolean failed();
    
    /**
     * Invoked on application startup, this code will download an ad or timeout 
     */
    public void showWelcomeAd() {
        ConnectionRequest r = createAdRequest();
        r.setPriority(ConnectionRequest.PRIORITY_HIGH);
        r.setTimeout(timeout);
        InfiniteProgress ip = new InfiniteProgress();
        Dialog ipDialog = ip.showInifiniteBlocking();
        NetworkManager.getInstance().addToQueueAndWait(r);
        ipDialog.dispose();
        if(failed() && !allowWithoutNetwork) {
            Dialog.show("Network Error", "Please try again later", "Exit", null);
            Display.getInstance().exitApplication();
        }
        Component c = getPendingAd();
        if(c != null) {
            Form adForm = new AdForm(c);
            adForm.setTransitionInAnimator(CommonTransitions.createEmpty());
            adForm.setTransitionOutAnimator(CommonTransitions.createEmpty());
            adForm.show();
        }
    }
        
    /**
     * Binds an ad to appear periodically after a given timeout
     * @param timeForNext the timeout in which an ad should be shown in milliseconds
     */
    public void bindTransitionAd(final int timeForNext) {
        Runnable onTransitionAndExit = new Runnable() {
            private long lastTime = System.currentTimeMillis();
            public void run() {
                long t = System.currentTimeMillis();
                if(t - lastTime > timeForNext) {
                    lastTime = t;
                    Component c = getPendingAd();
                    if(c != null) {
                        Form adForm = new AdForm(c);
                        adForm.show();
                    }
                }
            }
        };
        CodenameOneImplementation.setOnCurrentFormChange(onTransitionAndExit);
        CodenameOneImplementation.setOnExit(onTransitionAndExit);
        Timer t = new Timer();
        int tm = Math.max(5000, timeForNext - 600);
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!hasPendingAd()) {
                    ConnectionRequest r = createAdRequest();
                    r.setPriority(ConnectionRequest.PRIORITY_LOW);
                    r.setTimeout(timeout);
                    NetworkManager.getInstance().addToQueue(r);
                }
            }
        }, tm, tm);
    }

    /**
     * If set to true this flag allows the application to load even if an Ad cannot be displayed
     * @return the allowWithoutNetwork
     */
    public boolean isAllowWithoutNetwork() {
        return allowWithoutNetwork;
    }

    /**
     * If set to true this flag allows the application to load even if an Ad cannot be displayed
     * @param allowWithoutNetwork the allowWithoutNetwork to set
     */
    public void setAllowWithoutNetwork(boolean allowWithoutNetwork) {
        this.allowWithoutNetwork = allowWithoutNetwork;
    }

    /**
     * The timeout in milliseconds for an ad request
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * The timeout in milliseconds for an ad request
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the adDisplayTime
     */
    public int getAdDisplayTime() {
        return adDisplayTime;
    }

    /**
     * @param adDisplayTime the adDisplayTime to set
     */
    public void setAdDisplayTime(int adDisplayTime) {
        this.adDisplayTime = adDisplayTime;
    }
    
    class AdForm extends Form {
        boolean blocked = true;
        private long shown = -1;
        public AdForm(Component ad) {
            setLayout(new BorderLayout());
            addComponent(BorderLayout.CENTER, ad);
            Command open = new Command("Open") {
                public void actionPerformed(ActionEvent ev) {
                    Display.getInstance().execute(getAdDestination());
                }
            };
            Command skip = new Command("Skip") {
                public void actionPerformed(ActionEvent ev) {
                    blocked = false;
                }
            };
            if(Display.getInstance().isTouchScreenDevice()) {
                Container grid = new Container(new GridLayout(1, 2));
                grid.addComponent(new Button(open));
                grid.addComponent(new Button(skip));
                addComponent(BorderLayout.SOUTH, grid);
            } else {
                addCommand(open);
                addCommand(skip);
            }
            registerAnimated(this);
        }
        
        protected void onShow() {
            shown = System.currentTimeMillis();
        }

        public void show() {
            super.show();
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    try {
                        while(blocked) {
                            Thread.currentThread().sleep(100);
                        }
                    } catch(Exception err) {
                        err.printStackTrace();
                    }
                }
            });
            clearPendingAd();
        }
        
        public boolean animate() {
            if(shown > -1 && System.currentTimeMillis() - shown >= adDisplayTime) {
                blocked = false;
            }
            return false;
        }
    }
}
