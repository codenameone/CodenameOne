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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Header merging for upgrade responses, and the virtual-thread stack raise that
/// must happen before any server starts.
class HttpServerHeadersAndStackTest {
    @Test
    void namesDifferingOnlyByCaseMergeIntoTheFirstSpelling() {
        Map first = new LinkedHashMap();
        first.put("Set-Cookie", "a=1");
        Map second = new LinkedHashMap();
        second.put("set-cookie", "b=2");
        second.put("X-Other", "x");
        Map merged = HttpServer.mergeHeaders(first, second);
        assertEquals(2, merged.size());
        assertEquals(Arrays.asList("a=1", "b=2"), merged.get("Set-Cookie"));
        assertNull(merged.get("set-cookie"));
        assertEquals("x", merged.get("X-Other"));
    }

    @Test
    void listAndScalarValuesMerge() {
        Map first = new LinkedHashMap();
        first.put("Vary", Arrays.asList("Origin", "Accept"));
        Map second = new LinkedHashMap();
        second.put("VARY", "Cookie");
        Map merged = HttpServer.mergeHeaders(first, second);
        assertEquals(Arrays.asList("Origin", "Accept", "Cookie"), (List) merged.get("Vary"));
    }

    @Test
    void emptySideAnswersTheOther() {
        Map only = new LinkedHashMap();
        only.put("A", "1");
        assertSame(only, HttpServer.mergeHeaders(null, only));
        assertSame(only, HttpServer.mergeHeaders(only, new LinkedHashMap()));
        assertNull(HttpServer.mergeHeaders(null, null));
    }

    @Test
    void raisingTheStackAfterAServerStartedIsRefused() throws Exception {
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "ok");
            }
        });
        try {
            assertThrows(IllegalStateException.class,
                    () -> HttpServer.requireVirtualThreadStack(Integer.MAX_VALUE));
            // Asking for no more than is already there stays a no-op.
            HttpServer.requireVirtualThreadStack(1);
        } finally {
            server.stop(1000);
        }
        // The last live server stopped, so a raise is accepted again; the size
        // is put back with the smallest legal raise (it never lowers).
        HttpServer.requireVirtualThreadStack(256 * 1024);
    }

    @Test
    void aStartThatFailsToBindDoesNotLatch() throws Exception {
        HttpServer holder = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "ok");
            }
        });
        int taken = holder.getPort();
        holder.stop(1000);
        HttpServer blocker = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "ok");
            }
        });
        int busy = blocker.getPort();
        blocker.stop(1000);
        java.net.ServerSocket occupied = new java.net.ServerSocket(busy, 1,
                java.net.InetAddress.getByName("127.0.0.1"));
        try {
            assertThrows(java.io.IOException.class, () -> HttpServer.start("127.0.0.1", busy, 16, 1,
                    new HttpServer.Handler() {
                        public HttpServer.Response handle(HttpServer.Request request) {
                            return HttpServer.Response.text(200, "ok");
                        }
                    }));
            HttpServer.requireVirtualThreadStack(320 * 1024);
        } finally {
            occupied.close();
        }
        assertTrue(taken > 0);
    }
}
