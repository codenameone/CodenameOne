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
package com.codename1.call;

/// Thrown through the failure path of every `AsyncResource` returned by the
/// `com.codename1.call` APIs, and passed to the failure callbacks of the
/// session and directory listeners. [#getError()] returns a typed
/// [CallError] so callers react without string-matching the message.
public class CallException extends Exception {

    private final CallError error;

    public CallException(CallError error) {
        super(error == null ? "UNKNOWN" : error.name());
        this.error = error == null ? CallError.UNKNOWN : error;
    }

    public CallException(CallError error, String message) {
        super(message);
        this.error = error == null ? CallError.UNKNOWN : error;
    }

    public CallException(CallError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error == null ? CallError.UNKNOWN : error;
    }

    /// Typed error code describing the failure. Never `null`.
    public CallError getError() {
        return error;
    }
}
