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

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.impl.backend.BackendAccess;

/// Calls a running backend's routes in the same process: no socket, no port, the
/// same answer. A request goes through what the listener would have done with it
/// -- the body decoded and refused by the same rules, then sessions, scoped beans,
/// CORS, compression, tracing and metrics -- on the calling thread.
///
/// A [BackendTest] injects one into an `@Autowired MockMvc` field. A test that
/// assembles its own server can make one with [#MockMvc(Backend)].
public final class MockMvc {
    private final Backend backend;

    /// A client for `backend`, which must be running.
    public MockMvc(Backend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("No backend");
        }
        this.backend = backend;
    }

    /// Sends one request and returns what came back, to assert on.
    public ResultActions perform(MockRequestBuilder request) throws Exception {
        // What the request was told with(...): applied now, and whatever a
        // post-processor changes for the length of the request -- who the thread
        // is acting as -- is undone when it has been sent, however it ends.
        MockRequestBuilder prepared = request.prepare();
        try {
            prepared.runBefore();
            return send(prepared);
        } finally {
            prepared.runAfter();
        }
    }

    private ResultActions send(MockRequestBuilder request) throws Exception {
        MockRequestBuilder.Built built = request.build();
        BackendAccess access = BackendAccess.get();
        // Never null: Backend.dispatch answers a path nothing routes with the same
        // 404 the HTTP/1 and HTTP/2 listeners send, so status().isNotFound() asserts
        // what a real request gets (GreetingApiTest.unknownRoutesAreNotFound in the
        // developer guide's demos covers it).
        HttpServer.Response response = access.dispatch(backend, built.method, built.target,
                built.headers, built.body);
        int status = access.status(response);
        // No body where the HTTP writers send none -- a HEAD, a 1xx, 204, 205 or
        // 304 -- so a test cannot assert on content no client ever receives.
        boolean bodiless = "HEAD".equalsIgnoreCase(built.method) || (status >= 100 && status < 200)
                || status == 204 || status == 205 || status == 304;
        byte[] body;
        if (bodiless) {
            // Released unread: a static file's response holds the file open, and
            // only reading it closed it, so each HEAD assertion leaked a descriptor.
            access.discard(response);
            body = new byte[0];
        } else {
            body = access.body(response);
        }
        MockResponse mock = new MockResponse(status, access.contentType(response),
                access.headers(response), body);
        return new ResultActions(new MvcResult(built.method, built.target, mock));
    }
}
