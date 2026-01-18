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
import com.codename1.ui.list.DefaultListModel;
import com.codename1.util.StringUtil;

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

    // PMD Fix (UnusedPrivateField): Removed redundant token storage; the access token is forwarded directly via request arguments.

    public static final String GRAPH_URL = "https://graph.facebook.com/";
    public static String PICTURE = "picture";
    public static String FRIENDS = "friends";
    public static String LIKES = "likes";
    public static String ALBUMS = "albums";
    public static String PHOTOS = "photos";
    public static String COMMENTS = "comments";
    public static String HOME = "home";
    public static String FEED = "feed";
    public static String POSTS = "posts";
    public static String INBOX = "inbox";
    public static String EVENTS = "events";
    public static String NOTES = "notes";
    private final Hashtable entry = new Hashtable();
    private Hashtable currentData = entry;
    private final Vector stack = new Vector();
    private String connectionType = "";
    private DefaultListModel responseDestination;
    private int responseOffset = -1;
    private String root;

    public FacebookRESTService(String token, String id, String connectionType, boolean post) {
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
        setPost(post);
        addArgumentNoEncoding("access_token", token);
        //addArgument("access_token", token);
        //setQuery(query);
        setUrl(url);
    }

    protected void setQuery(String query) {
        int queryParamsIndex = query.indexOf("?");
        if (queryParamsIndex >= 0) {
            String search = query.substring(queryParamsIndex + 1);
            query = query.substring(0, queryParamsIndex);
            java.util.List<String> parts = StringUtil.tokenize(search, "&");
            for (String part : parts) {
                java.util.List<String> kv = StringUtil.tokenize(part, "=");
                addArgumentNoEncoding(kv.get(0), kv.size() > 1 ? kv.get(1) : "");
            }
        }
        String url = GRAPH_URL + query;
        if (FaceBookAccess.getApiVersion().length() > 0) {
            url = GRAPH_URL + FaceBookAccess.getApiVersion() + "/" + query;
        }
        setUrl(url);
    }

    public String requestURL() {
        return createRequestURL();
    }

    @Override
    protected void readResponse(InputStream input) throws IOException {
        //BufferedInputStream i = new BufferedInputStream(new InputStreamReader(input, ));
        BufferedInputStream i;
        if (input instanceof BufferedInputStream) {
            i = (BufferedInputStream) input;
        } else {
            i = new BufferedInputStream(input);
        }
        i.setYield(-1);
        InputStreamReader reader = new InputStreamReader(i, "UTF-8");
        JSONParser.parse(reader, this);
        Util.cleanup(reader);
        if (stack.size() > 0) {
            fireResponseListener(new NetworkEvent(this, stack.elementAt(0)));
        }
    }

    @Override
    public void startBlock(String block) {
        if (block.equals("paging")) {
            return;
        }
        Object node;
        if (stack.size() == 0) {
            if (root == null) {
                root = "entry";
            }
            node = new Vector() {

                @Override
                public synchronized void addElement(Object obj) {
                    if (responseDestination != null) {
                        if (responseOffset == -1) {
                            responseDestination.addItem(obj);
                        } else {
                            Hashtable h = (Hashtable) responseDestination.getItemAt(responseOffset);
                            h.putAll((Hashtable) obj);
                            responseDestination.setItem(responseOffset, h);
                            responseOffset++;
                        }
                    } else {
                        super.addElement(obj);
                    }
                }
            };
            stack.addElement(node);
            String currentUrl = getUrl();
            if (connectionType.length() > 0 || currentUrl.indexOf("search") >= 0) {
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

    @Override
    public void startArray(String block) {
        if (block.equals("paging")) {
            return;
        }
        Vector items = new Vector();
        Object node;
        if (stack.size() == 1) {
            return;
        } else {
            if (stack.size() == 0) {
                node = new Vector() {

                    @Override
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

    @Override
    public void endArray(String block) {
        if (block.equals("paging")) {
            return;
        }
        if (stack.size() > 1) {
            stack.removeElementAt(stack.size() - 1);
            Object node = stack.elementAt(stack.size() - 1);
            if (node instanceof Hashtable) {
                currentData = (Hashtable) node;
            }
        }
    }

    @Override
    public void endBlock(String block) {
        if (block.equals("paging")) {
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

    @Override
    public void stringToken(String tok) {
    }

    @Override
    public void numericToken(double tok) {
    }

    @Override
    public void keyValue(String key, String value) {
        if (key == null) {
            return;
        }
        //make sure value is not null to prevent NPE
        if (value == null) {
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

    @Override
    public boolean isAlive() {
        return !isKilled();
    }

    @Override
    protected int getYield() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void longToken(long tok) {
    }

    @Override
    public void booleanToken(boolean tok) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        FacebookRESTService that = (FacebookRESTService) o;

        if (responseOffset != that.responseOffset) {
            return false;
        }
        if (connectionType != null ? !connectionType.equals(that.connectionType) : that.connectionType != null) {
            return false;
        }
        if (root != null ? !root.equals(that.root) : that.root != null) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (connectionType != null ? connectionType.hashCode() : 0);
        result = 31 * result + responseOffset;
        result = 31 * result + (root != null ? root.hashCode() : 0);
        return result;
    }

}
