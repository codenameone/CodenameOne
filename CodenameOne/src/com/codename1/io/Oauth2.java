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

import com.codename1.components.InfiniteProgress;
import com.codename1.components.WebBrowser;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.html.DocumentInfo;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.regex.StringReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

/**
 * This is a utility class that allows Oauth2 authentication This utility uses
 * the Codename One XHTML Component to display the authentication pages.
 * http://tools.ietf.org/pdf/draft-ietf-oauth-v2-12.pdf
 *
 * @author Chen Fishbein
 */
public class Oauth2 {

    public static final String TOKEN = "access_token";

    /**
     * Enables going back to the parent form after login is completed
     *
     * @return the backToParent
     */
    public static boolean isBackToParent() {
        return backToParent;
    }

    /**
     * Enables going back to the parent form after login is completed
     *
     * @param aBackToParent the backToParent to set
     */
    public static void setBackToParent(boolean aBackToParent) {
        backToParent = aBackToParent;
    }
    private String token;
    private static String expires;
    private String clientId;
    private String redirectURI;
    private String scope;
    private String clientSecret;
    private String oauth2URL;
    private String tokenRequestURL;
    private Hashtable additionalParams;
    private Dialog login;
    private static boolean backToParent = true;

    /**
     * Simple constructor
     *
     * @param oauth2URL the authentication url of the service
     * @param clientId the client id that would like to use the service
     * @param redirectURI the redirect uri
     */
    public Oauth2(String oauth2URL, String clientId, String redirectURI) {
        this(oauth2URL, clientId, redirectURI, null, null, null);
    }

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
     * Returns the expiry for the token received via oauth
     *
     * @return the expires argument for the token
     */
    public static String getExpires() {
        return expires;
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
     * @throws IOException the method will throw an IOException if something
     * went wrong in the communication.
     * @deprecated use createAuthComponent or showAuthentication which work
     * asynchronously and adapt better to different platforms
     */
    public String authenticate() {

        if (token == null) {
            login = new Dialog();
            boolean i = Dialog.isAutoAdjustDialogSize();
            Dialog.setAutoAdjustDialogSize(false);
            login.setLayout(new BorderLayout());
            login.setScrollable(false);

            Component html = createLoginComponent(null, null, null, null);
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

    /**
     * This method creates a component which can authenticate. You will receive
     * either the authentication key or an Exception object within the
     * ActionListener callback method.
     *
     * @param al a listener that will receive at its source either a token for
     * the service or an exception in case of a failure
     * @return a component that should be displayed to the user in order to
     * perform the authentication
     */
    public Component createAuthComponent(ActionListener al) {
        return createLoginComponent(al, null, null, null);
    }

    /**
     * This method shows an authentication for login form
     *
     * @param al a listener that will receive at its source either a token for
     * the service or an exception in case of a failure
     * @return a component that should be displayed to the user in order to
     * perform the authentication
     */
    public void showAuthentication(ActionListener al) {
        final Form old = Display.getInstance().getCurrent();
        InfiniteProgress inf = new InfiniteProgress();
        final Dialog progress = inf.showInifiniteBlocking();
        Form authenticationForm = new Form("Login");
        authenticationForm.setScrollable(false);
        if (old != null) {
            Command cancel = new Command("Cancel") {
                public void actionPerformed(ActionEvent ev) {
                    if (Display.getInstance().getCurrent() == progress) {
                        progress.dispose();
                    }
                    old.showBack();
                }
            };
            authenticationForm.addCommand(cancel);
            authenticationForm.setBackCommand(cancel);
        }
        authenticationForm.setLayout(new BorderLayout());
        authenticationForm.addComponent(BorderLayout.CENTER, createLoginComponent(al, authenticationForm, old, progress));
    }

    private Component createLoginComponent(final ActionListener al, final Form frm, final Form backToForm, final Dialog progress) {

        String URL = oauth2URL + "?client_id=" + clientId
                + "&redirect_uri=" + Util.encodeUrl(redirectURI);
        if (scope != null) {
            URL += "&scope=" + scope;
        }
        if (clientSecret != null) {
            URL += "&response_type=code";
        } else {
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

        DocumentInfo.setDefaultEncoding(DocumentInfo.ENCODING_UTF8);
        final WebBrowser[] web = new WebBrowser[1];
        web[0] = new WebBrowser() {

            @Override
            public void onLoad(String url) {
                handleURL(url, this, al, frm, backToForm, progress);
            }

            public void onStart(String url) {
            }
        };
        web[0].setURL(URL);

        return web[0];
    }

    private void handleURL(String url, WebBrowser web, final ActionListener al, final Form frm, final Form backToForm, final Dialog progress) {
        if ((url.startsWith(redirectURI))) {
            if (Display.getInstance().getCurrent() == progress) {
                progress.dispose();
            }

            web.stop();

            //remove the browser component.
            if (login != null) {
                login.removeAll();
                login.revalidate();
            }

            if (url.indexOf("code=") > -1) {
                Hashtable params = getParamsFromURL(url);
                ConnectionRequest req = new ConnectionRequest() {

                    protected void readResponse(InputStream input) throws IOException {
                        byte[] tok = Util.readInputStream(input);
                        String t = new String(tok);
                        
                        if(t.startsWith("{")){
                            JSONParser p = new JSONParser();
                            Map map = p.parseJSON(new StringReader(t));
                            token = (String) map.get("access_token");
                            Object ex = map.get("expires_in");
                            if(ex == null){
                                ex = map.get("expires");                            
                            }
                            if(ex != null){
                                expires = ex.toString();
                            }
                        }else{
                            token = t.substring(t.indexOf("=") + 1, t.indexOf("&"));
                            int off = t.indexOf("expires=");
                            int start = 8;
                            if(off == -1){
                                off = t.indexOf("expires_in=");
                                start = 11;
                            }
                            if (off > -1) {
                                int end = t.indexOf('&', off);
                                if (end < 0 || end < off) {
                                    end = t.length();
                                }
                                expires = t.substring(off + start, end);
                            }
                        }
                        if (login != null) {
                            login.dispose();
                        }
                    }

                    protected void handleException(Exception err) {
                        if (backToForm != null) {
                            backToForm.showBack();
                        }
                        if (al != null) {
                            al.actionPerformed(new ActionEvent(err));
                        }
                    }

                    protected void postResponse() {
                        if (backToParent && backToForm != null) {
                            backToForm.showBack();
                        }
                        if (al != null) {
                            al.actionPerformed(new ActionEvent(new AccessToken(token, expires)));
                        }
                    }
                };
                req.setUrl(tokenRequestURL);
                req.setPost(true);
                req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                req.addArgument("client_id", clientId);
                req.addArgument("redirect_uri", redirectURI);
                req.addArgument("client_secret", clientSecret);
                req.addArgument("code", (String) params.get("code"));
                req.addArgument("grant_type", "authorization_code");

                NetworkManager.getInstance().addToQueue(req);
            } else if (url.indexOf("error_reason=") > -1) {
                Hashtable table = getParamsFromURL(url);
                String error = (String) table.get("error_reason");
                if (login != null) {
                    login.dispose();
                }
                if (backToForm != null) {
                    backToForm.showBack();
                }
                if (al != null) {
                    al.actionPerformed(new ActionEvent(new IOException(error)));
                }
            } else {
                boolean success = url.indexOf("#") > -1;
                if (success) {
                    String accessToken = url.substring(url.indexOf("#") + 1);
                    if (accessToken.indexOf("&") > 0) {
                        token = accessToken.substring(accessToken.indexOf("=") + 1, accessToken.indexOf("&"));
                    } else {
                        token = accessToken.substring(accessToken.indexOf("=") + 1);
                    }
                    if (login != null) {
                        login.dispose();
                    }
                    if (backToParent && backToForm != null) {
                        backToForm.showBack();
                    }
                    if (al != null) {
                        al.actionPerformed(new ActionEvent(new AccessToken(token, expires)));
                    }
                }
            }
        } else {
            if (frm != null && Display.getInstance().getCurrent() != frm) {
                progress.dispose();
                frm.show();
            }
        }

    }

    private Hashtable getParamsFromURL(String url) {
        int paramsStarts = url.indexOf('?');
        if (paramsStarts > -1) {
            url = url.substring(paramsStarts + 1);
        }
        Hashtable retVal = new Hashtable();

        String[] params = Util.split(url, "&");
        int plen = params.length;
        for (int i = 0; i < plen; i++) {
            if (params[i].indexOf("=") > 0) {
                String[] keyVal = Util.split(params[i], "=");
                retVal.put(keyVal[0], keyVal[1]);
            }
        }
        return retVal;
    }
}
