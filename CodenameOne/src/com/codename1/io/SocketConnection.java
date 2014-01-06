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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Callback for establishment of a socket connection. Notice this callback
 * is always invoked on a new separate thread to allow uninterrupted IO.
 * 
 * @author Shai Almog
 */
public abstract class SocketConnection {
    private boolean connected;
    
    /**
     * Invoked in case of an error in the socket connection, this method is invoked off the EDT
     * @param errorCode the error code
     * @param message error message if applicable
     */
    public abstract void connectionError(int errorCode, String message);
    
    /**
     * Invoked when a socket connection is established, this method is invoked off the EDT
     * @param is input stream for the socket
     * @param os output stream for the socket
     */
    public abstract void connectionEstablished(InputStream is, OutputStream os);
    
    /**
     * Returns true if this connection is currently active
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    void setConnected(boolean b) {
        connected = b;
    }
}
