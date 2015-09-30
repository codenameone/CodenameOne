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
import com.codename1.impl.ios.IOSNative;
import com.codename1.ui.Display;
import com.codename1.util.Callback;

/**
 *
 * @author Shai Almog
 */
public class FacebookImpl extends FacebookConnect {
    boolean loginCompleted;
    boolean loginCancelled;
    private static IOSNative nativeInterface;
    static Callback inviteCallback;
    public static void init(Object n) {
        FacebookConnect.implClass = FacebookImpl.class;
        nativeInterface = (IOSNative)n;
    }
    
    @Override
    public boolean isFacebookSDKSupported() {
        String v = Display.getInstance().getProperty("OSVer", "6");
        return !v.startsWith("5");
    }

    @Override
    public void login() {
        loginCompleted = false;
        loginCancelled = false;
        nativeInterface.facebookLogin(this);
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while(!loginCompleted && !loginCancelled) {
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException ie) {}
                }
            }
        });
        if (loginCancelled) {
            return;
        }
        if(callback != null) {
            if(isLoggedIn()) {
                FaceBookAccess.setToken(getToken());
                callback.loginSuccessful();
            } else {
                callback.loginFailed("");
            }
        } 
    }
    
    @Override
    public boolean isLoggedIn() {
        return nativeInterface.isFacebookLoggedIn();
    }

    @Override
    public String getToken() {
        return nativeInterface.getFacebookToken();
    }

    @Override
    public void logout() {
        nativeInterface.facebookLogout();
    }

    @Override
    public void askPublishPermissions(final LoginCallback lc){
        if(!isLoggedIn()) {
            setCallback(new LoginCallback() {
                public void loginSuccessful() {
                    askPublishPermissions(lc);
                }
            });
            login();
            return;
        }
        nativeInterface.askPublishPermissions(lc);
    }
    
    @Override
    public boolean hasPublishPermissions(){
        return nativeInterface.hasPublishPermissions();
    }
    
    /**
     * Opens and invite dialog to invite friends to the app
     * https://developers.facebook.com/docs/app-invites
     * 
     * @param appLinkUrl App Link for what should be opened when the recipient 
     * clicks on the install/play button on the app invite page.
     * @param previewImageUrl url to an image to be used in the invite, can be null
     */ 
    @Override
    public void inviteFriends(String appLinkUrl, String previewImageUrl){
        inviteFriends(appLinkUrl, previewImageUrl, null);
    }

    @Override
    public void inviteFriends(String appLinkUrl, String previewImageUrl, Callback cb) {
        inviteCallback = cb;
        nativeInterface.inviteFriends(appLinkUrl, previewImageUrl);
        
    }
    
    /**
     * Callback called from native code
     * See - (void)appInviteDialog:(FBSDKAppInviteDialog *)appInviteDialog didCompleteWithResults:(NSDictionary *)results {
     * in CodenameOne_GLViewController.m
     */
    static void inviteDidCompleteSuccessfully() {
        if (inviteCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if (inviteCallback != null) {
                        inviteCallback.onSucess(null);
                        inviteCallback = null;
                    }
                }
            });
            
        }
                
    }
    
    /**
     * Callback called from native code
     * See - (void)appInviteDialog:(FBSDKAppInviteDialog *)appInviteDialog didFailWithError:(NSError *)error
     * In CodenameOne_GLViewController.m
     * @param error 
     */
    static void inviteDidFailWithError(final int code, final String error) {
        if (inviteCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if (inviteCallback != null) {
                        inviteCallback.onError(null, new RuntimeException(error), code, error);
                        inviteCallback = null;
                    }
                }
            });
            
        }
                
    }
    
    /**
     * Returns true if inviteFriends is implemented, it is supported on iOS and 
     * Android
     * 
     * @return true if inviteFriends is implemented
     */ 
    @Override
    public boolean isInviteFriendsSupported(){
        return true;
    }
    
}
