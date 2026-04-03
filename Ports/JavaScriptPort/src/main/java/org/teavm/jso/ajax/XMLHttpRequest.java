/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.ajax;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;

public interface XMLHttpRequest extends JSObject {
    int UNSENT = 0;
    int OPENED = 1;
    int HEADERS_RECEIVED = 2;
    int LOADING = 3;
    int DONE = 4;
    
    int getReadyState();
    int getStatus();
    String getStatusText();
    String getResponseText();
    JSObject getResponse();
    Object getResponse();
    
    @JSBody(params = {}, script = "return new XMLHttpRequest()")
    static XMLHttpRequest create() { return null; }
    
    void open(String method, String url);
    void open(String method, String url, boolean async);
    void open(String method, String url, boolean async, String user);
    void open(String method, String url, boolean async, String user, String password);
    void send();
    void send(JSObject body);
    void send(String body);
    void abort();
    void setRequestHeader(String header, String value);
    String getResponseHeader(String header);
    String getAllResponseHeaders();
    void overrideMimeType(String mimeType);
    
    @JSProperty void setOnReadyStateChange(ReadyStateChangeHandler handler);
    @JSProperty void setOnLoad(ReadyStateChangeHandler handler);
    @JSProperty void setOnError(ReadyStateChangeHandler handler);
    @JSProperty void setOnProgress(ProgressHandler handler);
}

@JSFunctor
public interface ReadyStateChangeHandler extends JSObject {
    void stateChanged();
}

@JSFunctor
public interface ProgressHandler extends JSObject {
    void onProgress(long loaded, long total);
}