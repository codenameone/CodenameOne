/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
