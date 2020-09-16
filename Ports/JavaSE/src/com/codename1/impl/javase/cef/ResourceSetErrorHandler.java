package com.codename1.impl.javase.cef;

import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler.ErrorCode;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class ResourceSetErrorHandler extends CefResourceHandlerAdapter {
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        System.out.println("processRequest: " + request);
        callback.Continue();
        return true;
    }

    @Override
    public void getResponseHeaders(
            CefResponse response, IntRef response_length, StringRef redirectUrl) {
        response.setError(ErrorCode.ERR_NOT_IMPLEMENTED);
        System.out.println("getResponseHeaders: " + response);
    }
}