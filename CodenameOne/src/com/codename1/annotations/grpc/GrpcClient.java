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

/// Marks an interface as a gRPC client that the build-time annotation
/// processor wires up to a generated gRPC-Web implementation. Each
/// abstract method must carry an [Rpc] annotation naming the gRPC
/// method, and at most one non-callback parameter -- the request
/// message, a `@ProtoMessage`-annotated POJO. The final parameter is
/// an `OnComplete<Response<ResponseMessage>>` callback.
///
/// The fully qualified service path defaults to
/// `<value()>/<methodName>` -- for example
/// `helloworld.Greeter/SayHello`. Override per-method via
/// [Rpc#service()] when the service path needs to differ from the
/// interface-level default.
///
/// The processor emits a `<SimpleName>Impl` class in generated-sources
/// and registers it with [com.codename1.io.grpc.GrpcClients] so the
/// interface's `static T of(String baseUrl)` factory can return an
/// instance without the project source referencing the impl directly.
/// Mirrors [com.codename1.annotations.rest.RestClient].
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GrpcClient {
    /// Fully qualified gRPC service path (without leading slash), e.g.
    /// `helloworld.Greeter`. Combined with each method's [Rpc#value()]
    /// to form the request URI segment `/<service>/<method>` appended
    /// to the `baseUrl`. Empty string means each method must specify
    /// the full service path via [Rpc#service()].
    String value() default "";
}
