/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.annotations.grpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds a [GrpcClient] interface method to a gRPC unary RPC. The
/// `value` is the gRPC method name (e.g. `SayHello`) and is combined
/// with the interface-level [GrpcClient#value()] to form
/// `/<service>/<method>`. The optional [#service()] overrides the
/// interface-level service path for a single method (useful when an
/// interface aggregates calls into multiple services).
///
/// Streaming RPCs are not supported in this release -- only unary
/// (single request, single response).
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Rpc {
    /// The gRPC method name as declared in the `.proto` `rpc` line,
    /// e.g. `SayHello`. Joined to the service path via `/`.
    String value();

    /// Optional override of the interface-level service path. Empty
    /// string means inherit from [GrpcClient#value()].
    String service() default "";
}
