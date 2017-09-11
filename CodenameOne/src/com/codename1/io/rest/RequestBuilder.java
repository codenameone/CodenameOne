/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.io.rest;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkEvent;
import com.codename1.io.gzip.GZConnectionRequest;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.Base64;
import com.codename1.util.Callback;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to build, invoke the http request and to get the http 
 * response 
 *
 * @author Chen Fishbein
 */
public class RequestBuilder {

    private String method;

    private String url;

    private Map<String, String> queryParams = new HashMap();

    private Map<String, String> headers = new HashMap();

    private Map<String, String> pathParams = new HashMap();
    
    private Integer timeout;
    
    private String body;
    
    private boolean isGzip = false;
    
    RequestBuilder(String method, String url) {
        this.method = method;
        this.url = url;
    }

    /**
     * Add a path param to the request.
     * For example if the request url is: http://domain.com/users/{id}
     * The path param can be - key="id", value="1"
     * When the request executes the path would be: http://domain.com/users/1
     * 
     * @param key the identifier key in the request.
     * @param value the value to replace in the url
     * @return RequestBuilder instance
     */ 
    public RequestBuilder pathParam(String key, String value) {
        pathParams.put(key, value);
        return this;
    }
    
    /**
     * Add a query parameter to the request
     * 
     * @param key param key
     * @param value  param value
     * @return RequestBuilder instance
     */ 
    public RequestBuilder queryParam(String key, String value) {
        queryParams.put(key, value);
        return this;
    }

    /**
     * Add a header to the request
     * 
     * @param key 
     * @param value 
     * @return RequestBuilder instance
     */ 
    public RequestBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }
    
    /**
     * Sets the request body
     * 
     * @param body request body 
     * @return RequestBuilder instance
     */ 
    public RequestBuilder body(String body) {
        this.body = body;
        return this;
    }
    
    /**
     * Sets the request timeout
     * 
     * @param timeout request timeout in milliseconds
     * @return RequestBuilder instance
     */ 
    public RequestBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * Sets the request to be a gzip request
     * 
     * @return RequestBuilder instance
     */ 
    public RequestBuilder gzip() {
        isGzip = true;
        header("Accept-Encoding", "gzip");
        return this;
    }
    
    /**
     * Add accept json header to the request
     * 
     * @return RequestBuilder instance
     */ 
    public RequestBuilder acceptJson() {
        header("Accept", "application/json");
        return this;
    }

    /**
     * Add a basic authentication Authorization header
     * 
     * @return RequestBuilder instance
     */ 
    public RequestBuilder basicAuth(String username, String password) {
        header("Authorization", "Basic " + Base64.encodeNoNewline((username + ":" + password).getBytes()));
        return this;
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     */ 
    public void getAsStringAsync(final Callback<Response<String>> callback) {
        ConnectionRequest request = createRequest();
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                try {
                    res = new Response(evt.getResponseCode(), new String(evt.getConnectionRequest().getResponseData(), "UTF-8"));
                    callback.onSucess(res);
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
            }
        });
        CN.addToQueue(request);        
    }

    /**
     * Executes the request synchronously
     * 
     * @return Response Object
     */ 
    public Response<String> getAsString() {
        ConnectionRequest request = createRequest();
        CN.addToQueueAndWait(request);
        Response res = null;
        try {
            res = new Response(request.getResponseCode(), new String(request.getResponseData(), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     */ 
    public void getAsBytesAsync(final Callback<Response<byte[]>> callback) {
        ConnectionRequest request = createRequest();
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                res = new Response(evt.getResponseCode(), evt.getConnectionRequest().getResponseData());
                callback.onSucess(res);
            }
        });
        CN.addToQueue(request);        
    }
    
    /**
     * Executes the request synchronously
     * 
     * @return Response Object
     */ 
    public Response<byte[]> getAsBytes() {
        ConnectionRequest request = createRequest();
        CN.addToQueueAndWait(request);
        Response res = new Response(request.getResponseCode(), request.getResponseData());
        return res;
    }

    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     */ 
    public void getAsJsonMapAsync(final Callback<Response<Map>> callback) {
        ConnectionRequest request = createRequest();
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                byte[] data = evt.getConnectionRequest().getResponseData();
                JSONParser parser = new JSONParser();
                try {
                    Map response = parser.parseJSON(new InputStreamReader(new ByteArrayInputStream(data), "UTF-8"));
                    res = new Response(evt.getResponseCode(), response);
                    callback.onSucess(res);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        CN.addToQueue(request);        
    }
    
    /**
     * Executes the request synchronously
     * 
     * @return Response Object
     */ 
    public Response<Map> getAsJsonMap() {
        ConnectionRequest request = createRequest();
        CN.addToQueueAndWait(request);
        Response res = null;
        byte[] data = request.getResponseData();
        JSONParser parser = new JSONParser();
        try {
            Map response = parser.parseJSON(new InputStreamReader(new ByteArrayInputStream(data), "UTF-8"));
            res = new Response(request.getResponseCode(), response);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    private ConnectionRequest createRequest() {

        ConnectionRequest req;
        if(!isGzip){
            req = new ConnectionRequest();
        }else{
            req = new GZConnectionRequest();
        }
        for (String key : pathParams.keySet()) {
            url = com.codename1.util.StringUtil.replaceAll(url, "{" + key + "}", pathParams.get(key));
        }        
        req.setReadResponseForErrors(true);
        req.setDuplicateSupported(true);
        req.setUrl(url);
        req.setHttpMethod(method);
        req.setPost(method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT"));
        if(body != null){
            req.setRequestBody(body);
            req.setWriteRequest(true);
        }
        if(timeout != null){
            req.setTimeout(timeout);
        }
        for (String key : queryParams.keySet()) {
            req.addArgument(key, queryParams.get(key));
        }
        for (String key : headers.keySet()) {
            req.addRequestHeader(key, headers.get(key));
        }

        return req;
    }

}
