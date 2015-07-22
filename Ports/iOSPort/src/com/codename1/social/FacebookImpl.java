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

/**
 *
 * @author Shai Almog
 */
public class FacebookImpl extends FacebookConnect {
    boolean loginCompleted;
    boolean loginCancelled;
    private static IOSNative nativeInterface;
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
}
