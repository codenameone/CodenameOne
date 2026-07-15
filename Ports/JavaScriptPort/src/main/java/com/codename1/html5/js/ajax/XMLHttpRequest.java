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
package com.codename1.html5.js.ajax;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.typedarrays.ArrayBuffer;

/**
 * XMLHttpRequest interface for AJAX.
 * https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest
 */
public interface XMLHttpRequest extends JSObject {
    static XMLHttpRequest create() {
        return null; // Native implementation
    }
    
    int getReadyState();
    int getStatus();
    String getStatusText();
    String getResponseText();
    ArrayBuffer getResponseArrayBuffer();
    Object getResponse();
    String getResponseURL();
    void open(String method, String url);
    void open(String method, String url, boolean async);
    void setRequestHeader(String name, String value);
    void send();
    void send(String data);
    void send(ArrayBuffer data);
    void abort();
    void setResponseType(String type);
    String getResponseType();
    void overrideMimeType(String mimeType);
    int getTimeout();
    void setTimeout(int timeout);
    Object getUpload();
    void setOnReadyStateChange(ReadyStateChangeHandler handler);
    ReadyStateChangeHandler getOnReadyStateChange();
    void setOnLoad(Object handler);
    void setOnError(Object handler);
    void setOnProgress(Object handler);
    String getAllResponseHeaders();
    String getResponseHeader(String name);
}