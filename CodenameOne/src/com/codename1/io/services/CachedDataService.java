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
package com.codename1.io.services;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.ui.events.ActionListener;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple service that allows downloading and caching data locally.
 * When the data is requested again the server is queried with a conditional
 * get query.
 *
 * @author Shai Almog
 */
public class CachedDataService extends ConnectionRequest {
    private CachedData data = new CachedData();
    
    private CachedDataService() {}

    /**
     * Makes sure the cached data class is properly registered as an externalizable. This must
     * be invoked for caching to work
     */
    public static void register() {        
        Util.register("CachedData", CachedData.class);
    }
    
    /**
     * Checks that the cached data is up to date and if a newer version exits it updates the data in place
     * 
     * @param d the data to check
     * @param callback optional callback to be invoked on request completion
     */
    public static void updateData(CachedData d, ActionListener callback) {
        if(d.isFetching()) {
            return;
        }
        d.setFetching(true);
        CachedDataService c = new CachedDataService();
        c.setUrl(d.getUrl());
        c.setPost(false);
        if(callback != null) {
            c.addResponseListener(callback);
        }
        if(d.getModified() != null && d.getModified().length() > 0) {
            c.addRequestHeader("If-Modified-Since", d.getModified());
            if(d.getEtag() != null) {
                c.addRequestHeader("If-None-Match", d.getEtag());
            }
        }
        NetworkManager.getInstance().addToQueue(c);        
    }
    
    /**
     * @inheritDoc
     */
    protected void handleException(Exception err) {
        data.setFetching(false);
        super.handleException(err);
    }
    
    /**
     * @inheritDoc
     */
    protected void handleErrorResponseCode(int code, String message) {
        data.setFetching(false);
        if(code == 304) {
            // data unmodified
            return;
        }
        super.handleErrorResponseCode(code, message);
    }


    /**
     * @inheritDoc
     */
    protected void readHeaders(Object connection) throws IOException {
        String last = getHeader(connection, "Last-Modified");
        String etag = getHeader(connection, "ETag");
        if(last != null && last.length() > 0) {
            data.setModified(last);
            data.setEtag(etag);
        }
    }

    /**
     * @inheritDoc
     */
    protected void readResponse(InputStream input) throws IOException  {
        data.setData(Util.readInputStream(input));
        fireResponseListener(new NetworkEvent(this, data));
        data.setFetching(false);
    }
}
