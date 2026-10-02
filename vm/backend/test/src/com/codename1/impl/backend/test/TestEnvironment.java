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
package com.codename1.impl.backend.test;

import com.codename1.backend.Backend;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.TestRestTemplate;
import com.codename1.impl.backend.BackendAccess;

/// A running test application: the server, its beans, and the clients a test
/// injects.
public final class TestEnvironment {
    private final TestContext context;
    private final Backend backend;
    private MockMvc mockMvc;
    private TestRestTemplate restTemplate;

    TestEnvironment(TestContext context, Backend backend) {
        this.context = context;
        this.backend = backend;
    }

    public TestContext context() {
        return context;
    }

    public Backend backend() {
        return backend;
    }

    /// The port the server listens on.
    public int port() {
        return backend.getServer().getPort();
    }

    /// The bean of this name from the running wiring, or null.
    public Object bean(String name) {
        Object app = BackendAccess.get().applicationOf(backend);
        if (app instanceof TestBeans) {
            return ((TestBeans) app).bean(name);
        }
        throw new IllegalStateException("The running application is not a test wiring");
    }

    public MockMvc mockMvc() {
        if (mockMvc == null) {
            mockMvc = new MockMvc(backend);
        }
        return mockMvc;
    }

    public TestRestTemplate restTemplate() {
        if (restTemplate == null) {
            restTemplate = new TestRestTemplate("http://127.0.0.1:" + port());
        }
        return restTemplate;
    }
}
