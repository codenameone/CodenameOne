/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.test;

import java.util.ArrayList;
import java.util.List;

/// What a [MockMvc] request came back with.
public final class MockResponse {
    private final int status;
    private final String contentType;
    private final List headers;
    private final byte[] body;

    MockResponse(int status, String contentType, List headers, byte[] body) {
        this.status = status;
        this.contentType = contentType;
        this.headers = headers;
        this.body = body == null ? new byte[0] : body;
    }

    public int getStatus() {
        return status;
    }

    /// The Content-Type, or null.
    public String getContentType() {
        String header = getHeader("Content-Type");
        return header != null ? header : contentType;
    }

    /// The body as UTF-8 text.
    public String getContentAsString() {
        try {
            return new String(body, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err.toString(), err);
        }
    }

    public byte[] getContentAsByteArray() {
        return body.clone();
    }

    /// The first value of a header, by a name in any case; or null.
    public String getHeader(String name) {
        List all = getHeaders(name);
        return all.isEmpty() ? null : (String) all.get(0);
    }

    /// Every value of a header, by a name in any case.
    public List getHeaders(String name) {
        List out = new ArrayList();
        for (Object entry : headers) {
            String[] h = (String[]) entry;
            if (h[0].equalsIgnoreCase(name)) {
                out.add(h[1]);
            }
        }
        return out;
    }

    /// The names of the headers set, in order.
    public List getHeaderNames() {
        List out = new ArrayList();
        for (Object entry : headers) {
            String name = ((String[]) entry)[0];
            if (!out.contains(name)) {
                out.add(name);
            }
        }
        return out;
    }

    /// The value of a cookie this response sets, or null.
    public String getCookie(String name) {
        List all = getHeaders("Set-Cookie");
        for (Object entry : all) {
            String cookie = (String) entry;
            int eq = cookie.indexOf('=');
            if (eq > 0 && cookie.substring(0, eq).trim().equals(name)) {
                int semi = cookie.indexOf(';', eq);
                return cookie.substring(eq + 1, semi < 0 ? cookie.length() : semi).trim();
            }
        }
        return null;
    }

    /// The Location of a redirect, or null.
    public String getRedirectedUrl() {
        return getHeader("Location");
    }
}
