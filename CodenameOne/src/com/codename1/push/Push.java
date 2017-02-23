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
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.ui.Display;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Utility class for sending a push message to a different device 
 * through the Codename One push servers.
 *
 * @author Shai Almog
 */
public class Push {
    
    private final String token;
    private final String body;
    private boolean production;
    private String googleAuthKey="";
    private String iosCertificateURL="";
    private String iosCertificatePassword="";
    private String wnsSID="";
    private String wnsClientSecret="";
    private int pushType=1;
    private final String[] deviceKeys;
    
    /**
     * Creates a new push notification.
     * @param token the authorization token from the account settings in the CodenameOne website, this is used
     * to associate push quotas with your app
     * @param body the body of the message
     * @param deviceKeys Device keys when sending to specific devices.
     */
    public Push(String token, String body, String... deviceKeys) {
        this.token = token;
        this.body = body;
        this.deviceKeys = deviceKeys;
    }
    
    /**
     * Sets authentication for GMS (Android and Chrome)
     * @param googleAuthKey authorization key from the google play store
     * @return self for chaining
     */
    public Push gmsAuth(String googleAuthKey) {
        this.googleAuthKey = googleAuthKey;
        return this;
    }
    
    /**
     * Sets authentication for APNS (iOS)
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @param production True if this is a production certificate.  False if this is a development certificate.
     * @return Self for chaining
     */
    public Push apnsAuth(String iosCertificateURL, String iosCertificatePassword, boolean production) {
        this.iosCertificateURL = iosCertificateURL;
        this.iosCertificatePassword = iosCertificatePassword;
        this.production = production;
        return this;
    }
    
    /**
     * Sets authenticaton for WNS (Windows 10/UWP)
     * @param wnsSID The SID from the Windows store.
     * @param wnsClientSecret The client secret from the windows store
     * @return self for chaining.
     */
    public Push wnsAuth(String wnsSID, String wnsClientSecret) {
        this.wnsSID = wnsSID;
        this.wnsClientSecret = wnsClientSecret;
        return this;
    }
    
    /**
     * Sets the type of push to use.  See developer guide for details of different push types.  Default is 1
     * @param pushType
     * @return Self for chaining.
     */
    public Push pushType(int pushType) {
        this.pushType = pushType;
        return this;
    }

    /**
     * Sends push message.
     * @return True if the request was successful.
     */
    public boolean send() {
        PushConnection cr = createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", "", wnsSID, wnsClientSecret, pushType, deviceKeys);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        return cr.successful;
    }
    
    /**
     * Sends push message asynchronously.
     */
    public void sendAsync() {
        NetworkManager.getInstance().addToQueue(createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", "", wnsSID, wnsClientSecret, pushType, deviceKeys));
    }
    
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
     * @deprecated this method sends a push using the old push servers which will be retired, you need to switch
     * to the equivalent method that accepts a push token
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
     * @deprecated this method sends a push using the old push servers which will be retired, you need to switch
     * to the equivalent method that accepts a push token
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
     * @deprecated this method sends a push using the old push servers which will be retired, you need to switch
     * to the equivalent method that accepts a push token
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
     * @deprecated this method sends a push using the old push servers which will be retired, you need to switch
     * to the equivalent method that accepts a push token
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
     * @deprecated this method sends a push using the old push servers which will be retired, you need to switch
     * to getPushKey()
     */
    public static String getDeviceKey() {
        long l = Preferences.get("push_id", (long)-1);
        if(l == -1) {
            return null;
        }
        return "" + l;
    }
    
    /**
     * Returns the push device key if the device was previously successfully registered for push
     * otherwise returns null
     * @return the device key that can be used to push to this specific device.
     */    
    public static String getPushKey() {
        String key = Preferences.get("push_key", null);
        if(key != null) {
            if(!key.startsWith("cn1-")) {
                String pushPrefix = Display.getInstance().getProperty("cn1_push_prefix", null);
                if(pushPrefix != null) {
                    return "cn1-" + pushPrefix + "-" + key;
                }
            }
        }
        return null;
    }
    

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * This method uses the new push servers
     * 
     * @param token the authorization token from the account settings in the CodenameOne website, this is used
     * to associate push quotas with your app
     * @param body the body of the message
     * @param deviceKey the device key that will receive the push message (can't be null!)
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @return true if the message reached the Codename One server successfully, this makes no guarantee
     * of delivery.
     * @deprecated Please use new builder syntax with {@link #send()} which includes parameters for new platforms such as UWP.
     */
    public static boolean sendPushMessage(String token, String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword) {
        PushConnection cr = createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", "", "", "", 1, deviceKey);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        return cr.successful;
    }
    

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * This method uses the new push servers
     * 
     * @param token the authorization token from the account settings in the CodenameOne website, this is used
     * to associate push quotas with your app
     * @param body the body of the message
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @param pushType the type for the push in the server, this is useful for sending hidden pushes (type 2) should default
     * to 0 or 1
     * @param deviceKey set of devices that should receive the push
     * @return true if the message reached the Codename One server successfully, this makes no guarantee
     * of delivery.
     * @deprecated Please use new builder syntax with {@link #send()} which includes parameters for new platforms such as UWP.
     */
    public static boolean sendPushMessage(String token, String body, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, int pushType, String... deviceKey) {
        PushConnection cr = createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", "", "", "", pushType, deviceKey);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        return cr.successful;
    }
    

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * This method uses the new push servers
     * 
     * @param token the authorization token from the account settings in the CodenameOne website, this is used
     * to associate push quotas with your app
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
     * @deprecated Please use new builder syntax with {@link #send()} which includes parameters for new platforms such as UWP.
     */
    public static boolean sendPushMessage(String token, String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, String bbUrl, String bbApp, String bbPass, String bbPort) {
        PushConnection cr = createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, bbUrl, bbApp, bbPass, bbPort, "", "", 1, deviceKey);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        return cr.successful;
    }

    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * This method uses the new push servers
     * 
     * @param token the authorization token from the account settings in the CodenameOne website, this is used
     * to associate push quotas with your app
     * @param body the body of the message
     * @param deviceKey an optional parameter (can be null) when sending to a specific device
     * @param production whether pushing to production or test/sandbox environment 
     * @param googleAuthKey authorization key from the google play store
     * @param iosCertificateURL a URL where you host the iOS certificate for this applications push capabilities. 
     * @param iosCertificatePassword the password for the push certificate
     * @deprecated Please use new builder syntax with {@link #sendAsync()} which includes parameters for new platforms such as UWP.
     */
    public static void sendPushMessageAsync(String token, String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword) {
        NetworkManager.getInstance().addToQueue(createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, "", "", "", "", "", "", 1, deviceKey));
    }
    
    /**
     * Sends a push message and returns true if server delivery succeeded, notice that the 
     * push message isn't guaranteed to reach all devices.
     * This method uses the new push servers
     * 
     * @param token the authorization token from the account settings in the CodenameOne website, this is used
     * to associate push quotas with your app
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
     * @deprecated Please use new builder syntax with {@link #sendAsync()} which includes parameters for new platforms such as UWP.
     */
    public static void sendPushMessageAsync(String token, String body, String deviceKey, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, String bbUrl, String bbApp, String bbPass, String bbPort) {
        NetworkManager.getInstance().addToQueue(createPushMessage(token, body, production, googleAuthKey, iosCertificateURL, iosCertificatePassword, bbUrl, bbApp, bbPass, bbPort, "", "", 1, deviceKey));
    }
    
    static class PushConnection extends ConnectionRequest {
        boolean successful;
            @Override
            protected void readResponse(InputStream input) throws IOException {
                JSONParser jp = new JSONParser();
                Map<String, Object> data = jp.parseJSON(new InputStreamReader(input, "UTF-8"));
                String error = (String)data.get("error");
                if(error != null) {
                    // this is an error response...
                    Log.p(error);
                    Log.p("Full error: " + data);
                    successful = false;
                } else {
                    successful = true;
                }
            }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            successful = false;
        }

        @Override
        protected void handleException(Exception err) {
            successful = false;
            Log.e(err);
        }
    }

    private static PushConnection createPushMessage(String token, String body, boolean production, String googleAuthKey, 
            String iosCertificateURL, String iosCertificatePassword, String bbUrl, String bbApp, String bbPass, String bbPort, String wnsSID, String wnsClientSecret, int type, String... deviceKeys) {
        PushConnection cr = new PushConnection();
        cr.setPost(true);
        cr.setUrl("https://push.codenameone.com/push/push");
        cr.addArgument("token", token);
        cr.addArguments("device", deviceKeys);
        cr.addArgument("type", "" +type);
        cr.addArgument("auth", googleAuthKey);
        cr.addArgument("certPassword", iosCertificatePassword);
        cr.addArgument("cert", iosCertificateURL);
        cr.addArgument("body", body);
        cr.addArgument("burl", bbUrl);
        cr.addArgument("bbAppId", bbApp);
        cr.addArgument("bbPass", bbPass);
        cr.addArgument("bbPort", bbPort);
        cr.addArgument("sid", wnsSID);
        cr.addArgument("client_secret", wnsClientSecret);
        if(production) {
            cr.addArgument("production", "true");
        } else {
            cr.addArgument("production", "false");
        }
        cr.setFailSilently(true);
        return cr;
    }

}
