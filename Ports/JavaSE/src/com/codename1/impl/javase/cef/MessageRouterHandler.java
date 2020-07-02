// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import com.codename1.ui.BrowserComponent;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class MessageRouterHandler extends CefMessageRouterHandlerAdapter {
    private BrowserComponent browserComponent_;
    
    public MessageRouterHandler(BrowserComponent browserComponent) {
        this.browserComponent_ = browserComponent;
    }
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
            boolean persistent, CefQueryCallback callback) {

        if (request.startsWith("shouldNavigate:")) {
                
                String url = request.substring(request.indexOf(":")+1);
                System.out.println("In shouldNavigate callback with url: "+url);
                if (browserComponent_ != null) {
                    boolean res = browserComponent_.fireBrowserNavigationCallbacks(url);
                    callback.success(""+res);
                }
                callback.success("true");
                return true;
                
            }
        // Not handled.
        return false;
    }
}
