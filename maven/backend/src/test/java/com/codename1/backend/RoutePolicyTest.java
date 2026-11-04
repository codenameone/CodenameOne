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
package com.codename1.backend;

import com.codename1.backend.web.MediaTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The policy a `@CrossOrigin` route builds, and the media-type matching behind
/// `consumes`/`produces`.
class RoutePolicyTest {
    private static HttpServer.Request request(String method, String... headers) throws Exception {
        StringBuilder text = new StringBuilder(method + " /x HTTP/1.1\r\n");
        int[] slices = new int[headers.length * 4];
        for (int i = 0; i < headers.length; i++) {
            int colon = headers[i].indexOf(':');
            String name = headers[i].substring(0, colon);
            String value = headers[i].substring(colon + 1).trim();
            slices[i * 4] = text.length();
            slices[i * 4 + 1] = name.length();
            text.append(name).append(": ");
            slices[i * 4 + 2] = text.length();
            slices[i * 4 + 3] = value.length();
            text.append(value).append("\r\n");
        }
        byte[] raw = text.append("\r\n").toString().getBytes("UTF-8");
        return new HttpServer.Request(method, "/x", "HTTP/1.1", raw, slices, headers.length, null);
    }

    @Test
    void aRoutePolicyAdmitsOnlyItsOrigins() throws Exception {
        Cors policy = Cors.policy(new String[] {"https://app.example.com"}, new String[0],
                new String[] {"*"}, new String[] {"X-Total"}, true, 600);
        assertNull(policy.reject(request("GET", "Origin: https://app.example.com", "Host: api.example.com")));
        HttpServer.Response denied = policy.reject(request("GET", "Origin: https://evil.example", "Host: api.example.com"));
        assertNotNull(denied);
        assertEquals(403, denied.getStatus());
        HttpServer.Response preflight = policy.preflight(request("OPTIONS", "Origin: https://app.example.com",
                "Host: api.example.com", "Access-Control-Request-Method: DELETE"));
        assertEquals(204, preflight.getStatus());
        assertTrue(preflight.hasHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void credentialsWithAnyOriginIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Cors.policy(new String[] {"*"}, new String[0],
                new String[] {"*"}, new String[0], true, 600));
        assertThrows(IllegalArgumentException.class, () -> Cors.policy(new String[] {"https://a.example/path"},
                new String[0], new String[] {"*"}, new String[0], false, 600));
    }

    @Test
    void mediaTypesMatchWithWildcardsAndParameters() {
        assertTrue(MediaTypes.consumable("application/json; charset=UTF-8", "application/json"));
        assertTrue(MediaTypes.consumable("text/csv", "application/json,text/*"));
        assertFalse(MediaTypes.consumable(null, "application/json"));
        assertFalse(MediaTypes.consumable("*/*", "application/json"));
        assertFalse(MediaTypes.consumable("application/*", "application/json"));
        assertFalse(MediaTypes.consumable("*", "application/json"));
        assertFalse(MediaTypes.consumable("application/jsonx", "application/json"));
        assertTrue(MediaTypes.acceptable(null, "application/json"));
        assertTrue(MediaTypes.acceptable("*/*", "application/json"));
        assertFalse(MediaTypes.acceptable("application/json;q=0", "application/json"));
        assertFalse(MediaTypes.acceptable("image/png", "application/json"));
    }
}
