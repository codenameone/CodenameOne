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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.HttpStatus;
import com.codename1.backend.test.LocalServerPort;
import com.codename1.backend.test.ResponseEntity;
import com.codename1.backend.test.TestRestTemplate;
import com.codename1.backend.annotations.Autowired;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The server on a real port, through the backend's own HTTP client: what a
/// compiled run sends from the native binary.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class ServedTest {
    @Autowired
    private TestRestTemplate rest;

    @LocalServerPort
    private int port;

    @Test
    void servesOnARandomPort() throws Exception {
        assertTrue(port > 0 && port != 8765, "port " + port);
        assertEquals("Hello, Ada", rest.getForObject("/api/hello/{name}", String.class, "Ada"));
        ResponseEntity<String> missing = rest.getForEntity("/api/pets/999", String.class);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
    }

    @Test
    void postsJsonAndReadsItBack() throws Exception {
        Map<String, Object> pet = new LinkedHashMap<String, Object>();
        pet.put("name", "Tom");
        pet.put("species", "cat");
        pet.put("age", Long.valueOf(2));
        ResponseEntity<Map> created = rest.postForEntity("/api/pets", pet, Map.class);
        assertEquals(201, created.getStatusCodeValue());
        assertEquals("Tom", created.getBody().get("name"));
        List all = rest.getForObject("/api/pets", List.class);
        assertTrue(all.size() >= 1, String.valueOf(all));
    }

    @Test
    void headersAndBasicAuth() throws Exception {
        ResponseEntity<String> answer = rest.getForEntity("/api/headers", String.class);
        assertEquals("one", answer.getHeaders().getFirst("X-Probe"));
        assertEquals(401, rest.getForEntity("/api/auth/basic", String.class).getStatusCodeValue());
        assertEquals("user", rest.withBasicAuth("user", "pass")
                .getForObject("/api/auth/basic", String.class));
    }
}
