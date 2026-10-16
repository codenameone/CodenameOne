/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

/**
 * Reaches {@code ConnectionRequest}'s protected {@code handleException}/
 * {@code handleErrorResponseCode} from flutter-runtime's tests, which live in a
 * different Maven module (and so a different package) than {@code ConnectionRequest}
 * itself. These simulate the two failure paths that never produce a plain response --
 * a DNS/connect/TLS/timeout failure, and a non-2xx/non-redirect HTTP status -- without
 * a real network round trip, exactly as {@code NetworkManagerAutoDetectAPNTest} does
 * for the core module's own tests.
 */
public final class ConnectionRequestFailureTestAccess {

    private ConnectionRequestFailureTestAccess() {
    }

    /** Simulates a DNS/connect/TLS/timeout failure: {@code ConnectionRequest}'s own
     *  {@code handleException}, which an {@code addExceptionListener} intercepts. */
    public static void simulateException(ConnectionRequest req, Exception err) {
        req.handleException(err);
    }

    /** Simulates a non-2xx/non-redirect HTTP status: {@code ConnectionRequest}'s own
     *  {@code handleErrorResponseCode}, which an {@code addResponseCodeListener}
     *  intercepts. */
    public static void simulateErrorResponseCode(ConnectionRequest req, int code, String message) {
        req.handleErrorResponseCode(code, message);
    }
}
