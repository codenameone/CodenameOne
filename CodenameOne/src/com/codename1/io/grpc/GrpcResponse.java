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

/// Result of a unary gRPC call. Mirrors the shape of
/// [com.codename1.io.rest.Response] so call sites that already use
/// the REST helpers feel at home, but the integer status code
/// returned by [#getResponseCode()] is the **gRPC** status, not
/// the HTTP one:
///
/// - `0` -- `OK`. Wire-level success.
/// - `1..16` -- standard gRPC statuses (CANCELLED, UNKNOWN, ...).
/// - `-1` -- the transport itself failed (network error, HTTP
///   non-200, missing status trailer). [#getHttpCode()] still
///   returns the underlying HTTP code in that case.
///
/// Use [#isOk()] for the common "request succeeded" check.
public final class GrpcResponse<T> {

    public static final int STATUS_OK = 0;
    public static final int STATUS_CANCELLED = 1;
    public static final int STATUS_UNKNOWN = 2;
    public static final int STATUS_INVALID_ARGUMENT = 3;
    public static final int STATUS_DEADLINE_EXCEEDED = 4;
    public static final int STATUS_NOT_FOUND = 5;
    public static final int STATUS_ALREADY_EXISTS = 6;
    public static final int STATUS_PERMISSION_DENIED = 7;
    public static final int STATUS_RESOURCE_EXHAUSTED = 8;
    public static final int STATUS_FAILED_PRECONDITION = 9;
    public static final int STATUS_ABORTED = 10;
    public static final int STATUS_OUT_OF_RANGE = 11;
    public static final int STATUS_UNIMPLEMENTED = 12;
    public static final int STATUS_INTERNAL = 13;
    public static final int STATUS_UNAVAILABLE = 14;
    public static final int STATUS_DATA_LOSS = 15;
    public static final int STATUS_UNAUTHENTICATED = 16;

    /// Sentinel status indicating the transport itself failed (no
    /// `grpc-status` trailer was parsed). Inspect [#getHttpCode()]
    /// for the underlying HTTP error.
    public static final int STATUS_TRANSPORT_FAILURE = -1;

    private final int grpcStatus;
    private final int httpCode;
    private final T responseData;
    private final String responseMessage;

    public GrpcResponse(int grpcStatus, int httpCode, T responseData, String responseMessage) {
        this.grpcStatus = grpcStatus;
        this.httpCode = httpCode;
        this.responseData = responseData;
        this.responseMessage = responseMessage;
    }

    /// The deserialised response message, or `null` when the call
    /// failed or the server returned an empty body.
    public T getResponseData() {
        return responseData;
    }

    /// The gRPC status code. `0` is success; non-zero codes follow
    /// the standard gRPC status enumeration. `-1` ([#STATUS_TRANSPORT_FAILURE])
    /// signals a transport-level error -- the call never reached
    /// `grpc-status`.
    public int getResponseCode() {
        return grpcStatus;
    }

    /// The underlying HTTP status code. Usually `200` even for a
    /// non-zero gRPC status, because the server returns gRPC errors
    /// in trailers rather than via HTTP status. Useful when
    /// [#getResponseCode()] is [#STATUS_TRANSPORT_FAILURE].
    public int getHttpCode() {
        return httpCode;
    }

    /// `true` iff [#getResponseCode()] is `0` (gRPC `OK`).
    public boolean isOk() {
        return grpcStatus == STATUS_OK;
    }

    /// The server-supplied `grpc-message` trailer (or the HTTP error
    /// message when the call failed before reaching the trailer).
    public String getResponseErrorMessage() {
        return responseMessage;
    }
}
