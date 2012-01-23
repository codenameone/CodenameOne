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

import com.codename1.io.Externalizable;
import com.codename1.io.Util;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Cached data class for use with the cached data service
 * 
 * @author Shai Almog
 */
public class CachedData implements Externalizable {
    private byte[] data;
    private String url;
    private String etag;
    private String modified;
    
    // lock to prevent multiple requests to the same data
    private boolean fetching;
    
    /**
     * @inheritDoc
     */
    public int getVersion() {
        return 1;
    }

    /**
     * @inheritDoc
     */
    public void externalize(DataOutputStream out) throws IOException {
        Util.writeUTF(url, out);
        Util.writeUTF(etag, out);
        Util.writeUTF(modified, out);
        out.writeInt(data.length);
        out.write(data);
    }

    /**
     * @inheritDoc
     */
    public void internalize(int version, DataInputStream in) throws IOException {
        url = Util.readUTF(in);
        etag = Util.readUTF(in);
        modified = Util.readUTF(in);
        data = new byte[in.readInt()];
        in.readFully(data);
    }

    public String getObjectId() {
        return "CachedData";
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the etag
     */
    String getEtag() {
        return etag;
    }

    /**
     * @param etag the etag to set
     */
    void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * @return the modified
     */
    String getModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    void setModified(String modified) {
        this.modified = modified;
    }

    /**
     * @return the fetching
     */
    boolean isFetching() {
        return fetching;
    }

    /**
     * @param fetching the fetching to set
     */
    void setFetching(boolean fetching) {
        this.fetching = fetching;
    }
    
}
