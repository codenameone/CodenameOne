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

package com.codename1.io.services;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Label;
import com.codename1.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Calls to the Twitter REST API can be performed via this class although currently
 * support for authentication isn't implemented due to the transition to oAuth instead
 * of basic authentication.
 *
 * @author Shai Almog
 */
public class TwitterRESTService extends ConnectionRequest {
    private static String authToken;
    private Hashtable parseTree;
    
    public static final String METHOD_USER_TIMELINE = "statuses/user_timeline";
    public static final String METHOD_TWEETS = "search/tweets";
    
    /**
     * The constructor accepts the method to invoke
     *
     * @param method the api method to invoke e.g. "statuses/public_timeline"
     */
    public TwitterRESTService(String method) {
        this(method, "1.1", false);
    }

    /**
     * The constructor accepts the method to invoke
     *
     * @param method the api method to invoke e.g. "statuses/public_timeline"
     * @param post true for post requests and false for get request
     */
    public TwitterRESTService(String method, boolean post) {
        this(method, "1.1", post);
    }

    /**
     * Logs in to twitter as an application
     * 
     * @param consumerKey the key to login with
     * @param consumerSecret the secret to to login with
     * @return the authorization token
     */
    public static String initToken(String consumerKey, String consumerSecret) {
        ConnectionRequest auth = new ConnectionRequest() {
            protected void readResponse(InputStream input) throws IOException  {
                JSONParser p = new JSONParser();
                Hashtable h = p.parse(new InputStreamReader(input));
                authToken = (String)h.get("access_token");
                if(authToken == null) {
                    return;
                }
            }
        };
        auth.setPost(true);
        auth.setUrl("https://api.twitter.com/oauth2/token");
        
        // YOU MUST CHANGE THIS IF YOU BUILD YOUR OWN APP
        String encoded = Base64.encodeNoNewline((consumerKey + ":" + consumerSecret).getBytes());
        auth.addRequestHeader("Authorization", "Basic " + encoded);
        auth.setContentType("application/x-www-form-urlencoded;charset=UTF-8");
        auth.addArgument("grant_type", "client_credentials");
        NetworkManager.getInstance().addToQueueAndWait(auth);
        return authToken;
    }
    
    /**
     * For every request twitter now needs an authorization token
     * @param token the token
     */
    public static void setToken(String token) {
        authToken = token;
    }
    
    /**
     * The constructor accepts the method to invoke
     *
     * @param method the api method to invoke e.g. "statuses/public_timeline"
     * @param version the API version to send e.g. "1"
     * @param post true for post requests and false for get request
     */
    public TwitterRESTService(String method, String version, boolean post) {
        setPost(post);
        setUrl("https://api.twitter.com/" + version + "/" + method + ".json");
        addRequestHeader("Authorization", "Bearer " + authToken);
        setContentType("application/json");
        addRequestHeader("Accept", "application/json");
    }


    /**
     * @inheritDoc
     */
    protected void readResponse(InputStream input) throws IOException  {
        InputStreamReader i = new InputStreamReader(input, "UTF-8");
        parseTree = new JSONParser().parse(i);
        fireResponseListener(new NetworkEvent(this, parseTree));
    }
    
    /**
     * Returns the full Hashtable parse tree read from the server
     * @return the parse tree
     */
    public Hashtable<String, Object> getParseTree() {
        return parseTree;
    }
    
    /**
     * Returns the number of statuses within the response
     * @return the number of statuses
     */
    public int getStatusesCount() {
        Vector v = (Vector)parseTree.get("statuses");
        if(v == null) {
            return 0;
        }
        return v.size();
    }

    /**
     * Returns the status at the given offset
     * @param offset  the offset for the status
     * @return the status hashtable
     */
    public Hashtable<String, Object> getStatus(int offset) {
        Vector v = (Vector)parseTree.get("statuses");
        return (Hashtable<String, Object>)v.get(offset);
    }
    
    /**
     * Gets the id string of the first entry which is important if we want to set the id
     * to start with in the next request
     * @return the id of the first entry
     */
    public String getIdStr() {
        if(getStatusesCount() > 0) {
            return (String)getStatus(0).get("id_str");
        }
        return null;
    }
}
