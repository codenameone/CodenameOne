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
package com.codename1.io.graphql;

import java.util.HashMap;
import java.util.Map;

/// Runtime registry that wires `@GraphQLClient`-annotated interfaces
/// to the build-time-generated implementations. The generated
/// `cn1app.GraphQLClientBootstrap` calls [#register(Class, Factory)]
/// for every GraphQL interface in the project; user code reaches them
/// via the `static of(String endpoint)` factory that
/// `cn1:generate-graphql` puts on each interface, and that factory in
/// turn calls [#create(Class, String)] here.
///
/// Mirrors [com.codename1.io.rest.RestClients] and
/// [com.codename1.io.grpc.GrpcClients].
public final class GraphQLClients {

    private static final Map<Class<?>, Factory<?>> REGISTRY = new HashMap<Class<?>, Factory<?>>();

    private GraphQLClients() {
    }

    /// Registers a factory for a `@GraphQLClient`-annotated interface.
    public static <T> void register(Class<T> apiType, Factory<T> factory) {
        if (apiType == null || factory == null) {
            return;
        }
        REGISTRY.put(apiType, factory);
    }

    /// Returns a freshly-built client for the requested API bound to
    /// `endpoint` (the GraphQL HTTP endpoint URL).
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> apiType, String endpoint) {
        Factory<T> factory = (Factory<T>) REGISTRY.get(apiType);
        if (factory == null) {
            throw new IllegalStateException(
                    "No GraphQLClient impl registered for " + apiType.getName()
                            + " -- did cn1:process-annotations run?");
        }
        return factory.create(endpoint);
    }

    /// Factory the generated bootstrap registers per API interface.
    /// Single-method interface -- not `java.util.function.Function`
    /// -- so CLDC-targeted builds remain happy.
    public interface Factory<T> {
        T create(String endpoint);
    }
}
