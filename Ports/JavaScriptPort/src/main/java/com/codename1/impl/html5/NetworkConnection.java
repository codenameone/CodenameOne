/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.teavm.io.ArrayBufferInputStream;
import com.codename1.teavm.io.BlobUtil;
import com.codename1.teavm.jso.io.Blob;
import com.codename1.util.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.ReadyStateChangeHandler;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.core.JSRegExp;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 *
 * @author shannah
 */
public class NetworkConnection implements JavaScriptNetworkAdapter.Connection {
    private String url;
    private boolean read;
    private boolean write;
    private int timeout=5000;
    private XMLHttpRequest req;
    private ByteArrayOutputStream body;
    private boolean post;
    private boolean isOpen;
    private InputStream inputStream;
    private String httpMethod="GET";
    private ArrayList<Header> headers = new ArrayList<Header>();
    
    private class Header {
        String name;
        String value;
        
        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
    
    

    public NetworkConnection(String url, boolean read, boolean write, int timeout){
        this.url = url;
        this.read = read;
        this.write = write;
        this.timeout = timeout;
        req = XMLHttpRequest.create();
    }
    
    private static interface XHRBlob extends JSObject {
        void send(Blob blob);
    }
    
    private void open(){
        if (!isOpen){
            isOpen = true;
            req.open(httpMethod, url, true);
            req.setResponseType("arraybuffer");
        }
    }

    public void setHeader(String key, String val) {
        //open();
        if ("user-agent".equals(key.toLowerCase())){
            return;
        }
        if ("cookie".equalsIgnoreCase(key)) {
            setHeader("X-CN1-Cookie", val);
            return;
        }
        if ("content-length".equalsIgnoreCase(key)) {
            setHeader("X-CN1-Content-Length", val);
            return;
        }
        try {
            headers.add(new Header(key, val));
        } catch (Throwable t){
            
        }
        
    }

    public int getContentLength(){
        if (req.getResponse()==null){
            return 0;
        } else {
            return ((ArrayBuffer)req.getResponse()).getByteLength();
        }
        
    }


    public void setHttpMethod(String method) {
        this.httpMethod = method;
        if ("post".equalsIgnoreCase(method) && !post) {
            setPostRequest(true);
        }
    } 
    
    public String getHttpMethod() {
        return this.httpMethod;
    }
    
    public InputStream openInputStream() throws IOException {
         if (inputStream!=null){
             return inputStream;
         }
         
        final Object lock = new Object();
        open();
        for (Header h : headers) {
            req.setRequestHeader(h.name, h.value);
        }
        final boolean[] complete = new boolean[1];
        req.setOnReadyStateChange(new ReadyStateChangeHandler(){

            @Override
            public void stateChanged() {

                if ( req.getReadyState() == XMLHttpRequest.DONE){
                    new Thread() {
                        @Override
                        public void run() {
                            
                            complete[0] = true;
                            synchronized (lock){
                                lock.notifyAll();
                            }
                        }
                    }.start();
                }
            }

        });
        //req.setResponseType("arraybuffer");
        if ( body != null ){
            
            //req.send(new String(body.toByteArray(), "UTF-8"));
            ((XHRBlob)req).send(BlobUtil.createBlob(body.toByteArray(), "application/octet-stream"));
        } else {
            req.send();
        }
        if (!complete[0]){
            synchronized(lock){
                try {
                    lock.wait(timeout);
                } catch (InterruptedException ex) {
                    Log.e(ex);
                }
            }
        }

        
        if (req.getResponse() == null || req.getStatus() == 0 ){
            System.out.println(req.getAllResponseHeaders());
            System.out.println(req.getStatusText());
            System.out.println("Failed to load url "+url);
            System.out.println("Status code was "+req.getStatus());
            throw new IOException("Failed to load "+url+".  Status "+req.getStatusText());
        }
        
        inputStream = new ArrayBufferInputStream(Uint8Array.create((ArrayBuffer)req.getResponse()), req.getResponseType());
        return inputStream;


        
    }

     public OutputStream openOutputStream() throws IOException {
         body = new ByteArrayOutputStream();
         return body;
     }

    public OutputStream openOutputStream( int offset) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setPostRequest(boolean post){
        this.post = post;
        if (post && !"POST".equalsIgnoreCase(httpMethod)) {
            this.setHttpMethod("POST");
        }
    }

    public int getResponseCode() throws IOException {
        if (inputStream == null){
            openInputStream();
        }
        if (req.getStatus() == 0 && req.getResponse() != null){
            // Workaround for file:// ajax requests which always seem to return
            // 0 response code
            return 200;
        }
        
        // Since XMLHttpRequest automatically follows redirects, we set up 
        // the proxy to send redirect statuses as nonstandard header
        String xStatus = getHeaderField("X-CN1-Status");
            
        if (xStatus != null) {
            //log("FOUND X-CN1-Status Header");
            //log(xStatus);
            return Integer.parseInt(xStatus);
        } else {
            return req.getStatus();
        }
    }

    public String getResponseMessage() throws IOException {
        if (inputStream == null){
            openInputStream();
        }
        return req.getStatusText();
    }

    public String getHeaderField(String name){
        String[] out = getHeaderFields(name);
        if (out.length > 0){
            return out[0];
        } else {
            return null;
        }
    }

    public String[] getHeaderFieldNames() {
        List<String> out = new ArrayList<String>();
        List<String> headerStrings =  StringUtil.tokenize(req.getAllResponseHeaders(), "\n");
        for (String str : headerStrings){
            int colonPos = str.indexOf(":");
            if (colonPos < 0) {
                continue;
            }
            String hname = str.substring(0, colonPos).trim();
            out.add(hname);
            if ("X-CN1-Location".equalsIgnoreCase(hname)) {
                out.add("Location");
            }
            if ("X-CN1-Set-Cookie".equalsIgnoreCase(hname)) {
                out.add("Set-Cookie");
            }
        }
        
        return out.toArray(new String[out.size()]);
    }

    
    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(String str);
    
    public String[] getHeaderFields(String name) {
        List<String> flds =  StringUtil.tokenize(req.getAllResponseHeaders(), "\n");
        List<String> out = new ArrayList<String>();
        
        
        for (String header : flds){
            if (header == null) {
                continue;
            }
            //System.out.println("Header field "+header);
            int colonPos = header.indexOf(":");
            if (colonPos >= 0) {
                String hName = header.substring(0, colonPos).trim();
                if ("X-CN1-Set-Cookie".equalsIgnoreCase(hName)) {
                    hName = "Set-Cookie";
                }
                String val = header.substring(colonPos+1).trim();
                //System.out.println("name="+hName+", val="+val);
                if (name.equalsIgnoreCase(hName) && hName.equalsIgnoreCase("Set-Cookie") && val.indexOf(",") > -1) {
                    int pos = 0;
                    int len = val.length();
                    StringBuilder sb = new StringBuilder();
                    char c;
                    boolean escaping = false;
                    int lastNonAlphaNumeric = -1;
                    while (pos < len) {
                        c = val.charAt(pos);
                        if (c=='=') {
                            
                            String key = val.substring(lastNonAlphaNumeric+1, pos);
                            if (key.trim().equalsIgnoreCase("expires")) {
                                escaping = true;
                            } else {
                                escaping = false;
                            }
                            
                            sb.append(c);
                        } else if (c == ',' && escaping) {
                            sb.append("CN1$$$");
                        } else if (c == ',' || c == ';' || c == ' ') {
                            if (!escaping) {
                                lastNonAlphaNumeric = pos;
                            } else if (c == ';') {
                                escaping = false;
                            }
                            sb.append(c);
                        } else {
                            sb.append(c);
                        }
                        pos++;
                    }
                    val = sb.toString();
                    String[] parts = Util.split(val, ",");
                    for (String part : parts) {
                        out.add(part.trim().replace("CN1$$$", ","));
                    }
                    continue;
                }
                
                if ("X-CN1-Location".equalsIgnoreCase(hName) && "Location".equalsIgnoreCase(name)) {
                    out.add(val);
                } else if (name.equalsIgnoreCase(hName)){
                    out.add(val);
                }
            }
        }
        return out.toArray(new String[out.size()]);
    }
    
    
    public void cleanup(){
        inputStream = null;
    }



}
