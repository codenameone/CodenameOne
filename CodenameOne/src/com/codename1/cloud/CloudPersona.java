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
package com.codename1.cloud;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * A persona represents a user of the cloud, this is an optional feature that allows
 * data to be limited to a specific user on the server side.
 *
 * @author Shai Almog
 * @deprecated the cloud storage API is no longer supported, we recommend switching to a solution such as parse4cn1
 */
public class CloudPersona {
    private CloudPersona() {
    }
    
    /**
     * This method returns the currently logged in persona or a blank persona
     * (token would be null)
     * 
     * @return the current persona
     */
    public static CloudPersona getCurrentPersona() {
        return null;
        
    }
    
    /**
     * Returns a unique login token that represents the current user and his password, while this login token shouldn't
     * be user visible (it's a password too!) it can be transfered to a different device to give
     * them both identical user role and joined access.
     * @return a persona UID
     */
    public String getToken() {
        return null;
    }
    
    /**
     * Initializes the persona based on a token, since this method assumes binary transfer of a completed
     * token the token isn't verified in any way and the user is considered logged in.
     * @param token the token
     */
    public static void createFromToken(String token) {
    }
    
    /**
     * Creates an anonymous persona that will be unique in the cloud, NEVER logout an anonymous user!
     * @return false in case login failed e.g. due to bad network connection
     */
    public static boolean createAnonymous() {
        return false;
    }
    
    /**
     * Creates a new user if a user isn't occupying the given login already, 
     * if the user exists performs a login operation.
     * 
     * @param login a user name
     * @param password a password
     * @return true if the login is successful false otherwise
     */
    public static boolean createOrLogin(String login, String password) {
        return false;
    }
    
    /**
     * Logs out the current user, notice that with an anonymous user this will effectively KILL all
     * the data in the cloud!
     */
    public void logout() {
    }
}
