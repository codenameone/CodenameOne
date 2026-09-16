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
package com.codenameone.developerguide.backend;

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;

/** The Backend chapter's server examples, compiled so they cannot drift. */
public final class ServerSnippets {

    private ServerSnippets() {
    }

    public static void serve() throws Exception {
// tag::backend-builder[]
Backend.builder()
        .port(9000)                       // otherwise cn1.server.port, or PORT, or 8080
        .handler(new Health())
        .run();
// end::backend-builder[]
    }

    /** A handler with nothing injected into it, for the example above. */
    public static final class Health implements HttpServer.Handler {
        public HttpServer.Response handle(HttpServer.Request request) throws Exception {
            if (!"/healthz".equals(request.getTarget())) {
                return null;              // null means "not mine", and then a 404
            }
            return new HttpServer.Response(200, "text/plain", "ok".getBytes("UTF-8"));
        }
    }
}
