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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple input stream that wraps an NSData object
 *
 * @author Shai Almog
 */
public class NSDataInputStream extends InputStream {
    private long nsData;
    private int offset = 0;
    private int length;
    private int markOffset = 0;
    public NSDataInputStream(String file) {
        nsData = IOSImplementation.nativeInstance.createNSData(file);
        length = IOSImplementation.nativeInstance.getNSDataSize(nsData);
    }

    @Override
    public int available() throws IOException {
        return length - offset;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    byte[] getArray() {
        byte[] bytes = new byte[length];
        IOSImplementation.nativeInstance.nsDataToByteArray(nsData, bytes);
        close();
        return bytes;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(offset >= length) {
            return -1;
        }
        if(offset + len > length) {
            len = length - offset;
        }
        IOSImplementation.nativeInstance.read(nsData, b, off, len, offset);
        return len;
    }

    @Override
    public int read() throws IOException {
        if(offset >= length) {
            return -1;
        }
        int val = IOSImplementation.nativeInstance.read(nsData, offset);
        offset++;
        return val;
    }

    @Override
    public void reset() throws IOException {
        offset = markOffset;
    }

    @Override
    public long skip(long n) throws IOException {
        if(offset + n >= length) {
            n = n - (offset + n - length);
            offset = length;
        } else {
            offset += (int)n;
        }
        
        return n;
    }

    @Override
    public void mark(int readlimit) {
        markOffset = offset;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
    
    public void finalize() {
        close();
    }
    
    public void close() {
        if(nsData != 0) {
            IOSImplementation.nativeInstance.releasePeer(nsData);
            nsData = 0;
        }        
    }
        
}
