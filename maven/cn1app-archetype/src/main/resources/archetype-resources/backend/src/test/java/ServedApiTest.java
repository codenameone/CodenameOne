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
package ${package};

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.HttpStatus;
import com.codename1.backend.test.TestRestTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The same server on a real port, reached through the backend's own HTTP client:
 * the request goes through the socket, the parser and the response writer that a
 * deployed server uses. The port is chosen at random, so a server already running
 * on 8080 does not get in the way.
 */
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class ServedApiTest {
    @Autowired
    private TestRestTemplate rest;

    @Test
    void answersOverTheNetwork() throws Exception {
        assertEquals("ok", rest.getForObject("/healthz", String.class));
        assertEquals("Hello, Ada", rest.getForObject("/greet/{name}", String.class, "Ada"));
        assertEquals(HttpStatus.NOT_FOUND, rest.getForEntity("/nowhere", String.class).getStatusCode());
    }
}
