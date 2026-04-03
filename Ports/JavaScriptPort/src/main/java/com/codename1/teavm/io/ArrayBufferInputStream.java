/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.io;


import com.codename1.teavm.jso.io.Blob;
import java.io.IOException;
import java.io.InputStream;
import com.codename1.html5.js.typedarrays.Uint8Array;


/**
 *
 * @author shannah
 */
public class ArrayBufferInputStream extends InputStream {
    private Uint8Array buf;
    private String type;
    int pos = 0;
    int len;
    String src;
    public ArrayBufferInputStream(Uint8Array buf, String type) {
        this.buf = buf;
        this.type=type;
        this.len = buf.getByteLength();
    }

    @Override
    public int read() throws IOException {
        if ( pos >= len ){
            return -1;
        }
        return buf.get(pos++);
    }

    @Override
    public void reset() throws IOException {
        pos = 0;
    }

    @Override
    public long skip(long n) throws IOException {
        
        int oldPos = pos;
        
        pos += (int)n;
        
        if ( pos > len ){
            pos = len;
        }
        int out = pos-oldPos;
        
        return pos-oldPos;
    }

    @Override
    public int available() throws IOException {
        return len-pos;
    }

    @Override
    public void close() throws IOException {
        buf = null;
        len = 0;
    }

    
    
    public Blob getBlob() {
        return BlobUtil.createBlob(buf, type);
    }
    
    public Uint8Array getBuffer() {
        return buf;
    }
    
    
    public String getSrc() {
        return src;
    }
    
    public void setSrc(String src) {
        this.src = src;
    }
      
}
