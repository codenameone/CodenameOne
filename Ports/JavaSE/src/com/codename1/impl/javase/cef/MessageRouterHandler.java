// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;


import com.codename1.ui.events.BrowserNavigationCallback;
import java.lang.ref.WeakReference;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class MessageRouterHandler extends CefMessageRouterHandlerAdapter {
    private BrowserNavigationCallback navigationCallback_;
    
    public MessageRouterHandler(BrowserNavigationCallback navigationCallback) {
        this.navigationCallback_ = navigationCallback;
    }
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
            boolean persistent, CefQueryCallback callback) {

        if (request.startsWith("shouldNavigate:")) {
            
            String url = request.substring(request.indexOf(":")+1);
            //BrowserNavigationCallback navigationCallback_ = navigationCallbackRef.get();
            if (navigationCallback_ != null) {
                boolean res = navigationCallback_.shouldNavigate(url);
                callback.success(""+res);
            }
            callback.success("true");
            return true;

        }
        // Not handled.
        return false;
    }
}
