/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import java.io.FileInputStream;
import java.io.InputStream;
import org.cef.callback.CefCallback;
import org.cef.network.CefRequest;

/**
 *
 * @author shannah
 */
public class StreamWrapper {

    public StreamWrapper(InputStream fis, String mimetype, long length) {
        this.stream = fis;
        this.mimeType = mimetype;
        this.length = length;
                
    }

    /**
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(long length) {
        this.length = length;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the stream
     */
    public InputStream getStream() {
        return stream;
    }

    /**
     * @param stream the stream to set
     */
    public void setStream(InputStream stream) {
        this.stream = stream;
    }
    
    
    
    private long length;
    private String mimeType;
    private InputStream stream;
    private long offset;


    /**
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }
    
  
    
    
}
