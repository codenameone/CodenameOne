// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.network.CefRequest;

/**
 * In this case we create a new CefRequest object with
 * http://www.google.com/#q=<entered value without scheme search>
 * as target and forward it to the CefBrowser instance.
 * The "search://"-request is aborted by returning false.
 */
public class SearchSchemeHandler extends CefResourceHandlerAdapter {
    public static final String scheme = "search";
    public static final String domain = "";

    private final CefBrowser browser_;

    public SearchSchemeHandler(CefBrowser browser) {
        browser_ = browser;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        // cut away "scheme://"
        String requestUrl = request.getURL();
        String newUrl = requestUrl.substring(scheme.length() + 3);
        // cut away a trailing "/" if any
        if (newUrl.indexOf('/') == newUrl.length() - 1) {
            newUrl = newUrl.substring(0, newUrl.length() - 1);
        }
        newUrl = "http://www.google.com/#q=" + newUrl;

        CefRequest newRequest = CefRequest.create();
        if (newRequest != null) {
            newRequest.setMethod("GET");
            newRequest.setURL(newUrl);
            newRequest.setFirstPartyForCookies(newUrl);
            browser_.loadRequest(newRequest);
        }
        return false;
    }
}
