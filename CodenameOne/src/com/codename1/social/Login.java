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

import com.codename1.io.AccessToken;
import com.codename1.io.Oauth2;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/**
 * The Login abstract base class is used to simplify Oauth2 authentications 
 * services.
 * Services can override the default Oauth2 web login and offers the native login
 * experience.
 * 
 * @author Chen
 */
public abstract class Login {

    LoginCallback callback;
    
    private AccessToken token;

    String oauth2URL;
    String clientId;
    String redirectURI;
    String clientSecret;
    String scope;
    
    /**
     * Logs in the user.
     * If the service has a native login it will try to use that, otherwise an
     * Oauth2 web login will be used.
     */
    public void doLogin(){
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
                        AccessToken t = (AccessToken)evt.getSource();
                        setAccessToken(t);
                        if(callback != null){
                            callback.loginSuccessful();
                        }
                        return;
                    }
                    if (evt.getSource() instanceof String) {
                        String t = (String)evt.getSource();
                        setAccessToken(new AccessToken(t, null));
                        if(callback != null){
                            callback.loginSuccessful();
                        }
                        return;
                    }
                    if (evt.getSource() instanceof Exception) {
                        if(callback != null){
                            Exception e = (Exception) evt.getSource();
                            callback.loginFailed(e.getMessage());
                        }
                        
                    }

                }

            });

        }
    
    }
    
    /**
     * Logs out the current user
     */
    public void doLogout(){
        if (isNativeLoginSupported()) {
            nativeLogout();
        } else {
            setAccessToken(null);
        }    
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
    public boolean nativeIsLoggedIn(){
        throw new RuntimeException("Native isLoggedIn not implemented");    
    }
    
    /**
     * Logs in the current user natively.
     * Subclasses that uses a native sdk to login/logout should override this 
     * method.
     */
    public void nativelogin(){
        throw new RuntimeException("Native login not implemented");
    }
    
    /**
     * Logs out the current user natively.
     * Subclasses that uses a native sdk to login/logout should override this 
     * method.
     */
    public void nativeLogout(){
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
        return token;
    }

    /**
     * Sets the Login access token
     */ 
    public void setAccessToken(AccessToken token) {
        this.token = token;
    }
    
    /**
     * Sets the login callback that will receive event callback notification
     * from the API
     *
     * @param lc the login callback or null to remove the existing login
     * callback
     */
    public void setCallback(LoginCallback lc) {
        callback = lc;
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
    
    
}
