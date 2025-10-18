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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;

import java.util.Hashtable;

/**
 * The GoogleConnect Login class allows the sign in with google functionality.
 * The GoogleConnect requires to create a corresponding google cloud project.
 * To enable the GoogleConnect to sign-in on the Simulator create a corresponding
 * web login - https://developers.google.com/+/web/signin/
 * <p>
 * To enable the GoogleConnect to sign-in on Android
 * Follow step 1 from here - https://developers.google.com/+/mobile/android/getting-started
 * <p>
 * To enable the GoogleConnect to sign-in on iOS
 * follow step 1 from here - https://developers.google.com/+/mobile/ios/getting-started
 *
 * @author Chen
 */
public class GoogleConnect extends Login {

    static Class implClass;
    private static final String tokenURL = "https://www.googleapis.com/oauth2/v3/token";
    private static GoogleConnect instance;

    static {
        implClass = null;
    }

    GoogleConnect() {
        setOauth2URL("https://accounts.google.com/o/oauth2/auth");
        setScope("profile email");
    }

    /**
     * Gets the GoogleConnect singleton instance
     * .
     *
     * @return the GoogleConnect instance
     */
    public static GoogleConnect getInstance() {
        if (instance == null) {
            if (implClass != null) {
                try {
                    instance = (GoogleConnect) implClass.newInstance();
                } catch (Throwable t) {
                    instance = new GoogleConnect();
                }
            } else {
                instance = new GoogleConnect();
            }
        }
        return instance;
    }

    @Override
    public boolean isNativeLoginSupported() {
        return false;
    }

    @Override
    protected Oauth2 createOauth2() {
        Hashtable params = new Hashtable();
        params.put("approval_prompt", "force");
        params.put("access_type", "offline");

        Oauth2 auth = new Oauth2(oauth2URL, clientId, redirectURI, scope, tokenURL, clientSecret, params);
        return auth;
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
        req.setUrl("https://www.googleapis.com/plus/v1/people/me");
        req.addRequestHeader("Authorization", "Bearer " + token);
        NetworkManager.getInstance().addToQueueAndWait(req);
        return retval[0];

    }

}
