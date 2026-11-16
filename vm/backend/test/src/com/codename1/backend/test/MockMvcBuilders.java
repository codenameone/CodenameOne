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

import com.codename1.backend.HttpServer;
import com.codename1.impl.backend.test.StandaloneRouters;

/// Builds a [MockMvc] without an application: `standaloneSetup` routes
/// requests straight to the given controller instances, which the test
/// constructs itself -- with mocks, usually. Nothing else of the application
/// runs: no filters, no security, no interceptors and no `@ControllerAdvice`,
/// and no server is started, so it needs no `@BackendTest` and works beside
/// one.
public final class MockMvcBuilders {
    private MockMvcBuilders() {
    }

    /// A builder over `controllers`, tried in order for each request.
    public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
        if (controllers == null || controllers.length == 0) {
            throw new IllegalArgumentException("standaloneSetup needs at least one controller");
        }
        HttpServer.Handler[] routers = new HttpServer.Handler[controllers.length];
        for (int i = 0; i < controllers.length; i++) {
            routers[i] = StandaloneRouters.of(controllers[i]);
        }
        return new StandaloneMockMvcBuilder(routers);
    }

    /// What [#standaloneSetup] returns.
    public static final class StandaloneMockMvcBuilder {
        private final HttpServer.Handler[] routers;

        StandaloneMockMvcBuilder(HttpServer.Handler[] routers) {
            this.routers = routers;
        }

        /// The client.
        public MockMvc build() {
            return new MockMvc(routers);
        }
    }
}
