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

/**
 * Invokes the native bundled facebook SDK to login/logout of facebook, notice that
 * in order for this to work server build arguments must indicate that you are using
 * the facebook sdk! To accomplish this just define: facebook.appId=YourAppId in your build
 * arguments. In order to obtain the app ID you need to create a native Android/iOS application
 * and generate the right app id.
 *
 * @author Shai Almog
 */
public class FacebookConnect {
    LoginCallback callback;
    private static FacebookConnect instance;
    static Class implClass;
    
    FacebookConnect() {}
    
    public static FacebookConnect getInstance() {
        if(instance == null) {
            try {
                instance = (FacebookConnect)implClass.newInstance();
            } catch(Throwable t) {
                instance = new FacebookConnect();
            }
        }
        return instance;
    }
    
    /**
     * Sets the login callback that will receive event callback notification from
     * the API
     * @param lc the login callback or null to remove the existing login callback
     */
    public void setCallback(LoginCallback lc) {
        callback = lc;
    }
    
    /**
     * Indicates whether the native platform supports native facebook login
     * @return true if supported
     */
    public boolean isFacebookSDKSupported() {
        return false;
    }
    
    /**
     * Logs into facebook, notice that this call might suspend the application which might 
     * trigger repeated invocations of stop()/start() etc. This is due to the facebook SDK
     * spawning a separate process to perform the login then returning to the application. 
     * Once logged in the facebook credentials will be available.
     */
    public void login() {
        throw new RuntimeException("Native facebook unsupported");
    }
    
    /**
     * Indicates if the user is currently logged in
     * @return true if logged in
     */
    public boolean isLoggedIn() {
        throw new RuntimeException("Native facebook unsupported");
    }
    
    /**
     * The facebook token that can be used to access facebook functionality
     * @return the token
     */
    public String getToken() {
        throw new RuntimeException("Native facebook unsupported");
    }
    
    /**
     * Logs out the current user from facebook
     */
    public void logout() {
        throw new RuntimeException("Native facebook unsupported");
    }
    
    /**
     * Asks for publish permissions, this call might suspend the application which might 
     * trigger repeated invocations of stop()/start().
     */ 
    public void askPublishPermissions(LoginCallback lc){
        throw new RuntimeException("Native facebook unsupported");
    }
    
    /**
     * Returns true if the current session already has publish permissions
     * @return 
     */
    public boolean hasPublishPermissions(){
        throw new RuntimeException("Native facebook unsupported");    
    }
    
}
