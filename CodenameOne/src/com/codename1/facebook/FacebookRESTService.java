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
package com.codename1.facebook;

import com.codename1.io.BufferedInputStream;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParseCallback;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkEvent;
import com.codename1.io.Util;
import com.codename1.io.services.ImageDownloadService;
import com.codename1.ui.list.DefaultListModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Invokes the Facebook REST API documented here http://developers.facebook.com/docs/guides/mobile/
 *
 * @author Shai Almog
 */
class FacebookRESTService extends ConnectionRequest implements JSONParseCallback {

    private String token;

    private Hashtable entry = new Hashtable();
    private Hashtable currentData = entry;
    private Vector stack = new Vector();
    private String connectionType = "";
    public static String PICTURE = "picture";
    public static String FRIENDS = "friends";
    public static String TAGGED = "tagged";
    public static String ACTIVITIES = "activities";
    public static String INTERESTS = "interests";
    public static String LIKES = "likes";
    public static String ALBUMS = "albums";
    public static String PHOTOS = "photos";
    public static String COMMENTS = "comments";
    public static String HOME = "home";
    public static String FEED = "feed";
    public static String POSTS = "posts";
    public static String INBOX = "inbox";
    public static String MESSAGES = "messages";
    public static String EVENTS = "events";
    public static String NOTES = "notes";      
    
    private DefaultListModel responseDestination;
    private int responseOffset = -1;
    private String imageKey;

    public static final String GRAPH_URL = "https://graph.facebook.com/";
    
    public FacebookRESTService(boolean post, String token) {
        this.token = token;
        setPost(post);
    }

    public FacebookRESTService(String token, String id, String connectionType, boolean post) {
        this.token = token;
        setPost(post);
        String query = id;
        if (connectionType.length() > 0) {
            query += "/" + connectionType;
        }
        this.connectionType = connectionType;
        addArgumentNoEncoding("access_token", token);
        //addArgument("access_token", token);
        setQuery(query);
    }

    public FacebookRESTService(String token, String url, boolean post) {
        this.token = token;
        setPost(post);
        addArgumentNoEncoding("access_token", token);
        //addArgument("access_token", token);
        //setQuery(query);
        setUrl(url);
    }

    protected void setQuery(String query) {
        String url = GRAPH_URL + query;
        if(FaceBookAccess.getApiVersion().length() > 0){
            url = GRAPH_URL + FaceBookAccess.getApiVersion() + "/" + query;
        }
        setUrl(url);
    }

    public String requestURL() {
        return createRequestURL();
    }

    protected void readResponse(InputStream input) throws IOException {
        //BufferedInputStream i = new BufferedInputStream(new InputStreamReader(input, ));
        BufferedInputStream i;
        if(input instanceof BufferedInputStream){
            i = (BufferedInputStream) input;
        }else{
            i = new BufferedInputStream(input);
        }
        i.setYield(-1);
        InputStreamReader reader = new InputStreamReader(i, "UTF-8");
        JSONParser.parse(reader, this);
        Util.cleanup(reader);
        if(stack.size() > 0){
            fireResponseListener(new NetworkEvent(this, stack.elementAt(0)));
        }
    }

    protected void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    protected String getConnectionType() {
        return connectionType;
    }
    private String root;

    public void startBlock(String block) {
        if(block.equals("paging")){
            return;
        }
        Object node;
        if (stack.size() == 0) {
            if (root == null) {
                root = "entry";
            }
            node = new Vector() {

                public synchronized void addElement(Object obj) {
                    if (responseDestination != null) {
                        if(responseOffset == -1) {
                            responseDestination.addItem(obj);
                        } else {
                            Hashtable h = (Hashtable)responseDestination.getItemAt(responseOffset);
                            h.putAll((Hashtable)obj);
                            responseDestination.setItem(responseOffset, h);
                            responseOffset++;
                        }
                    } else {
                        super.addElement(obj);
                    }
                }
            };
            stack.addElement(node);
            if (connectionType.length() > 0 || getUrl().indexOf("search") > 0) {
                return;
            }
        } else {
            node = stack.elementAt(stack.size() - 1);
        }

        Hashtable data = new Hashtable();
        if (node instanceof Hashtable) {
            ((Hashtable) node).put(block, data);
        } else {
            ((Vector) node).addElement(data);
        }
        stack.addElement(data);
        currentData = data;
    }

    public void startArray(String block) {
        if(block.equals("paging")){
            return;
        }
        Vector items = new Vector();
        Object node;
        if (stack.size() == 1) {
            return;
        } else {
            if (stack.size() == 0) {
                node = new Vector() {

                    public synchronized void addElement(Object obj) {
                        if (responseDestination != null) {
                            responseDestination.addItem(obj);
                        } else {
                            super.addElement(obj);
                        }
                    }
                };
                stack.addElement(node);
            }
            node = stack.elementAt(stack.size() - 1);
        }

        if (node instanceof Hashtable) {
            ((Hashtable) node).put(block, items);
        } else {
            ((Vector) node).addElement(items);
        }
        stack.addElement(items);
    }

    public void endArray(String block) {
        if(block.equals("paging")){
            return;
        }
        if(stack.size() > 1){
            stack.removeElementAt(stack.size() - 1);
            Object node = stack.elementAt(stack.size() - 1);
            if (node instanceof Hashtable) {
                currentData = (Hashtable) node;
            }
        }
    }
    
    public void endBlock(String block) {
        if(block.equals("paging")){
            return;
        }
        if (stack.size() > 1) {
            stack.removeElement(currentData);
            Object node = stack.elementAt(stack.size() - 1);
            if (node instanceof Hashtable) {
                currentData = (Hashtable) node;
            }
        }

    }

    public void stringToken(String tok) {
    }

    public void numericToken(double tok) {
    }

    public void keyValue(String key, String value) {
        //make sure value is not null to prevent NPE
        if (key != null && value == null) {
            value = "";
        }
        getCurrent().put(key, value);
    }


    protected Hashtable getCurrent() {
        if (currentData == null) {
            currentData = new Hashtable();
        }
        return currentData;
    }

    public void setResponseDestination(DefaultListModel des) {
        responseDestination = des;
    }    

    public boolean isAlive() {
        return !isKilled();
    }

    protected int getYield() {
        return -1;
    }

    /**
     * @return the responseOffset
     */
    public int getResponseOffset() {
        return responseOffset;
    }

    /**
     * @param responseOffset the responseOffset to set
     */
    public void setResponseOffset(int responseOffset) {
        this.responseOffset = responseOffset;
    }

    /**
     * @return the imageKey
     */
    public String getImageKey() {
        return imageKey;
    }

    /**
     * @param imageKey the imageKey to set
     */
    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    /**
     * @inheritDoc
     */
    public void longToken(long tok) {
    }

}
