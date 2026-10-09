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
package com.codename1.backend.mvc;

import com.codename1.backend.HttpServer;

/** Helpers for the htmx wire protocol. */
public final class Htmx {
    private Htmx() {}

    public static boolean isRequest(HttpServer.Request request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"))
                && !"true".equalsIgnoreCase(request.getHeader("HX-History-Restore-Request"));
    }

    public static HttpServer.Response redirect(String location) {
        Html.localLocation(location);
        return HttpServer.Response.text(200, "").header("HX-Redirect", location);
    }

    public static HttpServer.Response refresh() {
        return HttpServer.Response.text(200, "").header("HX-Refresh", "true");
    }

    private static boolean letter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static HttpServer.Response trigger(HttpServer.Response response, String event) {
        if (event == null || event.length() == 0 || !letter(event.charAt(0)))
            throw new IllegalArgumentException("Invalid htmx event name");
        for (int i = 1; i < event.length(); i++) {
            char c = event.charAt(i);
            if (!letter(c) && !(c >= '0' && c <= '9') && "_:.-".indexOf(c) < 0)
                throw new IllegalArgumentException("Invalid htmx event name");
        }
        return response.header("HX-Trigger", event);
    }
}
