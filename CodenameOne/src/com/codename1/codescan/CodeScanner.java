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

import com.codename1.ui.Display;
import java.util.Vector;

/**
 * Deprecated!!:  Please use the <a href="https://github.com/shannah/cn1-codescan">cn1-codescan library</a> instead.
 * 
 * <p>A barcode/qrcode scanner API, this class is a singleton, notice that this
 * API might not be implemented for all platforms in which case the getInstance()
 * method will return null!</p>
 *
 * @author Shai Almog
 * @deprecated Use the cn1-codescanner cn1lib.
 */
@Deprecated
public abstract class CodeScanner {
    
    /**
     * Returns the instance of the code scanner, notice that this method is equivalent 
     * to Display.getInstance().getCodeScanner().
     * 
     * @return instance of the code scanner
     */
    public static CodeScanner getInstance() {
        return Display.getInstance().getCodeScanner();
    }
        
    /**
     * Scans based on the settings in this class and returns the results
     * 
     * @param callback scan results
     */
    public abstract void scanQRCode(ScanResult callback);
        
    /**
     * Scans based on the settings in this class and returns the results
     * 
     * @param callback scan results
     */
    public abstract void scanBarCode(ScanResult callback);
}
