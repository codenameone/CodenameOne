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

package com.codename1.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A cookie for an HTTP request
 *
 * @author Shai Almog
 */
public class Cookie implements Externalizable {
    private String name;
    private String value;
    private String domain;
    private long expires;

    private static boolean autoStored = true;

    public static String STORAGE_NAME = "Cookies";
    
    static{
        Util.register("Cookie", Cookie.class);
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return the expires
     */
    public long getExpires() {
        return expires;
    }

    /**
     * @param expires the expires to set
     */
    public void setExpires(long expires) {
        this.expires = expires;
    }

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
        out.writeUTF(name);
        if(value != null) {
            out.writeBoolean(true);
            out.writeUTF(value);
        } else {
            out.writeBoolean(false);
        }
        if(domain != null) {
            out.writeBoolean(true);
            out.writeUTF(domain);
        } else {
            out.writeBoolean(false);
        }
        out.writeLong(expires);
    }

    /**
     * @inheritDoc
     */
    public void internalize(int version, DataInputStream in) throws IOException {
        name = in.readUTF();
        if(in.readBoolean()) {
            value = in.readUTF();
        }
        if(in.readBoolean()) {
            domain = in.readUTF();
        }
        expires = in.readLong();
    }

    /**
     * @inheritDoc
     */
    public String getObjectId() {
        return "Cookie";
    }

    /**
     * @inheritDoc
     */
    public String toString() {
        return "name = " + name + " value = " + value + " domain = " + domain;
    }

    /**
     * This method configures the auto storage of cookies
     *
     * @param autoStored
     */
    public static void setAutoStored(boolean autoStored) {
        Cookie.autoStored = autoStored;
    }

    /**
     * Returns true if the Cookies are auto stored to storage
     * 
     * @return autoStored
     */
    public static boolean isAutoStored() {
        return autoStored;
    }
    
    /**
     * Clears all cookies history from storage
     */
    public static void clearCookiesFromStorage() {
        if (Storage.getInstance().exists(Cookie.STORAGE_NAME)) {
            Storage.getInstance().deleteStorageFile(Cookie.STORAGE_NAME);
        }
    }
}
