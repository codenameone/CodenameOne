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

import java.io.InputStream;
import java.util.Map;

/// A response an application supplies in place of a network load. The web
/// views Codename One hosts load resources natively, so
/// [WebViewClient#shouldInterceptRequest(WebView, WebResourceRequest)] is
/// never consulted; the class exists so code that builds responses compiles.
public class WebResourceResponse {
    private String mMimeType;
    private String mEncoding;
    private int mStatusCode = 200;
    private String mReasonPhrase = "OK";
    private Map<String, String> mResponseHeaders;
    private InputStream mInputStream;

    public WebResourceResponse(String mimeType, String encoding, InputStream data) {
        mMimeType = mimeType;
        mEncoding = encoding;
        mInputStream = data;
    }

    public WebResourceResponse(String mimeType, String encoding, int statusCode, String reasonPhrase,
                               Map<String, String> responseHeaders, InputStream data) {
        this(mimeType, encoding, data);
        setStatusCodeAndReasonPhrase(statusCode, reasonPhrase);
        mResponseHeaders = responseHeaders;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setEncoding(String encoding) {
        mEncoding = encoding;
    }

    public String getEncoding() {
        return mEncoding;
    }

    public void setStatusCodeAndReasonPhrase(int statusCode, String reasonPhrase) {
        mStatusCode = statusCode;
        mReasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getReasonPhrase() {
        return mReasonPhrase;
    }

    public void setResponseHeaders(Map<String, String> headers) {
        mResponseHeaders = headers;
    }

    public Map<String, String> getResponseHeaders() {
        return mResponseHeaders;
    }

    public void setData(InputStream data) {
        mInputStream = data;
    }

    public InputStream getData() {
        return mInputStream;
    }
}
