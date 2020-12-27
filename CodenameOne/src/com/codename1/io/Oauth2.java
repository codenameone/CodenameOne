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
import com.codename1.ui.BrowserWindow;
import com.codename1.ui.CN;
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
import com.codename1.util.AsyncResource;
import com.codename1.util.regex.StringReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
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
    private boolean useRedirectForWeb = false;
    private boolean useBrowserWindow = "true".equals(CN.getProperty("oauth2.useBrowserWindow", "true"));
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
    private String refreshToken;
    private String identityToken;
    
    private String clientId;
    private String redirectURI;
    private String scope;
    private String clientSecret;
    private String oauth2URL;
    private String tokenRequestURL;
    private Hashtable additionalParams;
    private Dialog login;
    private static boolean backToParent = true;
    
    private void serializeAuth() {
        Map params = new HashMap();
        params.put("token", token);
        params.put("refreshToken", refreshToken);
        params.put("identityToken", identityToken);
        params.put("clientId", clientId);
        params.put("redirectURI", redirectURI);
        params.put("scope", scope);
        params.put("clientSecret", clientSecret);
        params.put("oauth2URL", oauth2URL);
        params.put("tokenRequestURL", tokenRequestURL);
        params.put("additionalParams", additionalParams);
        params.put("backToParent", backToParent);
        Storage s = Storage.getInstance();
        s.writeObject("__oauth2Params", params);
    }
     
     
    public static Oauth2 fetchSerializedOauth2Request() {
        Storage s = Storage.getInstance();
        Map m = (Map)s.readObject("__oauth2Params");
        if (m == null) {
            return null;
        }
        Oauth2 out = new Oauth2((String)m.get("oauth2URL"), (String)m.get("clientId"), (String)m.get("redirectURI"));
        out.token = (String)m.get("token");
        out.refreshToken = (String)m.get("refreshToken");
        out.identityToken = (String)m.get("identityToken");
        out.scope = (String)m.get("scope");
        out.clientSecret = (String)m.get("clientSecrete");
        out.tokenRequestURL = (String)m.get("tokenRequestURL");
        if (m.get("additionalParams") != null) {
            out.additionalParams = new Hashtable();
            out.additionalParams.putAll((Map)m.get("additionalParams"));
        }
        out.backToParent = (Boolean)m.get("backToParent");
        s.deleteStorageFile("__oauth2Params");
        return out;
        
    }
    

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
     * Set this OAuth2 object to use a {@link BrowserWindow} for the login process.  You can set the global default via the "oauth2.useBrowserWindow"
     * display property with either a "true" or "false" value.
     * 
     * <p>When this property is set, the login prompt will be displayed in a separate Window containing a web browser on the desktop.  Platforms that 
     * don't have windows (e.g. iOS/Android) will fall back to a separate Form with a webview).</p>
     * 
     * @param useBrowserWindow True to use a browser window for the login process.
     * @since 7.0
     */
    public void setUseBrowserWindow(boolean useBrowserWindow) {
        this.useBrowserWindow = useBrowserWindow;
    }
    
    /**
     * Checks if this component will use an external web browser window for the login process.
     * @return True if this component will use an external web browser window.
     * @since 7.0
     */
    public boolean isUseBrowserWindow() {
        return useBrowserWindow;
    }
    
    /**
     * Sets thisOAuth2 object to use a redirect for login instead of an iframe when running on the Web (via the Javascript port).  Some
     * Oauth providers won't work inside an iframe.
     * 
     * <p>Using this option will cause the browser to navigate away from the app to go to the login page.  The Oauth
     * login will redirect back to the app after login is complete.</p>
     * 
     * <p><strong>Warning</strong>: If the user has unsaved changes in the app, navigating away from the app may cause them to lose their changes.  You should provide
     * a warning, or confirmation prompt for the user in such cases.  The usual onbeforeunload handler is disabled when using this action so the user
     * won't receive any warnings other than what you explicitly prompt.</p>
     * @param redirect Set to true to use a redirect for Oauth login instead of an iframe when running on the web.
     * @since 7.0
     * @see #isUseRedirectForWeb() 
     * @see #handleRedirect(com.codename1.ui.events.ActionListener) 
     */
    public void setUseRedirectForWeb(boolean redirect) {
        this.useRedirectForWeb = redirect;
    }
    
    /**
     * Checks wither this Oauth component is configured to use a redirect for Oauth login when running on the web.
     * @return True if this component will use a redirect for Oauth login.
     * @since 7.0
     * @see #setUseRedirectForWeb(boolean) 
     * @see #handleRedirect(com.codename1.ui.events.ActionListener) 
     */
    public boolean isUseRedirectForWeb() {
        return useRedirectForWeb;
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
     * When using the {@link #setUseRedirectForWeb(boolean) } option you should call this method at the beginning of your app's 
     * {@code start()} method.  If the app was loaded as a result of redirecting from an Oauth login, then this method will handle the login
     * and will call the callback method on complete.
     * @param callback a listener that will receive at its source either a token for
     * the service or an exception in case of a failure
     * @return True the redirect was handled.  False if it was not handled.  If this returns {@literal true}, then you should just return from the start() method, and instead
     * handle control flow in your callback.
     * @since 7.0
     * @see #setUseRedirectForWeb(boolean) 
     * @see #isUseRedirectForWeb() 
     */
    public static boolean handleRedirect(ActionListener callback) {
        Oauth2 request = fetchSerializedOauth2Request();
        if (request == null) {
            return false;
        }
        String href = CN.getProperty("browser.window.location.href", null);
        request.handleURL(href, null, callback, null, null, null);
        return true;
    }
    
    /**
     * This method shows an authentication for login form
     *
     * @param al a listener that will receive at its source either a token for
     * the service or an exception in case of a failure
     */
    public void showAuthentication(final ActionListener al) {
        
        
        if ("HTML5".equals(CN.getPlatformName()) && useRedirectForWeb) {
            String href = CN.getProperty("browser.window.location.href", null);
            redirectURI = href;
            serializeAuth();
            CN.execute("javascript:(function(){window.onbeforeunload=function(){}; window.location.href='"+buildURL()+"';})();");
            return;
        }
        
        if (useBrowserWindow) {
            final BrowserWindow win = new BrowserWindow(buildURL());
            win.setTitle("Login");
            win.addLoadListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    String url = (String)evt.getSource();
                    if (url.startsWith(redirectURI)) {
                        win.close();
                        handleURL((String)evt.getSource(), null, al, null, null, null);
                    }    
                }
            });

            win.show(); 
            return;
            
        }
        
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
            if (authenticationForm.getToolbar() != null){
                authenticationForm.getToolbar().addCommandToLeftBar(cancel);
            } else {
                authenticationForm.addCommand(cancel);
            }
            authenticationForm.setBackCommand(cancel);
        }
        authenticationForm.setLayout(new BorderLayout());
        authenticationForm.addComponent(BorderLayout.CENTER, createLoginComponent(al, authenticationForm, old, progress));
        authenticationForm.show();
    }

    private String buildURL() {
        String URL = oauth2URL + "?client_id=" + Util.encodeUrl(clientId)
                + "&redirect_uri=" + Util.encodeUrl(redirectURI);
        if (scope != null) {
            URL += "&scope=" + Util.encodeUrl(scope);
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
                URL += "&" + Util.encodeUrl(key) + "=" + Util.encodeUrl(val);
            }
        }
        return URL;
    }
    
    private Component createLoginComponent(final ActionListener al, final Form frm, final Form backToForm, final Dialog progress) {

        String URL = buildURL();

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
    
    /**
     * Processes token request responses that are formatted as a JSON object.  May be overridden
     * by subclasses, but subclass implementations should call super.handleTokenRequestResponse()
     * so that the default implementation can parse out the token, expires, and refreshToken fields.
     * @param map Parsed JSON object of response.
     * @since 7.0
     */
    protected void handleTokenRequestResponse(Map map) {
        token = (String) map.get("access_token");
        Object ex = map.get("expires_in");
        if(ex == null){
            ex = map.get("expires");
        }
        if(ex != null){
            expires = ex.toString();
        }
        refreshToken = (String)map.get("refresh_token");
        identityToken = (String)map.get("id_token");
        
    }
    
    /**
     * Processes token request responses that are formatted as HTTP query strings.  May be
     * overridden by subclass, but subclass implementations should call super.handleTokenRequestResponse()
     * so that the default implementation can parse out the token, expires, and refreshToken fields.
     * @param t The query string.
     * @since 7.0
     */
    protected void handleTokenRequestResponse(String t) {
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
        off = t.indexOf("refresh_token=");
        refreshToken = null;
        start = "refresh_token=".length();
        if (off > -1) {
            int end = t.indexOf('&', off);
            if (end < 0 || end < off) {
                end = t.length();
            }
            refreshToken = t.substring(off +  start, end);
        }
    }

    /**
     * Method that can be overridden by subclasses to intercept parameters extracted from
     * the redirect URL when the login flow reaches the redirect URL.  This will give subclasses
     * an opportunity to parse out special information that the OAuth2 service provides in the callback.
     * @param params Parsed query parameters passed to the redirect URL.
     */
    protected void handleRedirectURLParams(Map params) {
        
    }
    
    public class RefreshTokenRequest extends AsyncResource<AccessToken> {
        
    }
    
    public RefreshTokenRequest refreshToken(String refreshToken) {
        final RefreshTokenRequest out = new RefreshTokenRequest();
        refreshToken(refreshToken, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (out.isDone()) {
                    return;
                }
                if (evt.getSource() instanceof Throwable) {
                    out.error(new AsyncResource.AsyncExecutionException((Throwable)evt.getSource()));
                } else {
                    out.complete((AccessToken)evt.getSource());
                }
            }
        });
        return out;
    }
    
    private void refreshToken(String refreshToken, ActionListener al) {
        handleURL(redirectURI + "?code="+Util.encodeUrl(refreshToken)+"&cn1_refresh_token=1", null, al, null, null, null);
    }
    
    private void handleURL(String url, WebBrowser web, final ActionListener al, final Form frm, final Form backToForm, final Dialog progress) {
        if ((url.startsWith(redirectURI))) {
            if (progress != null && Display.getInstance().getCurrent() == progress) {
                progress.dispose();
            }

            if (web != null) {
                web.stop();
            }

            //remove the browser component.
            if (login != null) {
                login.removeAll();
                login.revalidate();
            }

            if (url.indexOf("code=") > -1) {
                Hashtable params = getParamsFromURL(url);
                handleRedirectURLParams(params);
                class TokenRequest extends ConnectionRequest {
                    boolean callbackCalled;
                    protected void readResponse(InputStream input) throws IOException {
                        byte[] tok = Util.readInputStream(input);
                        String t = new String(tok);
                        boolean expiresRelative = true;
                        if(t.startsWith("{")){
                            JSONParser p = new JSONParser();
                            Map map = p.parseJSON(new StringReader(t));
                            handleTokenRequestResponse(map);
                        }else{
                            handleTokenRequestResponse(t);
                        }
                        if (login != null) {
                            login.dispose();
                        }
                    }

                    protected void handleException(Exception err) {
                        if (backToForm != null && !callbackCalled) {
                            backToForm.showBack();
                        }
                        if (al != null) {
                            if (!callbackCalled) {
                                callbackCalled = true;
                                al.actionPerformed(new ActionEvent(err,ActionEvent.Type.Exception));
                            }
                        }
                    }

                    protected void postResponse() {
                        
                        if (backToParent && backToForm != null && !callbackCalled) {
                            backToForm.showBack();
                        }
                        if (al != null) {
                            if (!callbackCalled) {
                                callbackCalled = true;
                                if (getResponseCode() >= 200 && getResponseCode() < 300) {
                                    al.actionPerformed(new ActionEvent(new AccessToken(token, expires, refreshToken, identityToken),ActionEvent.Type.Response));
                                } else {
                                    al.actionPerformed(new ActionEvent(new IOException(getResponseErrorMessage()),ActionEvent.Type.Exception));
                                }
                            }
                        }
                    }
                };
                final TokenRequest req = new TokenRequest();
                req.setReadResponseForErrors(true);
                
                req.setUrl(tokenRequestURL);
                req.setPost(true);
                req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                req.addArgument("client_id", clientId);
                req.addArgument("redirect_uri", redirectURI);
                req.addArgument("client_secret", clientSecret);
                if (params.containsKey("cn1_refresh_token")) {
                    req.addArgument("grant_type", "refresh_token");
                    req.addArgument("refresh_token", (String)params.get("code"));
                } else {
                    req.addArgument("code", (String) params.get("code"));
                    req.addArgument("grant_type", "authorization_code");
                }

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
                    al.actionPerformed(new ActionEvent(new IOException(error),ActionEvent.Type.Exception));
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
                        al.actionPerformed(new ActionEvent(new AccessToken(token, expires),ActionEvent.Type.Response));
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
