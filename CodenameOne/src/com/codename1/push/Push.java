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
package com.codename1.push;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.ui.Display;

/**
 * Utility class for sending a push message to a different device 
 * through the Codename One push servers.
 *
 * @author Shai Almog
 */
public class Push {
    /**
     * Key for the hashtable argument when pushing to the google play store
     */
    public static final String GOOGLE_PUSH_KEY = "googlePlay";

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * 
     * @param body the body of the message
     * @param deviceKey an optional parameter (can be null) when sending to a specific device
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @return true if the message reached the Codename One server successfully, this makes no guarantee
     * of delivery.
     */
    public static boolean sendPushMessage(String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword) {
        ConnectionRequest cr = createPushMessage(body, deviceKey, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", "");
        NetworkManager.getInstance().addToQueueAndWait(cr);
        if(cr.getResposeCode() == 200) {
            return true;
        }
        return false;
    }
    

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * 
     * @param body the body of the message
     * @param deviceKey an optional parameter (can be null) when sending to a specific device
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @param bbUrl the URL to which the push should be submitted when sending a blackberry push for evaluation use https://pushapi.eval.blackberry.com
     * for production you will need to apply at https://cp310.pushapi.na.blackberry.com
     * @param bbApp the application id to authenticate on push for RIM devices
     * @param bbPass the application password credentials authenticate on push for RIM devices
     * @param bbPort the port of the blackberry push
     * @return true if the message reached the Codename One server successfully, this makes no guarantee
     * of delivery.
     */
    public static boolean sendPushMessage(String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, String bbUrl, String bbApp, String bbPass, String bbPort) {
        ConnectionRequest cr = createPushMessage(body, deviceKey, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, bbUrl, bbApp, bbPass, bbPort);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        if(cr.getResposeCode() == 200) {
            return true;
        }
        return false;
    }

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * 
     * @param body the body of the message
     * @param deviceKey an optional parameter (can be null) when sending to a specific device
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     */
    public static void sendPushMessageAsync(String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword) {
        NetworkManager.getInstance().addToQueue(createPushMessage(body, deviceKey, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", ""));
    }
    
    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * 
     * @param body the body of the message
     * @param deviceKey an optional parameter (can be null) when sending to a specific device
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @param bbUrl the URL to which the push should be submitted when sending a blackberry push for evaluation use https://pushapi.eval.blackberry.com
     * for production you will need to apply at https://cp310.pushapi.na.blackberry.com
     * @param bbApp the application id to authenticate on push for RIM devices
     * @param bbPass the application password credentials authenticate on push for RIM devices
     * @param bbPort the port of the blackberry push
     */
    public static void sendPushMessageAsync(String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, String bbUrl, String bbApp, String bbPass, String bbPort) {
        NetworkManager.getInstance().addToQueue(createPushMessage(body, deviceKey, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, bbUrl, bbApp, bbPass, bbPort));
    }

    private static ConnectionRequest createPushMessage(String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, String bbUrl, String bbApp, String bbPass, String bbPort) {
        ConnectionRequest cr = new ConnectionRequest();
        cr.setPost(true);
        cr.setUrl(Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "sendPushMessage");
        cr.addArgument("packageName", Display.getInstance().getProperty("package_name", ""));
        cr.addArgument("email", Display.getInstance().getProperty("built_by_user", ""));
        if(deviceKey != null) {
            cr.addArgument("device", deviceKey);
        }
        cr.addArgument("type", "1");
        cr.addArgument("auth", googleAuthKey);
        cr.addArgument("certPassword", iosCertificatePassword);
        cr.addArgument("cert", iosCertificateURL);
        cr.addArgument("body", body);
        cr.addArgument("burl", bbUrl);
        cr.addArgument("bbAppId", bbApp);
        cr.addArgument("bbPass", bbPass);
        cr.addArgument("bbPort", bbPort);
        if(production) {
            cr.addArgument("production", "true");
        } else {
            cr.addArgument("production", "false");
        }
        cr.setFailSilently(true);
        return cr;
    }

    /**
     * Returns the push device key if the device was previously successfully registered for push
     * otherwise returns null
     * @return the device key that can be used to push to this specific device.
     */
    public static String getDeviceKey() {
        long l = Preferences.get("push_id", (long)-1);
        if(l == -1) {
            return null;
        }
        return "" + l;
    }
}
