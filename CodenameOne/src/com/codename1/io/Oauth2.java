/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.io;

import com.codename1.components.WebBrowser;
import com.codename1.io.html.AsyncDocumentRequestHandlerImpl;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.html.AsyncDocumentRequestHandler.IOCallback;
import com.codename1.ui.html.DocumentInfo;
import com.codename1.ui.html.HTMLComponent;
import com.codename1.ui.layouts.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This is a utility class that allows Oauth2 authentication
 * This utility uses the Codename One XHTML Component to display the authentication
 * pages.
 * http://tools.ietf.org/pdf/draft-ietf-oauth-v2-12.pdf
 *
 * @author Chen Fishbein
 */
public class Oauth2 {

    public static final String TOKEN = "access_token";
    private String token;
    private String clientId;
    private String redirectURI;
    private String scope;
    private String clientSecret;
    private String oauth2URL;
    private String tokenRequestURL;
    private Hashtable additionalParams;
    private Dialog login;

    /**
     * Simple constructor
     *
     * @param oauth2URL the authentication url of the service
     * @param clientId the client id that would like to use the service
     * @param redirectURI the redirect uri
     * @param scope the authentication scope
     */
    public Oauth2(String oauth2URL, String clientId, String redirectURI, String scope) {
        this(oauth2URL, clientId, redirectURI, scope, null, null);
    }

    /**
     * Simple constructor
     *
     * @param oauth2URL the authentication url of the service
     * @param clientId the client id that would like to use the service
     * @param redirectURI the redirect uri
     * @param scope the authentication scope
     * @param clientSecret the client secret
     */
    public Oauth2(String oauth2URL, String clientId, String redirectURI, String scope,
            String tokenRequestURL, String clientSecret) {
        this(oauth2URL, clientId, redirectURI, scope, tokenRequestURL, clientSecret, null);
    }

    /**
     * Simple constructor
     *
     * @param oauth2URL the authentication url of the service
     * @param clientId the client id that would like to use the service
     * @param redirectURI the redirect uri
     * @param scope the authentication scope
     * @param clientSecret the client secret
     * @param additionalParams hashtable of additional parameters to the
     * authentication request
     */
    public Oauth2(String oauth2URL, String clientId, String redirectURI, String scope, String tokenRequestURL, String clientSecret, Hashtable additionalParams) {
        this.oauth2URL = oauth2URL;
        this.redirectURI = redirectURI;
        this.clientId = clientId;
        this.scope = scope;
        this.clientSecret = clientSecret;
        this.tokenRequestURL = tokenRequestURL;
        this.additionalParams = additionalParams;
    }

    /**
     * This method preforms the actual authentication, this method is a blocking
     * method that will display the user the html authentication pages.
     *
     * @return the method if passes authentication will return the access token
     * or null if authentication failed.
     *
     * @throws IOException the method will throw an IOException if something went
     * wrong in the communication.
     */
    public String authenticate() throws IOException {

        if (token == null) {
            login = new Dialog();
            boolean i = Dialog.isAutoAdjustDialogSize();
            Dialog.setAutoAdjustDialogSize(false);
            login.setLayout(new BorderLayout());
            login.setScrollable(false);

            Component html = createLoginComponent();
            login.addComponent(BorderLayout.CENTER, html);
            login.setScrollable(false);
            login.setDialogUIID("Container");
            login.setTransitionInAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 300));
            login.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 300));
            login.show(0, 0, 0, 0, false, true);
            Dialog.setAutoAdjustDialogSize(i);
        }

        return token;
    }

    private Component createLoginComponent() {

        String URL = oauth2URL + "?client_id=" + clientId
                + "&redirect_uri=" + Util.encodeUrl(redirectURI) + "&scope=" + scope;

        if(clientSecret != null){
            URL += "&response_type=code";        
        }else{
            URL += "&response_type=token";
        }


        if (additionalParams != null) {
            Enumeration e = additionalParams.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = additionalParams.get(key).toString();
                URL += "&" + key + "=" + val;
            }
        }
        
        WebBrowser web = new WebBrowser(){

            public void onStart(String url) {
                System.out.println("url " + url);
                if ((url.startsWith(redirectURI))) {
                    //remove the browser component.
                    login.removeAll();
                    login.revalidate();
                    
                    if (url.indexOf("code=") > -1) {
                        Hashtable params = getParamsFromURL(url);
                        ConnectionRequest req = new ConnectionRequest() {

                            protected void readResponse(InputStream input) throws IOException {
                                byte[] tok = Util.readInputStream(input);
                                token = new String(tok);
                                token = token.substring(token.indexOf("=") + 1, token.indexOf("&"));
                                login.dispose();
                            }
                        };

                        String URL = tokenRequestURL
                                + "?client_id=" + clientId
                                + "&redirect_uri=" + Util.encodeUrl(redirectURI)
                                + "&client_secret=" + clientSecret
                                + "&code=" + params.get("code");

                        req.setUrl(URL);
                        req.setPost(false);

                        NetworkManager.getInstance().addToQueue(req);
                    } else if (url.indexOf("error_reason=") > -1) {
                        Hashtable table = getParamsFromURL(url);
                        
                        String error = (String) table.get("error_reason");
                        String description = (String) table.get("error_description");
                        Dialog.show(error, description, "OK", "");
                        login.dispose();
                        
                    } else {
                        boolean success = url.indexOf("#") > -1;
                        if (success) {
                            String accessToken = url.substring(url.indexOf("#") + 1);
                            token = accessToken.substring(accessToken.indexOf("=") + 1, accessToken.indexOf("&"));
                            login.dispose();
                        }
                    }
                }
            }
        };
        web.setURL(URL);
        
        return web;
    }

    private Hashtable getParamsFromURL(String url) {
        int paramsStarts = url.indexOf('?');
        if (paramsStarts > -1) {
            url = url.substring(paramsStarts + 1);
        }
        Hashtable retVal = new Hashtable();

        String[] params = Util.split(url, "&");
        for (int i = 0; i < params.length; i++) {
            if (params[i].indexOf("=") > 0) {
                String[] keyVal = Util.split(params[i], "=");
                retVal.put(keyVal[0], keyVal[1]);
            }
        }
        return retVal;
    }
}
