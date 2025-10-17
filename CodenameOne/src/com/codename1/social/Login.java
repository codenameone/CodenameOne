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

import com.codename1.compat.java.util.Objects;
import com.codename1.io.AccessToken;
import com.codename1.io.Log;
import com.codename1.io.Oauth2;
import com.codename1.io.Oauth2.RefreshTokenRequest;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The Login abstract base class is used to simplify Oauth2 authentications
 * services.
 * Services can override the default Oauth2 web login and offers the native login
 * experience.
 *
 * @author Chen
 */
public abstract class Login {


    LoginCallback callback = new LoginCallBackProxy();
    String oauth2URL;
    String clientId;
    String redirectURI;
    String clientSecret;
    String scope;
    private LoginCallback loginCallback;
    private ArrayList<LoginCallback> loginCallbacksSingleUse = new ArrayList<LoginCallback>();
    private boolean callbackEnabled = true;
    private String validateErr = null;
    private AccessToken token;
    // A flag that is used in the javascript port to optionally use a redirect instead of
    // a popup for the signin prompt.  Redirect is better for UX, but it also means that
    // you leave the app and restart it after login, which may be a non-starter.
    // So this needs to be explicitly enabled by the app.
    private boolean preferRedirectPrompt = false;

    /**
     * Adds the given scopes to the OAuth2 login request.
     *
     * @param scopes Scopes to add.
     * @return Self for chaining.
     * @see #setScope(java.lang.String)
     * @since 7.0
     */
    public Login addScopes(String... scopes) {
        ArrayList<String> existing = new ArrayList<String>();
        if (scope != null) {
            for (String str : Util.split(scope, " ")) {
                str = str.trim();
                if (str.length() == 0) {
                    continue;
                }
                if (!existing.contains(str)) {
                    existing.add(str);
                }
            }
        }
        for (String scope : scopes) {
            if (scope.trim().length() == 0) {
                continue;
            }
            if (!existing.contains(scope)) {
                existing.add(scope);
            }
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String scope : existing) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(scope);
        }
        scope = sb.toString();
        return this;
    }

    /**
     * Connects to the login service asynchronously, automatically logging in
     * if not yet logged in.
     *
     * @return AsyncResource that can be monitored for completion.
     * @since 7.0
     */
    public AsyncResource<Login> connect() {
        final AsyncResource<Login> out = new AsyncResource<Login>();
        if (isUserLoggedIn()) {
            out.complete(this);
        } else {
            doLogin(new LoginCallback() {
                @Override
                public void loginSuccessful() {
                    out.complete(Login.this);
                }

                @Override
                public void loginFailed(String errorMessage) {
                    out.error(new RuntimeException(errorMessage));
                }
            });
        }
        return out;
    }

    /**
     * Initiates login using the given single-use callback.
     *
     * @param callback Callback to be called if login succeeds or fails.
     * @since 7.0
     */
    public void doLogin(LoginCallback callback) {
        if (callback != null) {
            loginCallbacksSingleUse.add(callback);
        }
        doLogin();
    }

    /**
     * Logs in the user.
     * If the service has a native login it will try to use that, otherwise an
     * Oauth2 web login will be used.
     */
    public void doLogin() {
        if (isNativeLoginSupported()) {
            nativelogin();
        } else {

            if (oauth2URL == null) {
                System.out.println("No oauth2URL found Use setOauth2URL");
                return;
            }
            if (clientId == null) {
                System.out.println("No ClientId found Use setClientId");
                return;
            }
            if (redirectURI == null) {
                System.out.println("No redirectURI found Use setRedirectURI");
                return;
            }
            if (clientSecret == null) {
                System.out.println("No clientSecret found Use setClientSecret");
                return;
            }

            Oauth2 auth = createOauth2();
            auth.showAuthentication(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    //success
                    if (evt.getSource() instanceof AccessToken) {
                        AccessToken t = (AccessToken) evt.getSource();
                        setAccessToken(t);
                        if (callback != null) {
                            callback.loginSuccessful();
                        }
                        return;
                    }
                    if (evt.getSource() instanceof String) {
                        String t = (String) evt.getSource();
                        setAccessToken(new AccessToken(t, (String) null));
                        if (callback != null) {
                            callback.loginSuccessful();
                        }
                        return;
                    }
                    if (evt.getSource() instanceof Exception) {
                        if (callback != null) {
                            Exception e = (Exception) evt.getSource();
                            Log.e(e);
                            System.out.println("Calling loginFailed of callback " + callback);
                            String msg = e.getMessage();
                            if (msg == null) {
                                msg = "Unknown error";
                            }
                            callback.loginFailed(msg);
                        }

                    }

                }

            });

        }

    }

    /**
     * Logs out the current user
     */
    public void doLogout() {
        if (isNativeLoginSupported()) {
            nativeLogout();
        } else {
            setAccessToken(null);
        }
        Preferences.delete(Login.this.getClass().getName() + "Token");
    }

    /**
     * Indicates if the user is currently logged in
     *
     * @return true if logged in
     */
    public boolean isUserLoggedIn() {
        if (isNativeLoginSupported()) {
            return nativeIsLoggedIn();
        } else {
            AccessToken accessTok = getAccessToken();
            if (accessTok != null && accessTok.getExpiryDate() != null && accessTok.isExpired()) {
                return false;
            }
            return token != null;
        }
    }

    /**
     * Indicates if the user is currently logged in.
     * Subclasses that uses a native sdk to login/logout should override this
     * method.
     *
     * @return true if logged in
     */
    public boolean nativeIsLoggedIn() {
        throw new RuntimeException("Native isLoggedIn not implemented");
    }

    /**
     * Logs in the current user natively.
     * Subclasses that uses a native sdk to login/logout should override this
     * method.
     */
    public void nativelogin() {
        throw new RuntimeException("Native login not implemented");
    }

    /**
     * Logs out the current user natively.
     * Subclasses that uses a native sdk to login/logout should override this
     * method.
     */
    public void nativeLogout() {
        throw new RuntimeException("Native logout not implemented");
    }


    /**
     * Returns true if this service supports native login.
     * If implementation returns true here, the nativelogin, nativelogout,
     * nativeIsLoggedIn should be implemented
     *
     * @return true if the service supports native login
     */
    public abstract boolean isNativeLoginSupported();

    /**
     * The AccessToken of this service
     *
     * @return the token
     */
    public AccessToken getAccessToken() {
        if (token == null) {
            Util.register(new AccessToken());
            token = (AccessToken) Storage.getInstance().readObject(getClass().getName() + "AccessToken");
        }
        return token;
    }

    /**
     * Sets the Login access token
     */
    public void setAccessToken(AccessToken token) {
        if (!Objects.equals(token, this.token)) {
            this.token = token;
            Storage.getInstance().writeObject(getClass().getName() + "AccessToken", token);
        }
    }

    /**
     * This method tries to validate the last access token if exists, if the
     * last token is not valid anymore it will try to login the user in order to
     * get a fresh token
     * The method blocks until a valid token has been granted
     */
    public void validateToken() throws IOException {

        String token = Preferences.get(Login.this.getClass().getName() + "Token", null);
        if (token == null || !validateToken(token)) {
            AccessToken accessTok = getAccessToken();
            if (accessTok != null && accessTok.getRefreshToken() != null) {
                String refreshTok = accessTok.getRefreshToken();
                RefreshTokenRequest refreshReq = createOauth2().refreshToken(refreshTok);
                try {
                    System.out.println("Attempting to refresh the access token");
                    AccessToken newTok = refreshReq.get(5000);
                    setAccessToken(newTok);
                    Preferences.set(Login.this.getClass().getName() + "Token", getAccessToken().getToken());
                    return;
                } catch (Throwable t) {
                }
            }

            // At this point We'll need to attempt to login again.
            callbackEnabled = false;
            doLogin();
            Display.getInstance().invokeAndBlock(new Runnable() {

                public void run() {
                    while (!callbackEnabled) {
                        Util.sleep(100);
                    }
                }
            });
            if (validateErr != null) {
                throw new IOException(validateErr);
            }
        }
    }

    /**
     * Returns true if the previous granted access token is still valid otherwise
     * false.
     *
     * @param token the access token to check
     * @return true of the token is valid
     */
    protected abstract boolean validateToken(String token);

    /**
     * Sets the login callback that will receive event callback notification
     * from the API
     *
     * @param lc the login callback or null to remove the existing login
     *           callback
     */
    public void setCallback(LoginCallback lc) {
        loginCallback = lc;
    }


    /**
     * The client id (appid) which asks to connect
     *
     * @param clientId
     */
    public void setClientId(String id) {
        clientId = id;
    }

    /**
     * The client secret
     *
     * @param clientSecret
     */
    public void setClientSecret(String secret) {
        clientSecret = secret;
    }

    /**
     * The redirect URI
     *
     * @param redirectURI
     */
    public void setRedirectURI(String URI) {
        redirectURI = URI;
    }

    /**
     * The authentication scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * The oauth2 URL
     */
    public void setOauth2URL(String oauth2URL) {
        this.oauth2URL = oauth2URL;
    }

    /**
     * Creates the oauth2 to be used to login in case no native login is available
     * for this service.
     *
     * @return the Oauth2 to be used to login if no native login available and
     * on the simulator
     */
    protected Oauth2 createOauth2() {
        Oauth2 auth = new Oauth2(oauth2URL, clientId, redirectURI, scope);
        return auth;
    }

    /**
     * A flag used by the javascript port to indicate that the login will use a redirect
     * for the prompt instead of a popup.   On the web, a redirect is usually better UX
     * but it can be problematic since it involves leaving the app, and reloading it
     * after the login.
     *
     * @return the preferRedirectPrompt
     * @since 7.0
     */
    public boolean isPreferRedirectPrompt() {
        return preferRedirectPrompt;
    }

    /**
     * A flag used by the javascript port to indicate that the login will use a redirect
     * for the prompt instead of a popup.   On the web, a redirect is usually better UX
     * but it can be problematic since it involves leaving the app, and reloading it
     * after the login.
     *
     * @param preferRedirectPrompt the preferRedirectPrompt to set
     * @since 7.0
     */
    public void setPreferRedirectPrompt(boolean preferRedirectPrompt) {
        this.preferRedirectPrompt = preferRedirectPrompt;
    }

    class LoginCallBackProxy extends LoginCallback {

        public void loginSuccessful() {
            //store the access token upon login success for future use
            Preferences.set(Login.this.getClass().getName() + "Token", getAccessToken().getToken());

            if (callbackEnabled) {
                if (loginCallback != null) {
                    loginCallback.loginSuccessful();
                }
                while (!loginCallbacksSingleUse.isEmpty()) {
                    final LoginCallback cb = loginCallbacksSingleUse.remove(0);
                    if (!CN.isEdt()) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                cb.loginSuccessful();
                            }
                        });
                    } else {
                        cb.loginSuccessful();
                    }
                }
                return;
            }
            callbackEnabled = true;
            validateErr = null;
        }

        public void loginFailed(final String errorMessage) {
            if (callbackEnabled) {
                if (loginCallback != null) {
                    loginCallback.loginFailed(errorMessage);
                }
                while (!loginCallbacksSingleUse.isEmpty()) {
                    final LoginCallback cb = loginCallbacksSingleUse.remove(0);
                    if (!CN.isEdt()) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                cb.loginFailed(errorMessage);
                            }
                        });
                    } else {
                        cb.loginFailed(errorMessage);
                    }
                }
                return;
            }
            callbackEnabled = true;
            validateErr = errorMessage;
        }
    }
}
