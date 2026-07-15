/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.impl.html5;

import com.codename1.teavm.io.ArrayBufferInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author shannah
 */
public class MediaInputStream extends InputStream{
    
    private final String src;
    private ArrayBufferInputStream buf;
    private final HTML5Implementation impl;
    
    public MediaInputStream(String src, HTML5Implementation impl){
        this.src=src;
        this.impl=impl;
    }

    
    
    @Override
    public int read() throws IOException {
        //System.out.println("in MediaInputStream.read");
        if (buf==null){
            buf=(ArrayBufferInputStream)impl.getArrayBufferInputStream(src);
        }
        return buf.read();
    }

    public String getSrc() {
        return src;
    }
    
    
    
}
