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
import java.util.Date;

/**
 * This class represent an access token.
 * 
 * @author Chen
 */
public class AccessToken implements Externalizable{
    
    private String token;
    
    private String expires;

    /**
     * Constructor with parameters
     * 
     * @param token the token string
     * @param expires the access token expires date
     */ 
    public AccessToken(String token, String expires) {
        this.token = token;
        this.expires = expires;
    }

    /**
     * Simple getter
     * @return the token string
     */ 
    public String getToken() {
        return token;
    }

    /**
     * Simple getter
     * @return the expires date
     */ 
    public String getExpires() {
        return expires;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void externalize(DataOutputStream out) throws IOException {
        Util.writeUTF(token, out);
        Util.writeUTF(expires, out);
    }

    @Override
    public void internalize(int version, DataInputStream in) throws IOException {
        token = Util.readUTF(in);
        expires = Util.readUTF(in);
    }

    @Override
    public String getObjectId() {
        return "AccessToken";
    }
    
    
    
}
