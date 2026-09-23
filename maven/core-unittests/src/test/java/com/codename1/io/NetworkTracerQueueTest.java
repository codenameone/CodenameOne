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
package com.codename1.io;

import com.codename1.junit.UITestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// What `NetworkManager.addToQueue` records for the tracer, checked on a private
/// manager whose queue nothing consumes. Driving the shared manager through a busy
/// network thread instead was not deterministic: other test classes leave extra
/// network threads consuming the shared queue, so "still pending" could not be
/// arranged reliably.
class NetworkTracerQueueTest extends UITestBase {

    @AfterEach
    void removeTracer() {
        NetworkManager.setNetworkTracer(null);
    }

    @Test
    void aRejectedDuplicateEnqueueKeepsTheQueuedRequestsParent() throws Exception {
        NetworkManager manager = idleManager();
        final int[] calls = new int[1];
        NetworkTracer tracer = new NetworkTracer() {
            @Override
            public Object requestQueued(ConnectionRequest request) {
                calls[0]++;
                return calls[0] == 1 ? "first action" : "second action";
            }

            @Override
            public Object beforeRequest(ConnectionRequest request, Object parent) {
                return null;
            }

            @Override
            public void afterRequest(ConnectionRequest request, Object attempt, int status,
                                     Throwable error) {
            }
        };
        NetworkManager.setNetworkTracer(tracer);
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl("http://queue.test/");
        request.setDuplicateSupported(false);

        manager.addToQueue(request, false);
        assertEquals("first action", request.tracerParent);
        assertSame(tracer, request.tracerParentOwner);

        // The same request again while it is pending: rejected as a duplicate,
        // and it must keep the context of the enqueue that was accepted.
        manager.addToQueue(request, false);
        assertEquals(2, calls[0], "the second enqueue did ask the tracer");
        assertEquals("first action", request.tracerParent,
                "a rejected duplicate re-parented the queued request");
    }

    @Test
    void aRetryKeepsItsContextWithoutAskingAgain() throws Exception {
        NetworkManager manager = idleManager();
        final int[] calls = new int[1];
        NetworkManager.setNetworkTracer(new NetworkTracer() {
            @Override
            public Object requestQueued(ConnectionRequest request) {
                calls[0]++;
                return "action";
            }

            @Override
            public Object beforeRequest(ConnectionRequest request, Object parent) {
                return null;
            }

            @Override
            public void afterRequest(ConnectionRequest request, Object attempt, int status,
                                     Throwable error) {
            }
        });
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl("http://queue.test/retry");
        manager.addToQueue(request, false);
        manager.addToQueue(request, true);
        assertEquals(1, calls[0]);
        assertEquals("action", request.tracerParent);
    }

    @Test
    void addIfAbsentLeavesAnExplicitContentTypeAlone() {
        ConnectionRequest request = new ConnectionRequest();
        request.setContentType("application/json");
        assertEquals(false, request.addRequestHeaderIfAbsent("Content-Type", "text/plain"));
        assertEquals("application/json", request.getContentType());
        assertEquals("application/json", request.getRequestHeader("content-type"));

        ConnectionRequest plain = new ConnectionRequest();
        assertEquals(null, plain.getRequestHeader("Content-Type"),
                "the default content type is not a header anyone added");
        assertEquals(true, plain.addRequestHeaderIfAbsent("X-Trace", "1"));
        assertEquals("1", plain.getRequestHeader("x-trace"));
    }

    /// A manager that queues and never runs anything: marked running with one
    /// network thread that was never started.
    private static NetworkManager idleManager() throws Exception {
        Constructor<NetworkManager> constructor = NetworkManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        NetworkManager manager = constructor.newInstance();
        set(manager, "running", Boolean.TRUE);
        set(manager, "autoDetected", Boolean.TRUE);
        set(manager, "networkThreads", new NetworkManager.NetworkThread[] {manager.new NetworkThread()});
        return manager;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = NetworkManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
