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
package com.codenameone.developerguide.backend.beans;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;

import java.util.Properties;

/** How a test starts the build's wiring, as a Spring Boot test starts a context. */
public final class WiringTest {
    private WiringTest() {
    }

    // `wiring` is `new BackendWiring()`: the class the build generates beside the
    // entry point, which a test in the backend module can name directly.
    static void run(Backend.Application wiring) throws Exception {
// tag::backend-wiring-test[]
Properties settings = new Properties();
settings.setProperty("cn1.server.port", "0");        // any free port
Backend server = Backend.builder(Config.of(settings, "test"))
        .quiet()
        .application(wiring)
        .start();
try {
    int port = server.getServer().getPort();
    // ... send requests to http://127.0.0.1:<port> and assert on the answers ...
} finally {
    server.stop();
}
// end::backend-wiring-test[]
    }
}
