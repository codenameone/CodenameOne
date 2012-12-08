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

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements the output stream interface on top of NSData
 *
 * @author Shai Almog
 */
public class NSDataOutputStream extends OutputStream {
    private String file;
    private boolean written;
    public NSDataOutputStream(String file) {
        this.file = file;
    }

    public NSDataOutputStream(String file, int offset) throws IOException {
        this.file = file;
        if(offset > 0) {
            long l = FileSystemStorage.getInstance().getLength(file);
            if(l < offset) {
                byte[] data = new byte[offset];
                InputStream is = FileSystemStorage.getInstance().openInputStream(file);
                Util.readAll(is, data);
                is.close();
                write(data);
            } else {
                written = true;
            }
        }
    }
        
    @Override
    public void write(int b) throws IOException {
        write(new byte[] {(byte)b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        if(written) {
            IOSImplementation.nativeInstance.appendToFile(b, file);
        } else {
            IOSImplementation.nativeInstance.writeToFile(b, file);
        }
        written = true;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(off == 0 && len == b.length) {
            write(b);
            return;
        }
        byte[] arr = new byte[len];
        System.arraycopy(b, off, arr, 0, len);
        write(arr);
    }

    
}
