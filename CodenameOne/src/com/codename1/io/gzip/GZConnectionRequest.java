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
import com.codename1.ui.Display;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>A connection request that can detect a GZipped response, parse it automatically and unzip it. 
 * Notice that some devices (iOS) always request gzip'ed data and always decompress it for us, however in 
 * the case of iOS it doesn't remove the gziped header. The {@code GZConnectionRequest} is aware of such 
 * behaviors so it's better to use that when connecting to the network (if applicable).</p>
 * <p>
 * By default `GZConnectionRequest` doesn't request gzipped data (only unzips it when its received) but it's 
 * pretty easy to do so just add the HTTP header `Accept-Encoding: gzip` e.g.:
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/9a4c6f49d836ca173235.js"></script>
 *
 * @author Shai Almog
 */
public class GZConnectionRequest extends ConnectionRequest {
    private boolean isGzipped;

    /**
     * {@inheritDoc}
     */
    protected void readHeaders(Object connection) throws IOException {
        super.readHeaders(connection);
        
        // ios does gzip seamlessly so this class will just break
        if(!Display.getInstance().getProperty("os.gzip", "false").equals("true")) {
            String c = getHeader(connection, "Content-Encoding");
            isGzipped = c != null && c.equalsIgnoreCase("gzip");
        }
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
