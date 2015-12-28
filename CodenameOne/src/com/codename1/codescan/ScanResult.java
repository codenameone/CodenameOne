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
package com.codename1.codescan;

/**
 * Deprecated!!:  Please use the <a href="https://github.com/codenameone/cn1-codescan">cn1-codescan library</a> instead.
 * 
 * <p>Callback for the code scanner indicating the result of a scan operation,
 * the methods of this call will always be invoked on the EDT!</p>
 *
 * @author Shai Almog
 * @deprecated Use the cn1-codescanner cn1lib.
 */
public interface ScanResult {
    /**
     * Called upon a successful scan operation
     * 
     * @param contents the contents of the data
     * @param formatName the format of the scan
     * @param rawBytes the bytes of data
     */
    public void scanCompleted(String contents, String formatName, byte[] rawBytes);
    
    /**
     * Invoked if the user canceled the scan
     */
    public void scanCanceled();
    
    /**
     * Invoked if an error occurred during the scanning process
     * 
     * @param errorCode code
     * @param message descriptive message
     */
    public void scanError(int errorCode, String message);
}
