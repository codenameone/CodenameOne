/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.rpc;

import java.io.IOException;

/// A failure the remote end reported: an HTTP status outside 2xx, or a JSON-RPC error object.
/// [#getCode] is the HTTP status for the first and the JSON-RPC error code for the second;
/// [#isHttp] says which.
@com.codename1.impl.SharedWithBackend
public class RpcException extends IOException {
    private final int code;
    private final boolean http;
    private final Object data;

    /// An error with a code, a message and the optional error data.
    public RpcException(int code, String message, Object data, boolean http) {
        super(message);
        this.code = code;
        this.data = data;
        this.http = http;
    }

    /// An HTTP failure.
    public static RpcException http(int status, String body) {
        return new RpcException(status, "HTTP " + status + (body == null || body.length() == 0 ? "" : ": "
                + (body.length() > 300 ? body.substring(0, 300) : body)), body, true);
    }

    /// The HTTP status or the JSON-RPC error code.
    public int getCode() {
        return code;
    }

    /// Whether the code is an HTTP status.
    public boolean isHttp() {
        return http;
    }

    /// The JSON-RPC error's `data` member, or the HTTP body.
    public Object getData() {
        return data;
    }
}
