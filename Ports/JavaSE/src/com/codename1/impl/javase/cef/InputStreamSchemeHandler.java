// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import com.codename1.io.Log;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

/**
 * The example for the second scheme with domain handling is a more
 * complex example and is taken from the parent project CEF. Please
 * see CEF: "cefclient/scheme_test.cpp" for futher details
 */
public class InputStreamSchemeHandler extends CefResourceHandlerAdapter {
    

    
    public static final String scheme = "cn1stream";
    public static final String domain = "cn1app";
    //public static final String domain = "tests";
    private StreamWrapper stream;
    
    
    private byte[] data_;
    private String mime_type_;
    private int offset_ = 0;
    private boolean closed;
    
    

    public InputStreamSchemeHandler() {
        super();

    }
    
    public static String getURL(String streamId) {
        return scheme+"://"+domain+"/streams/"+streamId;
    }
    private static final int LIMIT = 65536*2;
    private int written;
    boolean rangeRequest;
    long rangeStart;
    
    @Override
    public synchronized boolean processRequest(CefRequest request, CefCallback callback) {
        
        Map headerMap = new HashMap();
        request.getHeaderMap(headerMap);
        String range = (String)headerMap.get("Range");
        
        System.out.println("In processRequest "+request.getURL()+" " +headerMap+" handler="+this);
        String url = request.getURL();
        String streamId = null;
        if (url.indexOf("/") != -1) {
            streamId = url.substring(url.lastIndexOf("/")+1);
        }
        
        if (streamId != null) {
            stream = BrowserPanel.getStreamRegistry().getStream(streamId);
            
            if (range != null) {
                if (range.indexOf("=") != -1) {
                    rangeRequest = true;
                    range = range.substring(range.indexOf("=")+1);
                    String startStr = range.substring(0, range.indexOf("-"));
                    long start = Long.parseLong(startStr);
                    rangeStart = start;
                    if (stream.getOffset() < start) {
                        InputStream inputStream = stream.getStream();
                        int seekTo = (int)(start- stream.getOffset());
                        try {
                            for (int i=0; i<seekTo; i++) {
                                inputStream.read();
                            }
                            stream.setOffset(start);
                        } catch (IOException ex) {}

                    }
                }

            }
        }
        System.out.println("Stream found "+stream+" offset "+stream.getOffset());
        
        callback.Continue();
        
        
        return stream != null;
    }

    
    
    
    @Override
    public void getResponseHeaders(
            CefResponse response, IntRef response_length, StringRef redirectUrl) {
        System.out.println("In getResponseHeaders");
        if (stream != null) {
            System.out.println("InputStreamSchemeHandler:: "+stream.getMimeType()+" len="+stream.getLength());
            String mime = stream.getMimeType();
            if (mime == null) {
                mime = "application/octet-stream";
                //mime = "audio/wav";
            }
            
            response.setMimeType(mime);
            response.setHeaderByName("Accept-Ranges", "bytes", true);
            if (rangeRequest) {
                response.setHeaderByName("Content-Range", rangeStart+"-"+(stream.getLength())+"/"+stream.getLength(), true);
                response.setHeaderByName("Content-Length", ""+(stream.getLength()-rangeStart), true);
                response.setStatus(206);
                response_length.set((int)(stream.getLength()-rangeStart));
            } else {
                //response.setHeaderByName("Accept-Ranges", "bytes", true);
                //response.setHeaderByName("Content-Length", ""+(stream.getLength() - stream.getOffset()), true);
                //long remaining = stream.getLength() - stream.getOffset();

                response_length.set((int)(stream.getLength() - rangeStart));
                response.setStatus(200);
            }
            
            
            //response_length.set(-1);
            System.out.println("Set response length to "+response_length.get());
            
        } else {
            String msg = "Not found";
            data_ = msg.getBytes();
            mime_type_ = "text/plain";
            response.setMimeType(mime_type_);
            response.setStatus(404);
            response_length.set(data_.length);
        }

    }

    @Override
    public synchronized boolean readResponse(
            byte[] data_out, int bytes_to_read, IntRef bytes_read, CefCallback callback) {
        
        System.out.println("readResponse:"+data_out.length+", "+bytes_to_read);
        try {
            boolean has_data = true;
            if (closed) {
                System.out.println("Stream was closed");
                bytes_read.set(0);
                return false;
            }
            if (stream == null) {
                has_data = false;

                if (offset_ < data_.length) {
                    // Copy the next block of data into the buffer.
                    int transfer_size = Math.min(bytes_to_read, (data_.length - offset_));
                    System.arraycopy(data_, offset_, data_out, 0, transfer_size);
                    offset_ += transfer_size;

                    bytes_read.set(transfer_size);
                    has_data = true;
                } else {
                    offset_ = 0;
                    bytes_read.set(0);
                }
                System.out.println("Stream was null");
                return has_data;
            }

            try {

                if (stream.getStream().available() > 0) {
                    System.out.println("Abbout to attempt reading "+bytes_to_read);
                    int read = stream.getStream().read(data_out, 0, bytes_to_read > 0 ? Math.min(bytes_to_read, data_out.length) : data_out.length);
                    
                    System.out.println("Read "+read+" from stream");
                    if (read == -1) {
                        System.out.println("Reached the end of the stream");
                        has_data = false;
                        bytes_read.set(0);
                        stream.getStream().close();
                        closed = true;
                        BrowserPanel.getStreamRegistry().removeStream(stream);
                        stream = null;
                        return false;
                    } else {
                        written += read;
                        //System.out.println("Zero bytes were available");
                        long oldOffset = stream.getOffset();
                        oldOffset += read;
                        stream.setOffset(oldOffset);
                        bytes_read.set(read);
                        return true;
                    }

                } else {
                    System.out.println("No bytes available");
                    bytes_read.set(0);
                    
                    
                }
            } catch (IOException ex) {
                Log.e(ex);
            }
            System.out.println("Returning "+has_data);
            
            return has_data;
        } finally {
            System.out.println("Exiting readResponse");
        }
    }
    

    
}
