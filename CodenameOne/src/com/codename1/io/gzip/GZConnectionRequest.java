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
package com.codename1.io.gzip;

import com.codename1.io.ConnectionRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * A connection request that can detect a GZipped response and parse it
 *
 * @author Shai Almog
 */
public class GZConnectionRequest extends ConnectionRequest {
    private boolean isGzipped;

    /**
     * @inheritDoc
     */
    protected void readHeaders(Object connection) throws IOException {
        super.readHeaders(connection);
        String c = getHeader(connection, "Content-Encoding");
        isGzipped = c != null && c.equalsIgnoreCase("gzip");
    }

    /**
     * Overridden to convert the input stream you should now override readUnzipedResponse()
     */
    protected final void readResponse(InputStream input) throws IOException {
        if(isGzipped) {
            readUnzipedResponse(new GZIPInputStream(input));
        } else {
            readUnzipedResponse(input);
        }
    }
    
    /**
     * This method can be overridden instead of readResponse 
     * @param input an input stream that is guaranteed to be deflated
     */
    protected void readUnzipedResponse(InputStream input) throws IOException {
        super.readResponse(input);
    }
}
