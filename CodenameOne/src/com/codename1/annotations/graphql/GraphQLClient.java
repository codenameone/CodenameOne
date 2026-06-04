/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.annotations.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks an interface as a GraphQL client that the build-time
/// annotation processor wires up to a generated implementation. Each
/// abstract method carries one of [Query], [Mutation] or
/// [Subscription] holding the GraphQL operation document, zero or more
/// [Var]-annotated parameters supplying its variables, an optional
/// `@Header("Authorization") String` bearer token, and a trailing
/// callback:
///
/// - `OnComplete<GraphQLResponse<T>>` for [Query] / [Mutation];
/// - `GraphQLSubscription.Handler<T>` for [Subscription], in which case
///   the method returns a `GraphQLSubscription` handle.
///
/// The processor emits a `<SimpleName>Impl` class and registers it with
/// [com.codename1.io.graphql.GraphQLClients] so the interface's
/// `static T of(String endpoint)` factory can return an instance
/// without the project source referencing the impl directly. Mirrors
/// [com.codename1.annotations.rest.RestClient] and
/// [com.codename1.annotations.grpc.GrpcClient].
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GraphQLClient {
    /// Optional default GraphQL endpoint URL baked into the generated
    /// `of()` factory's documentation. The effective endpoint is the
    /// argument passed to `of(String endpoint)`; this value is purely
    /// informational and may be left empty.
    String value() default "";
}
