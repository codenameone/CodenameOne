/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
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