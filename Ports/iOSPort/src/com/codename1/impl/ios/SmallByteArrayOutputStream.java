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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Workaround for a BOHEM GC limitation of allocating large byte array output
 * streams for large blocks of data
 *
 * @author Shai Almog
 */
public class SmallByteArrayOutputStream extends OutputStream {
    private List<byte[]> bytes = new ArrayList<byte[]>();

    @Override
    public void write(int b) throws IOException {
        bytes.add(new byte[] {(byte)b});
    }


    @Override
    public void write(byte[] b) throws IOException {
        if(b.length == 0) {
            return;
        }
        bytes.add(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(b.length == 0) {
            return;
        }
            if(off == 0 && len == b.length) {
            write(b);
            return;
        }
        byte[] buf = new byte[len];
        System.arraycopy(b, off, buf, 0, len);
        write(buf);
    }
    
    public byte[] toByteArray() {
        int count = 0;
        for(byte[] c : bytes) {
            count += c.length;
        }
        byte[] response = new byte[count];
        count = 0;
        for(byte[] c : bytes) {
            System.arraycopy(c, 0, response, count, c.length);
            count += c.length;
        }
        return response;
    }
}
