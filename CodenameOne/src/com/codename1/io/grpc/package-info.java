/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
/// gRPC-Web client runtime. [GrpcWeb] sends framed protobuf
/// requests and parses the trailer-delimited response; [ProtoCodec]
/// is the per-message encoder/decoder contract; [ProtoCodecs] is
/// the runtime registry the build-time-generated codecs populate;
/// [ProtoWriter] / [ProtoReader] are the low-level wire helpers;
/// [GrpcResponse] is the typed result wrapper; [GrpcClients] is
/// the per-`@GrpcClient` factory registry.
///
/// End-to-end usage is documented on
/// [com.codename1.annotations.grpc] -- the user-facing entry point
/// is the generated `<Service>Grpc.of(baseUrl)` factory rather than
/// any class in this package directly.
package com.codename1.io.grpc;
