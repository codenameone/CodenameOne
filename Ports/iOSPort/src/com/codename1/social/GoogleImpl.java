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


import com.codename1.impl.ios.IOSNative;
import com.codename1.io.AccessToken;
import com.codename1.ui.Display;
import java.io.IOException;

/**
 * This is an implementation to the google sign in.
 * https://developers.google.com/+/mobile/android/getting-started
 * 
 * @author Chen
 */
public class GoogleImpl extends GoogleConnect  {

    private static IOSNative nativeInterface;
    boolean loginCompleted;
    String loginMessage;

    private boolean mIntentInProgress;

    public GoogleImpl() {
    }

    public static void init(IOSNative nativeInterface) {
        GoogleConnect.implClass = GoogleImpl.class;
        GoogleImpl.nativeInterface = nativeInterface;
    }
    
    

    @Override
    public boolean isNativeLoginSupported() {
        String v = Display.getInstance().getProperty("OSVer", "6");
        return !v.startsWith("5");
    }

    @Override
    public boolean nativeIsLoggedIn() {
        return nativeInterface.isGoogleLoggedIn();
    }

    @Override
    public void nativelogin() {
        loginCompleted = false;
        nativeInterface.googleLogin(this);
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while(!loginCompleted) {
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException ie) {}
                }
            }
        });
        if(callback != null) {
            if(nativeIsLoggedIn()) {
                this.setAccessToken(new AccessToken(nativeInterface.getGoogleToken(), null));
                callback.loginSuccessful();
            } else {
                callback.loginFailed(loginMessage);
            }
        } 
    }

    @Override
    public void nativeLogout() {
       nativeInterface.googleLogout();
    }

   

}
