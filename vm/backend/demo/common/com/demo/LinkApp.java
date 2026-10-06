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
package com.demo;

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.BackendApplication;
import com.codename1.impl.backend.Scheduler;
import com.codename1.impl.backend.WiringEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What the link-check servers share: an application with one handler,
/// standing in for the wiring a build generates, and a way to send it a request
/// in process.
///
/// Each of them -- LinkNone, LinkJwt, LinkForm and the rest -- is a whole server
/// that declares one kind of security, or none. They exist to be translated: the driver test
/// reads the symbols of each binary and requires the code of what a server did
/// NOT declare to be absent from it. This class names nothing of the security
/// layer, so the server with no chain carries none of it.
abstract class LinkApp implements BackendApplication {
    @Override
    public HttpServer.Handler[] create(WiringEnvironment environment) throws Exception {
        chains(environment);
        return new HttpServer.Handler[] {new HttpServer.Handler() {
            @Override
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "ok " + request.pathFrom(0));
            }
        }};
    }

    /// Registers the server's chains, if it has any.
    abstract void chains(WiringEnvironment environment) throws Exception;

    /// The status a request gets from `backend`, sent through its whole request
    /// path without a socket. `headers` are name and value pairs.
    static int status(Backend backend, String method, String target, String... headers)
            throws Exception {
        return BackendAccess.get().status(send(backend, method, target, null, headers));
    }

    static HttpServer.Response send(Backend backend, String method, String target, String form,
                                    String... headers) throws Exception {
        Map map = new HashMap();
        map.put("Host", "localhost");
        byte[] body = null;
        if (form != null) {
            body = form.getBytes("UTF-8");
            map.put("Content-Type", "application/x-www-form-urlencoded");
            map.put("Content-Length", String.valueOf(body.length));
        }
        for (int iter = 0 ; iter + 1 < headers.length ; iter += 2) {
            map.put(headers[iter], headers[iter + 1]);
        }
        return BackendAccess.get().dispatch(backend, method, target, map, body);
    }

    /// The first value of a response header, in any case, or null.
    static String header(HttpServer.Response response, String name) {
        for (Object header : BackendAccess.get().headers(response)) {
            String[] pair = (String[]) header;
            if (pair[0].equalsIgnoreCase(name)) {
                return pair[1];
            }
        }
        return null;
    }

    @Override
    public void registerWebSockets(HttpServer.WebSocketRegistry registry) {
    }

    @Override
    public void started(Backend backend) {
    }

    @Override
    public void stopping() {
    }

    @Override
    public void stopped() {
    }

    @Override
    public boolean tracksCurrentRequest() {
        return false;
    }

    @Override
    public void requestEnded(Object[] beans) {
    }

    @Override
    public void sessionEnded(Object[] beans) {
    }

    @Override
    public Scheduler getScheduler() {
        return null;
    }

    @Override
    public List describeBeans() {
        return new ArrayList();
    }

    @Override
    public List describeRoutes() {
        return new ArrayList();
    }
}
