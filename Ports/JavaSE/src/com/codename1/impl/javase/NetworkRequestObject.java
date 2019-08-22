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
package com.codename1.impl.javase;

/**
 * Simple data object for the network monitor
 *
 * @author Shai Almog
 */
public class NetworkRequestObject {

    

    private String url;
    private String method = "POST";
    private String headers;
    private String responseHeaders;
    private String requestBody;
    private String responseBody;
    private String responseCode;
    private String contentLength;
    private long timeQueued, timeSent, timeServerResponse, timeComplete;
    

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the headers
     */
    public String getHeaders() {
        return headers;
    }

    /**
     * @param headers the headers to set
     */
    public void setHeaders(String headers) {
        this.headers = headers;
    }

    /**
     * @return the responseHeaders
     */
    public String getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * @param responseHeaders the responseHeaders to set
     */
    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * @return the requestBody
     */
    public String getRequestBody() {
        return requestBody;
    }

    /**
     * @param requestBody the requestBody to set
     */
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * @return the responseBody
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * @param responseBody the responseBody to set
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * @return the responseCode
     */
    public String getResponseCode() {
        return responseCode;
    }

    /**
     * @param responseCode the responseCode to set
     */
    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * @return the contentLength
     */
    public String getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    public void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }
    
    
    /**
     * The timestamp of when the request is sent.
     * @return the timeSent
     * @since 7.0
     */
    public long getTimeSent() {
        return timeSent;
    }

    /**
     * The timestamp of when the request is sent.
     * @param timeSent the timeSent to set
     * @since 7.0
     */
    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }

    /**
     * The timestamp of when the server response is received.
     * @return the timeServerResponse
     * @since 7.0
     */
    public long getTimeServerResponse() {
        return timeServerResponse;
    }

    /**
     * The timestamp of when the server response is received.
     * @param timeServerResponse the timeServerResponse to set
     * @since 7.0
     */
    public void setTimeServerResponse(long timeServerResponse) {
        this.timeServerResponse = timeServerResponse;
    }
    
    /**
     * The timestamp of when the request is complete (including download).
     * @return the timeComplete
     * @since 7.0
     */
    public long getTimeComplete() {
        return timeComplete;
    }

    /**
     * The timestamp of when the request is complete (including download).
     * @param timeComplete the timeComplete to set
     * @since 7.0
     */
    public void setTimeComplete(long timeComplete) {
        this.timeComplete = timeComplete;
    }
    
    /**
     * @return the timeQueued
     */
    public long getTimeQueued() {
        return timeQueued;
    }

    /**
     * @param timeQueued the timeQueued to set
     */
    public void setTimeQueued(long timeQueued) {
        this.timeQueued = timeQueued;
    }

    
    
    public long getWaitTime() {
        if (getTimeSent() <= 0) {
            return -1;
        }
        if (getTimeServerResponse() <= 0) {
            return System.currentTimeMillis() - getTimeSent();
        }
        return getTimeServerResponse() - getTimeSent();
    }
    
    public long getTotalTime() {
        if (getTimeSent() <= 0) {
            return -1;
        }
        if (getTimeComplete() <= 0) {
            return System.currentTimeMillis() - getTimeSent();
        }
        return getTimeComplete() - getTimeSent();
    }
    
    public long getDownloadTime() {
        if (getTimeServerResponse() <= 0) {
            return -1;
        }
        if (getTimeComplete() <= 0) {
            return System.currentTimeMillis() - getTimeServerResponse();
        }
        return getTimeComplete() - getTimeServerResponse();
    }

    public long getQueuedTime() {
        if (getTimeQueued() <= 0) {
            return -1;
        }
        if (getTimeSent() <= 0) {
            return System.currentTimeMillis() - getTimeQueued();
        }
        return getTimeSent() - getTimeQueued();
    }
}
