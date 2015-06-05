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

import com.codename1.io.Util;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple input stream that wraps an NSFileHandle object
 *
 * @author Steve Hannah
 */

public class NSFileInputStream extends InputStream {
    private long nsFileHandle;
    private int length;
    private int markOffset = 0;
    public NSFileInputStream(long peer, int length) {
        this.nsFileHandle = peer;
        this.length = length;
    }
    public NSFileInputStream(String file) {
        nsFileHandle = IOSImplementation.nativeInstance.createNSFileHandle(file);
        length = IOSImplementation.nativeInstance.getNSFileSize(nsFileHandle);
    }

    public NSFileInputStream(String name, String type) {
        nsFileHandle = IOSImplementation.nativeInstance.createNSFileHandle(name, type);
        length = IOSImplementation.nativeInstance.getNSFileSize(nsFileHandle);
    }
    
    long getNSFileHandle() {
        return nsFileHandle;
    }

    @Override
    public int available() throws IOException {
        return IOSImplementation.nativeInstance.getNSFileAvailable(nsFileHandle);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    

    
    
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int offset = getOffset();
        if(offset >= length) {
            return -1;
        }
        if(offset + len > length) {
            len = length - offset;
        }
        IOSImplementation.nativeInstance.readFile(nsFileHandle, b, off, len);
        offset += len;
        return len;
    }

    @Override
    public int read() throws IOException {
        int offset = IOSImplementation.nativeInstance.getNSFileOffset(nsFileHandle);
        if(offset >= length) {
            return -1;
        }
        int val = IOSImplementation.nativeInstance.readNSFile(nsFileHandle);
        offset++;
        return val;
    }

    @Override
    public void reset() throws IOException {
        setOffset(markOffset);
    }

    @Override
    public long skip(long n) throws IOException {
        int offset = getOffset();
        if(offset + n >= length) {
            n = n - (offset + n - length);
            offset = length;
        } else {
            offset += (int)n;
        }
        setOffset(offset);
        return n;
    }

    private int getOffset() {
        return IOSImplementation.nativeInstance.getNSFileOffset(nsFileHandle);
    }
    
    private void setOffset(int off) {
        IOSImplementation.nativeInstance.setNSFileOffset(nsFileHandle, off);
    }
    
    @Override
    public void mark(int readlimit) {
        markOffset = getOffset();
    }

    @Override
    public boolean markSupported() {
        return true;
    }
    
    public void finalize() {
        close();
    }
    
    public void close() {
        if(nsFileHandle != 0) {
            IOSImplementation.nativeInstance.releasePeer(nsFileHandle);
            nsFileHandle = 0;
        }        
    }
        
}
