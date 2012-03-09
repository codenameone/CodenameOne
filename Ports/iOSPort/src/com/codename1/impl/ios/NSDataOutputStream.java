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
package com.codename1.impl.ios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implements the output stream interface on top of NSData
 *
 * @author Shai Almog
 */
public class NSDataOutputStream extends ByteArrayOutputStream {
    private String file;
    private long nsDataPtr;
    private int written;
    public NSDataOutputStream(String file) {
        this.file = file;
    }

    public NSDataOutputStream(String file, int offset) throws IOException {
        this.file = file;
        write(readOff(file, offset));
    }
    
    private static byte[] readOff(String file, int offset) throws IOException {
        byte[] bytes = new byte[IOSNative.getFileSize(file)];
        IOSNative.readFile(file, bytes);
        if(offset != bytes.length) {
            byte[] d = new byte[offset];
            System.arraycopy(bytes, 0, d, 0, Math.min(offset, bytes.length));
            return d;
        } 
        return bytes;
    }
    
    public void flush() throws IOException {
        super.flush();
        byte[] b = toByteArray();
        if(b.length == written) {
            return;
        }
        written = b.length;
        IOSNative.writeToFile(b, file);
    }
    
    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }

    
}
