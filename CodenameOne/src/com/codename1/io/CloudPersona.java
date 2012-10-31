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
package com.codename1.io;

import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * A persona represents a user of the cloud, this is an optional feature that allows
 * data to be limited to a specific user on the server side.
 *
 * @author Shai Almog
 */
public class CloudPersona {
    private String persona;
    private static CloudPersona instance;
    
    private CloudPersona() {
    }
    
    /**
     * This method returns the currently logged in persona or a blank persona
     * (token would be null)
     * 
     * @return the current persona
     */
    public static CloudPersona getCurrentPersona() {
        if(instance == null) {
            instance = new CloudPersona();
            instance.persona = Preferences.get("CN1Persona", null);
        }
        return instance;
        
    }
    
    /**
     * Returns a unique login token that represents the current user and his password, while this login token shouldn't
     * be user visible (it's a password too!) it can be transfered to a different device to give
     * them both identical user role and joined access.
     * @return a persona UID
     */
    public String getToken() {
        return persona;
    }
    
    /**
     * Initializes the persona based on a token, since this method assumes binary transfer of a completed
     * token the token isn't verified in any way and the user is considered logged in.
     * @param token the token
     */
    public static void createFromToken(String token) {
        if(instance == null) {
            instance = new CloudPersona();
        } 
        instance.persona = token;
        Preferences.set("CN1Persona", token);
    }
    
    /**
     * Creates an anonymous persona that will be unique in the cloud, NEVER logout an anonymous user!
     * @return false in case login failed e.g. due to bad network connection
     */
    public static boolean createAnonymous() {
        if(instance == null) {
            getCurrentPersona();
        }
        ConnectionRequest login = new ConnectionRequest();
        login.setPost(true);
        login.setUrl(CloudStorage.SERVER_URL + "/objStoreUser");
        login.addArgument("pk", Display.getInstance().getProperty("package_name", null));
        login.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));
        NetworkManager.getInstance().addToQueueAndWait(login);
        if(login.getResposeCode() != 200) {
            return false;
        }
        
        ByteArrayInputStream bi = new ByteArrayInputStream(login.getResponseData());
        DataInputStream di = new DataInputStream(bi);
        
        if(instance == null) {
            instance = new CloudPersona();
        } 
        try {
            instance.persona = di.readUTF();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Preferences.set("CN1Persona", instance.persona);
        Preferences.set("CN1PersonaAnonymous", true);
        
        Util.cleanup(di);
        
        return true;
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
        if(instance == null) {
            getCurrentPersona();
            if(instance.persona != null) {
                return true;
            }
        }
        ConnectionRequest loginRequest = new ConnectionRequest();
        loginRequest.setPost(true);
        loginRequest.setUrl(CloudStorage.SERVER_URL + "/objStoreUser");
        loginRequest.addArgument("l", login);
        loginRequest.addArgument("p", password);
        loginRequest.addArgument("pk", Display.getInstance().getProperty("package_name", null));
        loginRequest.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));
        NetworkManager.getInstance().addToQueueAndWait(loginRequest);
        if(loginRequest.getResposeCode() != 200) {
            return false;
        }
        
        ByteArrayInputStream bi = new ByteArrayInputStream(loginRequest.getResponseData());
        DataInputStream di = new DataInputStream(bi);
        
        try {
            if(di.readBoolean()) {
                if(instance == null) {
                    instance = new CloudPersona();
                } 
                instance.persona = di.readUTF();
                Preferences.set("CN1Persona", instance.persona);
                Util.cleanup(di);
            } else {
                Util.cleanup(di);
                return false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return true;
    }
    
    /**
     * Logs out the current user, notice that with an anonymous user this will effectively KILL all
     * the data in the cloud!
     */
    public void logout() {
        if(Preferences.get("CN1PersonaAnonymous", false)) {
            throw new RuntimeException("Anonymous personas can't be logged out!");
        }
        Preferences.delete("CN1Persona");
    }
}
