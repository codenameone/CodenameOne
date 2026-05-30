/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.io.grpc;

/// Thrown when a synchronous helper observes a non-OK gRPC status
/// or a transport-level failure. The async callback path uses
/// [GrpcResponse] instead -- this is only for code paths that
/// prefer exceptions over inspecting `responseCode`.
public class GrpcException extends RuntimeException {

    private final int status;
    private final int httpCode;

    public GrpcException(int status, int httpCode, String message) {
        super(message == null ? ("gRPC status " + status) : message);
        this.status = status;
        this.httpCode = httpCode;
    }

    public int getStatus() {
        return status;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
