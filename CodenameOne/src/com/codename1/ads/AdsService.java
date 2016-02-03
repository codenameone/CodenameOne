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
package com.codename1.ads;

import com.codename1.components.Ads;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.html.HTMLComponent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is an abstract Ads Service.
 * Each supported Ad network needs to extend this Service and to implement 
 * the initService method
 * 
 * @author Chen Fishbein
 */
public abstract class AdsService extends ConnectionRequest {

    private String currentAd;
    private boolean initialized = false;
    private static Class service = InnerActive.class;

    /**
     * Empty constructor
     */
    protected AdsService() {
    }

    /**
     * Initialize the ads service.
     */
    public void initialize(Ads adsComponent) {
        if (!initialized) {
            initService(adsComponent);
            initialized = true;
        }
    }
    
    /**
     * init the service requests.
     */
    public abstract void initService(Ads adsComponent);

    /**
     * Creates a new AdsService to be used by the Ads Component
     */
    public static AdsService createAdsService() {
        try {
            AdsService adsService = (AdsService) service.newInstance();
            return adsService;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return null;

    }

    /**
     * Sets the provider of the ads service.
     * @param provider this class needs to extend the AdsService class
     */
    public static void setAdsProvider(Class provider){
        service = provider;
    }
    
    /**
     * Returns the last requested ad
     */
    public String getCurrentAd() {
        return currentAd;
    }

    /**
     * Schedule this service on the Network thread and executes the request
     */
    public void requestAd() {
        NetworkManager.getInstance().addToQueue(this);
    }

    /**
     * {@inheritDoc}
     */
    protected void readResponse(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int len;
        while ((len = input.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }

        int size = out.toByteArray().length;
        if (size > 0) {
            String s = new String(out.toByteArray(), 0, size, "UTF-8");
            currentAd = s;
            fireResponseListener(new ActionEvent(currentAd,ActionEvent.Type.Response));
        }
    }

    /**
     * This a callback method to inform to the service the Ad is displayed
     * @param cmp 
     */
    public void onAdDisplay(HTMLComponent cmp) {
    }

    /**
     * {@inheritDoc}
     */
    protected void handleErrorResponseCode(int code, String message) {
        //do nothing, ads failure should not interfere with application flow
        System.err.println("error=" + code + " " + message);
    }

    /**
     * {@inheritDoc}
     */
    protected void handleRuntimeException(RuntimeException err) {
        //do nothing, ads failure should not interfere with application flow
        err.printStackTrace();
    }

    /**
     * {@inheritDoc}
     */
    protected void handleException(Exception err) {
        //do nothing, ads failure should not interfere with application flow
        err.printStackTrace();
    }


}
