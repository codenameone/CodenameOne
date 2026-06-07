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

/// Runtime registry that wires `@ProtoMessage` classes to their
/// build-time-generated [ProtoCodec]s. The generated
/// `cn1app.ProtoBootstrap` calls [#register(Class, ProtoCodec)] for
/// every protobuf message in the project; generated gRPC client
/// impls and per-class codecs call [#lookup(Class)] to resolve
/// nested-message types at runtime.
///
/// Mirrors [com.codename1.io.rest.RestClients].
public final class ProtoCodecs {

    private static final Map<Class<?>, ProtoCodec<?>> REGISTRY = new HashMap<Class<?>, ProtoCodec<?>>();

    private ProtoCodecs() {
    }

    /// Registers a codec for a `@ProtoMessage` class. Called from
    /// the generated `cn1app.ProtoBootstrap`.
    public static <T> void register(Class<T> type, ProtoCodec<T> codec) {
        if (type == null || codec == null) {
            return;
        }
        REGISTRY.put(type, codec);
    }

    /// Returns the codec previously registered for `type`. Throws
    /// [IllegalStateException] when no codec is registered so the
    /// failure is loud rather than silent (typical cause: the
    /// generated `cn1app.ProtoBootstrap` hasn't run, or `type` isn't
    /// annotated `@ProtoMessage`).
    @SuppressWarnings("unchecked")
    public static <T> ProtoCodec<T> lookup(Class<T> type) {
        ProtoCodec<?> c = REGISTRY.get(type);
        if (c == null) {
            throw new IllegalStateException(
                    "No ProtoCodec registered for " + type.getName()
                            + " -- did cn1:process-annotations run and is the "
                            + "class annotated @ProtoMessage?");
        }
        return (ProtoCodec<T>) c;
    }
}
