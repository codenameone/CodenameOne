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
import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.gzip.GZConnectionRequest;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.Base64;
import com.codename1.util.Callback;
import com.codename1.util.FailureCallback;
import com.codename1.util.OnComplete;
import com.codename1.util.SuccessCallback;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        
    private String contentType;
    
    private ErrorCodeHandler<byte[]> byteArrayErrorCallback;
    private ErrorCodeHandler<Map> jsonErrorCallback;
    private ErrorCodeHandler<String> stringErrorCallback;
    private ErrorCodeHandler<PropertyBusinessObject> propertyErrorCallback;
    private Class errorHandlerPropertyType;
    private ActionListener<NetworkEvent> errorCallback;
    
    RequestBuilder(String method, String url) {
        this.method = method;
        this.url = url;
    }

    /**
     * Sets the value of the content type
     * @param s the content type
     * @return RequestBuilder instance
     */
    public RequestBuilder contentType(String s) {
        contentType = s;
        return this;
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
     * Sets the request body to the JSON matching the given object
     * 
     * @param body request body 
     * @return RequestBuilder instance
     */ 
    public RequestBuilder body(PropertyBusinessObject body) {
        this.body = body.getPropertyIndex().toJSON();
        return this;
    }
        
    /**
     * In case of an error this method is invoked asynchronously to process
     * the error content with the byte array data
     * @param err the content of the error response
     * @return RequestBuilder instance
     */
    public RequestBuilder onErrorCodeBytes(ErrorCodeHandler<byte[]> err) {
        byteArrayErrorCallback = err;
        return this;
    }
    
    /**
     * In case of an error this method is invoked asynchronously to process
     * the error content with the JSON data
     * @param err the content of the error response
     * @return RequestBuilder instance
     */
    public RequestBuilder onErrorCodeJSON(ErrorCodeHandler<Map> err) {
        jsonErrorCallback = err;
        return this;
    }

    /**
     * In case of an error this method is invoked asynchronously to process
     * the error content with the JSON data and places it into a business
     * object in the callback
     * @param err the content of the error response
     * @param errorClass the class of the business object into which the data is parsed
     * @return RequestBuilder instance
     */
    public RequestBuilder onErrorCode(ErrorCodeHandler<PropertyBusinessObject> err, Class errorClass) {
        propertyErrorCallback = err;
        errorHandlerPropertyType = errorClass;
        return this;
    }
    
    /**
     * In case of an error this method is invoked asynchronously to process
     * the error content with the JSON data
     * @param err the content of the error response
     * @return RequestBuilder instance
     */
    public RequestBuilder onErrorCodeString(ErrorCodeHandler<String> err) {
        stringErrorCallback = err;
        return this;
    }
    
    /**
     * Invoked for exceptions or failures such as disconnect
     * @param error callback for a networking error
     * @return RequestBuilder instance
     * 
     */
    public RequestBuilder onError(ActionListener<NetworkEvent> error) {
        errorCallback = error;
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
     * @deprecated this API was implemented incorrectly
     */ 
    public RequestBuilder gzip() {
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
     * Sets both the content type and accept headers to "application/json"
     * @return RequestBuilder instance
     */
    public RequestBuilder jsonContent() {
        return contentType("application/json").
                header("Accept", "application/json");
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
     * Add an authorization bearer header, this is shorthand for 
     * {@code header("Authorization", "Bearer " + token)}
     * 
     * @param token the authorization token
     * @return RequestBuilder instance
     */ 
    public RequestBuilder bearer(String token) {
        header("Authorization", "Bearer " + token);
        return this;
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback invoked with the result of the builder query
     * @return the ConnectionRequest instance
     */
    public ConnectionRequest fetchAsString(final OnComplete<Response<String>> callback) {
        return getAsStringAsyncImpl(callback);
    }
    
    private ConnectionRequest getAsStringAsyncImpl(final Object callback) {
        ConnectionRequest request = createRequest(false);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                try {
                    res = new Response(evt.getResponseCode(), new String(evt.getConnectionRequest().getResponseData(), "UTF-8"), evt.getMessage());
                    if(callback instanceof Callback) {
                        ((Callback)callback).onSucess(res);
                    } else {
                        ((OnComplete<Response<String>>)callback).completed(res);
                    }
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
            }
        });
        CN.addToQueue(request);        
        return request;
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @deprecated please use {@link #fetchAsString(com.codename1.util.OnComplete)} instead
     */ 
    public void getAsStringAsync(final Callback<Response<String>> callback) {
        getAsStringAsyncImpl(callback);
    }

    /**
     * Executes the request synchronously
     * 
     * @return Response Object
     */ 
    public Response<String> getAsString() {
        ConnectionRequest request = createRequest(false);
        CN.addToQueueAndWait(request);
        Response res = null;
        try {
            res = new Response(request.getResponseCode(), new String(request.getResponseData(), "UTF-8"), request.getResponseErrorMessage());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @return the connection request instance
     */ 
    public ConnectionRequest fetchAsBytes(final OnComplete<Response<byte[]>> callback) {
        return getAsBytesAsyncImpl(callback);
    }
    
    private ConnectionRequest getAsBytesAsyncImpl(final Object callback) {
        ConnectionRequest request = createRequest(false);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                res = new Response(evt.getResponseCode(), evt.getConnectionRequest().getResponseData(), evt.getMessage());
                if(callback instanceof Callback) {
                    ((Callback)callback).onSucess(res);
                } else {
                    ((OnComplete)callback).completed(res);
                }
            }
        });
        CN.addToQueue(request);        
        return request;
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @deprecated use {@link #fetchAsBytes(com.codename1.util.OnComplete)} instead
     */ 
    public void getAsBytesAsync(final Callback<Response<byte[]>> callback) {
        getAsBytesAsyncImpl(callback);
    }
    
    /**
     * Executes the request synchronously
     * 
     * @return Response Object
     */ 
    public Response<byte[]> getAsBytes() {
        ConnectionRequest request = createRequest(false);
        CN.addToQueueAndWait(request);
        Response res = new Response(request.getResponseCode(), request.getResponseData(), request.getResponseErrorMessage());
        return res;
    }

    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @return returns the Connection Request object so it can be killed if necessary
     */ 
    public ConnectionRequest fetchAsJsonMap(final OnComplete<Response<Map>> callback) {
        ConnectionRequest request = createRequest(true);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                Map response = (Map)evt.getMetaData();
                res = new Response(evt.getResponseCode(), response, evt.getMessage());
                callback.completed(res);
            }
        });
        CN.addToQueue(request);       
        return request;
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback. This fetches JSON data and parses it into a properties business object
     * @param callback writes the response to this callback
     * @param type the class of the business object returned
     * @return returns the Connection Request object so it can be killed if necessary
     */ 
    public ConnectionRequest fetchAsProperties(final OnComplete<Response<PropertyBusinessObject>> callback, final Class type) {
        ConnectionRequest request = createRequest(true);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                Response res = null;
                Map response = (Map)evt.getMetaData();
                try {
                    PropertyBusinessObject pb = (PropertyBusinessObject)type.newInstance();
                    pb.getPropertyIndex().populateFromMap(response);
                    res = new Response(evt.getResponseCode(), pb, evt.getMessage());
                    callback.completed(res);
                } catch(Exception err) {
                    Log.e(err);
                    throw new RuntimeException(err.toString());
                }
            }
        });
        CN.addToQueue(request);       
        return request;
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @return returns the Connection Request object so it can be killed if necessary
     * @deprecated use {@link #fetchAsJsonMap(com.codename1.util.OnComplete)} instead
     */ 
    public ConnectionRequest getAsJsonMap(final SuccessCallback<Response<Map>> callback) {
        return getAsJsonMap(callback, null);
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @param onError the error callback
     * @return returns the Connection Request object so it can be killed if necessary
     * @deprecated use {@link #fetchAsJsonMap(com.codename1.util.OnComplete)} instead
     */ 
    public ConnectionRequest getAsJsonMap(final SuccessCallback<Response<Map>> callback, final FailureCallback<? extends Object> onError) {
        ConnectionRequest request = createRequest(true);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                if(onError != null) {
                    // this is an error response code and should be handled as an error
                    if(evt.getResponseCode() > 310) {
                        return;
                    }
                }
                Response res = null;
                Map response = (Map)evt.getMetaData();
                res = new Response(evt.getResponseCode(), response, evt.getMessage());
                callback.onSucess(res);
            }
        });
        bindOnError(request, onError);
        CN.addToQueue(request);       
        return request;
    }
    
    private void bindOnError(final ConnectionRequest req, final FailureCallback<? extends Object> f) {
        if(f == null) {
            return;
        }
        req.addResponseCodeListener(new ActionListener<NetworkEvent>() {
            public void actionPerformed(NetworkEvent evt) {
                evt.consume();
                f.onError(null, evt.getError(), evt.getResponseCode(), evt.getMessage());
            }
        });
        req.addExceptionListener(new ActionListener<NetworkEvent>() {
            public void actionPerformed(NetworkEvent evt) {
                evt.consume();
                f.onError(null, evt.getError(), evt.getResponseCode(), evt.getMessage());
            }
        });
    }
    
    /**
     * Executes the request asynchronously and writes the response to the provided
     * Callback
     * @param callback writes the response to this callback
     * @deprecated use {@link #fetchAsJsonMap(com.codename1.util.OnComplete)} instead
     */ 
    public void getAsJsonMapAsync(final Callback<Response<Map>> callback) {
        getAsJsonMap(callback, callback);
    }
    
    /**
     * Executes the request synchronously
     * 
     * @return Response Object
     */ 
    public Response<Map> getAsJsonMap() {
        ConnectionRequest request = createRequest(true);
        CN.addToQueueAndWait(request);
        Map response = ((Connection)request).json;
        return new Response(request.getResponseCode(), response, request.getResponseErrorMessage());
    }

    /**
     * Executes the request synchronously
     * 
     * @param type the type of the business object to create
     * @return Response Object
     */ 
    public Response<PropertyBusinessObject> getAsProperties(Class type) {
        ConnectionRequest request = createRequest(true);
        CN.addToQueueAndWait(request);
        Map response = ((Connection)request).json;
        try {
            PropertyBusinessObject pb = (PropertyBusinessObject)type.newInstance();
            pb.getPropertyIndex().populateFromMap(response);
            return new Response(request.getResponseCode(), pb, request.getResponseErrorMessage());
        } catch(Exception err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }

    /**
     * Executes the request synchronously
     * 
     * @param type the type of the business object to create
     * @return Response Object
     */ 
    public Response<List<PropertyBusinessObject>> getAsPropertyList(Class type) {
        ConnectionRequest request = createRequest(true);
        CN.addToQueueAndWait(request);
        Map response = ((Connection)request).json;
        try {
            List<Map> lst = (List<Map>)response.get("root");
            List<PropertyBusinessObject> result = new ArrayList<PropertyBusinessObject>();
            for(Map m : lst) {
                PropertyBusinessObject pb = (PropertyBusinessObject)type.newInstance();
                pb.getPropertyIndex().populateFromMap(m);
                result.add(pb);
            }
            return new Response(request.getResponseCode(), result, request.getResponseErrorMessage());
        } catch(Exception err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }
    
    class Connection extends GZConnectionRequest {
        private boolean parseJSON;
        private boolean errorCode;
        Map json;
        private ErrorCodeHandler errorHandler;
        private Object errorObject;
        
        public Connection(boolean parseJSON) {
            this.parseJSON = parseJSON;
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            if(byteArrayErrorCallback != null || stringErrorCallback != null ||
                    jsonErrorCallback != null || propertyErrorCallback != null) {
                errorCode = true;
            } else {
                super.handleErrorResponseCode(code, message);
            }
        }   
        
        @Override
        protected void readUnzipedResponse(InputStream input) throws IOException {
            if(errorCode) {
                if(byteArrayErrorCallback != null) {
                    super.readUnzipedResponse(input);
                    errorObject = getResponseData();
                    errorHandler = byteArrayErrorCallback;
                    return;
                }
                if(stringErrorCallback != null) {
                    super.readUnzipedResponse(input);
                    errorObject = new String(getResponseData(), "UTF-8");
                    errorHandler = stringErrorCallback;
                    return;
                }
                if(jsonErrorCallback != null) {
                    JSONParser jp = new JSONParser();
                    errorObject = jp.parseJSON(new InputStreamReader(input, "UTF-8"));
                    errorHandler = jsonErrorCallback;
                    return;
                }
                if(propertyErrorCallback != null) {
                    try {
                        JSONParser jp = new JSONParser();
                        Map m = jp.parseJSON(new InputStreamReader(input, "UTF-8"));
                        PropertyBusinessObject pb = (PropertyBusinessObject)errorHandlerPropertyType.newInstance();
                        pb.getPropertyIndex().populateFromMap(m);
                        errorObject = pb;
                        errorHandler = propertyErrorCallback;
                    } catch(InstantiationException err) {
                        Log.e(err);
                        throw new IOException(err.toString());
                    } catch(IllegalAccessException err) {
                        Log.e(err);
                        throw new IOException(err.toString());
                    }
                }
                return;
            }
            if(parseJSON) {
                JSONParser parser = new JSONParser();
                json = parser.parseJSON(new InputStreamReader(input, "UTF-8"));
                if(hasResponseListeners() && !isKilled()) {
                    fireResponseListener(new NetworkEvent(this, json));
                }
                return;
            } 
            super.readUnzipedResponse(input);
        }

        @Override
        protected void postResponse() {
            if(errorHandler != null) {
                errorHandler.onError(new Response(getResponseCode(), errorObject, getResponseErrorMessage()));
            }
        }
        
    }
    
    private ConnectionRequest createRequest(boolean parseJson) {
        ConnectionRequest req = new Connection(parseJson);
        for (String key : pathParams.keySet()) {
            url = com.codename1.util.StringUtil.replaceAll(url, "{" + key + "}", pathParams.get(key));
        }       
        if(contentType != null) {
            req.setContentType(contentType);
        }
        req.setFailSilently(true);
        req.setReadResponseForErrors(true);
        req.setDuplicateSupported(true);
        req.setUrl(url);
        req.setHttpMethod(method);
        req.setPost(method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH"));
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
        if(errorCallback != null) {
            req.addExceptionListener(errorCallback);
        }

        return req;
    }
}
