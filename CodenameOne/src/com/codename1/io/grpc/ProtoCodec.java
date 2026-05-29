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

import java.io.IOException;

/// Per-class encoder / decoder for a `@ProtoMessage` type. The
/// Codename One Maven plugin emits one `<Type>ProtoCodec` per
/// `@ProtoMessage` class at build time and registers it with
/// [ProtoCodecs] so gRPC clients can locate codecs for nested
/// message types by class without reflection.
///
/// Implementations must be stateless -- the same codec instance is
/// shared across concurrent requests.
public interface ProtoCodec<T> {

    /// Writes `value`'s populated fields to `out` in the protobuf
    /// binary wire format. Does not write a length prefix; callers
    /// that need length-delimited framing (such as nested messages
    /// or gRPC payload framing) handle that one layer up.
    void write(ProtoWriter out, T value) throws IOException;

    /// Reads a message from `in` and returns the populated instance.
    /// `in` is expected to be positioned at the start of the message
    /// body (no length prefix); callers wrap a slice when reading
    /// nested messages.
    T read(ProtoReader in) throws IOException;
}
