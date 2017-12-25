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
package com.codename1.social;

import com.codename1.facebook.FaceBookAccess;
import com.codename1.io.AccessToken;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;
import com.codename1.util.Callback;

/**
 * Invokes the native bundled facebook SDK to login/logout of facebook, notice
 * that in order for this to work server build arguments must indicate that you
 * are using the facebook sdk! To accomplish this just define:
 * facebook.appId=YourAppId in your build arguments. In order to obtain the app
 * ID you need to create a native Android/iOS application and generate the right
 * app id.
 *
 * @author Shai Almog
 */
public class FacebookConnect extends Login{

    private static FacebookConnect instance;
    static Class implClass;

    private String[] permissions = new String[]{"public_profile", "email", "user_friends"};

    FacebookConnect() {
        setOauth2URL("https://www.facebook.com/dialog/oauth");
    }


    /**
     * Gets the FacebookConnect singleton instance
     * .
     * @return the FacebookConnect instance
     */ 
    public static FacebookConnect getInstance() {
        if (instance == null) {
            if (implClass != null) {
                try {
                    instance = (FacebookConnect) implClass.newInstance();
                } catch (Throwable t) {
                    Log.e(t);
                    instance = new FacebookConnect();
                }
            } else {
                instance = new FacebookConnect();
            }
        }
        return instance;
    }

    /**
     * Indicates whether the native platform supports native facebook login
     *
     * @return true if supported
     */
    public boolean isFacebookSDKSupported() {
        return false;
    }

    /**
     * Logs into facebook, notice that this call might suspend the application
     * which might trigger repeated invocations of stop()/start() etc. This is
     * due to the facebook SDK spawning a separate process to perform the login
     * then returning to the application. Once logged in the facebook
     * credentials will be available.
     *
     * @deprecated use doLogin
     */
    public void login() {
        throw new RuntimeException("Native facebook unsupported");
    }

    
    /**
     * Logs out the current user from facebook
     *
     */
    public void doLogout() {
        super.doLogout();
        if(!isNativeLoginSupported()){
            FaceBookAccess.logOut();
        }
    }
    
    /**
     * The facebook token that can be used to access facebook functionality
     *
     * @return the token
     */
    public AccessToken getAccessToken() {
        AccessToken t = super.getAccessToken();
        if(t != null){
            return t;
        }
        return new AccessToken(getToken(), null);
    }

    /**
     * Indicates if the user is currently logged in
     *
     * @return true if logged in
     * @deprecated use isUserLoggedIn() instead
     */
    public boolean isLoggedIn() {
        throw new RuntimeException("Native facebook unsupported, if you are running on the Simulator use isUserLoggedIn");
    }

    /**
     * The facebook token that can be used to access facebook functionality
     *
     * @return the token
     * @deprecated use getAccessToken instead
     */
    public String getToken() {
        throw new RuntimeException("Native facebook unsupported, if you are running on the Simulator use getAccessToken");
    }

    /**
     * Logs out the current user from facebook
     * 
     * @deprecated use doLogout instead
     */
    public void logout() {
        throw new RuntimeException("Native facebook unsupported, if you are running on the Simulator use doLogout");
    }

    /**
     * Asks for publish permissions, this call might suspend the application
     * which might trigger repeated invocations of stop()/start().
     */
    public void askPublishPermissions(LoginCallback lc) {
        throw new RuntimeException("Native facebook unsupported");
    }

    /**
     * Returns true if the current session already has publish permissions
     *
     * @return
     */
    public boolean hasPublishPermissions() {
        throw new RuntimeException("Native facebook unsupported");
    }

    @Override
    public boolean isNativeLoginSupported() {
        return isFacebookSDKSupported();
                
    }

    @Override
    protected Oauth2 createOauth2() {
        FaceBookAccess.setClientId(clientId);
        FaceBookAccess.setClientSecret(clientSecret);
        FaceBookAccess.setRedirectURI(redirectURI);
        FaceBookAccess.setPermissions(permissions);
        return FaceBookAccess.getInstance().createOAuth();        
    }
    
    @Override
    public void nativelogin(){
        login();
    }
    
    @Override
    public void nativeLogout(){
        logout();
    }
    
    @Override
    public boolean nativeIsLoggedIn(){
        return isLoggedIn();
    }
    

    /**
     * Opens and invite dialog to invite friends to the app
     * https://developers.facebook.com/docs/app-invites
     * 
     * @param appLinkUrl App Link for what should be opened when the recipient 
     * clicks on the install/play button on the app invite page.
     * @param previewImageUrl url to an image to be used in the invite, can be null
     */ 
    public void inviteFriends(String appLinkUrl, String previewImageUrl){
    }

    /**
     * Opens and invite dialog to invite friends to the app
     * https://developers.facebook.com/docs/app-invites
     * 
     * @param appLinkUrl App Link for what should be opened when the recipient 
     * clicks on the install/play button on the app invite page.
     * @param previewImageUrl url to an image to be used in the invite, can be null
     * @param cb a Callback to be used when we need to know if the Facebook invite was successful.
     * If the invite was successful the onSucess method will be called
     * If the user canceled the onError method will be called with error code -1.
     * If an error occurred the onError method will be called with error code 0.
     * 
     */ 
    public void inviteFriends(String appLinkUrl, String previewImageUrl, final Callback cb) {
    }
    
    /**
     * Returns true if inviteFriends is implemented, it is supported on iOS and 
     * Android
     * 
     * @return true if inviteFriends is implemented
     */ 
    public boolean isInviteFriendsSupported(){
        return false;
    }

    @Override
    protected boolean validateToken(String token) {
        //make a call to the API if the return value is 40X the token is not 
        //valid anymore
        final boolean[] retval = new boolean[1];
        retval[0] = true;
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void handleErrorResponseCode(int code, String message) {
                //access token not valid anymore
                if (code >= 400 && code <= 410) {
                    retval[0] = false;
                    return;
                }
                super.handleErrorResponseCode(code, message);
            }

        };
        req.setPost(false);
        req.setUrl("https://graph.facebook.com/v2.4/me");
        req.addArgumentNoEncoding("access_token", token);
        NetworkManager.getInstance().addToQueueAndWait(req);
        return retval[0];
    }
}
