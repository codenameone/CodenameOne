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
package android.webkit;

import android.graphics.Bitmap;
import android.view.KeyEvent;

/// Receives a web view's navigation and loading events. Without one, a link
/// the user follows is handed to the system, as Android does: it opens in the
/// device's browser rather than in the view.
public class WebViewClient {

    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_HOST_LOOKUP = -2;
    public static final int ERROR_UNSUPPORTED_AUTH_SCHEME = -3;
    public static final int ERROR_AUTHENTICATION = -4;
    public static final int ERROR_PROXY_AUTHENTICATION = -5;
    public static final int ERROR_CONNECT = -6;
    public static final int ERROR_IO = -7;
    public static final int ERROR_TIMEOUT = -8;
    public static final int ERROR_REDIRECT_LOOP = -9;
    public static final int ERROR_UNSUPPORTED_SCHEME = -10;
    public static final int ERROR_FAILED_SSL_HANDSHAKE = -11;
    public static final int ERROR_BAD_URL = -12;
    public static final int ERROR_FILE = -13;
    public static final int ERROR_FILE_NOT_FOUND = -14;
    public static final int ERROR_TOO_MANY_REQUESTS = -15;
    public static final int ERROR_UNSAFE_RESOURCE = -16;

    public WebViewClient() {
    }

    /// Return true to keep the view from loading `url`.
    @Deprecated
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
    }

    public void onPageFinished(WebView view, String url) {
    }

    public void onLoadResource(WebView view, String url) {
    }

    public void onPageCommitVisible(WebView view, String url) {
    }

    /// Not consulted: resources load natively (see [WebResourceResponse]).
    @Deprecated
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return null;
    }

    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return shouldInterceptRequest(view, request.getUrl().toString());
    }

    @Deprecated
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    }

    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (request.isForMainFrame()) {
            onReceivedError(view, error.getErrorCode(), String.valueOf(error.getDescription()),
                    request.getUrl().toString());
        }
    }

    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
    }

    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
    }

    public void onScaleChanged(WebView view, float oldScale, float newScale) {
    }

    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        return false;
    }

    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
    }
}
