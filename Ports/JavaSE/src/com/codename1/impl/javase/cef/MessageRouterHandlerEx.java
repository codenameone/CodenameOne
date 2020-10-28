// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import com.codename1.ui.BrowserComponent;
import java.lang.ref.WeakReference;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class MessageRouterHandlerEx extends CefMessageRouterHandlerAdapter {
    private WeakReference<BrowserComponent> browserComponentRef;
    private final CefClient client_;
    private final CefMessageRouterConfig config_ =
            new CefMessageRouterConfig("myQuery", "myQueryAbort");
    private CefMessageRouter router_ = null;

    public MessageRouterHandlerEx(final CefClient client, BrowserComponent browserComponent) {
        client_ = client;
        this.browserComponentRef = new WeakReference<BrowserComponent>(browserComponent);
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
            boolean persistent, CefQueryCallback callback) {
        if (request.startsWith("hasExtension")) {
            if (router_ != null)
                callback.success("");
            else
                callback.failure(0, "");
        } else if (request.startsWith("enableExt")) {
            if (router_ != null) {
                callback.failure(-1, "Already enabled");
            } else {
                router_ = CefMessageRouter.create(config_, new ShouldNavigateMessageRouter());
                client_.addMessageRouter(router_);
                callback.success("");
            }
        } else if (request.startsWith("disableExt")) {
            if (router_ == null) {
                callback.failure(-2, "Already disabled");
            } else {
                client_.removeMessageRouter(router_);
                router_.dispose();
                router_ = null;
                callback.success("");
            }
        } else {
            // not handled
            return false;
        }
        return true;
    }

    private class JavaVersionMessageRouter extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
                boolean persistent, CefQueryCallback callback) {
            if (request.startsWith("jcefJava")) {
                callback.success(System.getProperty("java.version"));
                return true;
            }
            return false;
        };
    }
    
    private class ShouldNavigateMessageRouter extends CefMessageRouterHandlerAdapter {

        @Override
        public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
            if (request.startsWith("shouldNavigate:")) {
                String url = request.substring(request.indexOf(":")+1);
                BrowserComponent browserComponent_ = browserComponentRef.get();
                if (browserComponent_ != null) {
                    browserComponent_.fireBrowserNavigationCallbacks(url);
                    callback.success("true");
                }
                return true;
                
            }
            return false;
        }
        
    }
    
    
}
