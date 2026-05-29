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

import java.util.HashMap;
import java.util.Map;

/// Runtime registry that wires `@GrpcClient`-annotated interfaces
/// to the build-time-generated implementations. The generated
/// `cn1app.GrpcClientBootstrap` calls [#register(Class, Factory)]
/// for every gRPC interface in the project; user code reaches them
/// via the `static of(String baseUrl)` factory that
/// `cn1:generate-grpc` puts on each interface, and that factory in
/// turn calls [#create(Class, String)] here.
///
/// Mirrors [com.codename1.io.rest.RestClients].
public final class GrpcClients {

    private static final Map<Class<?>, Factory<?>> REGISTRY = new HashMap<Class<?>, Factory<?>>();

    private GrpcClients() {
    }

    /// Registers a factory for a `@GrpcClient`-annotated interface.
    public static <T> void register(Class<T> apiType, Factory<T> factory) {
        if (apiType == null || factory == null) {
            return;
        }
        REGISTRY.put(apiType, factory);
    }

    /// Returns a freshly-built client for the requested API.
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> apiType, String baseUrl) {
        Factory<T> factory = (Factory<T>) REGISTRY.get(apiType);
        if (factory == null) {
            throw new IllegalStateException(
                    "No GrpcClient impl registered for " + apiType.getName()
                            + " -- did cn1:process-annotations run?");
        }
        return factory.create(baseUrl);
    }

    /// Factory the generated bootstrap registers per API interface.
    /// Single-method interface -- not `java.util.function.Function`
    /// -- so CLDC-targeted builds remain happy.
    public interface Factory<T> {
        T create(String baseUrl);
    }
}
