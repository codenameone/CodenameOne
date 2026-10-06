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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;
import java.util.LinkedHashMap;
import java.util.Map;

/// The few answers the layer writes itself.
final class Responses {
    private Responses() {
    }

    /// A 302 to `location`, which is a path on this server. Sent as written --
    /// relative -- so the client resolves it against the address it used, and
    /// nothing here is built from a Host header the client chose.
    static HttpServer.Response redirect(String location) {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("Location", location);
        return new HttpServer.Response(302, "text/plain; charset=utf-8", new byte[0], headers);
    }

    static HttpServer.Response status(int status, String text) {
        return HttpServer.Response.text(status, text);
    }

    static HttpServer.Response html(int status, String body) {
        return new HttpServer.Response(status, "text/html; charset=utf-8", utf8(body));
    }

    static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    /// `value` safe to put in HTML text or a quoted attribute.
    static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /// A request parameter, from the query or a form body; null when it is not
    /// there or the body cannot be read as a form.
    static String param(HttpServer.Request request, String name) {
        try {
            return request.param(name);
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
