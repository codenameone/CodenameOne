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

/// Marks a POJO (or Java 17+ record) as a Protocol Buffers message.
/// The Codename One Maven plugin scans every `@ProtoMessage` class at
/// build time and emits a reflection-free `ProtoCodec` next to it
/// that serializes / deserializes the class to / from the binary
/// protobuf wire format. The generated `cn1app.ProtoBootstrap`
/// registers every codec with [com.codename1.io.grpc.ProtoCodecs] so
/// generated gRPC clients can resolve nested message types by class
/// without reflection.
///
/// Each persistable field on the class must carry [ProtoField] with a
/// unique tag. Field types may be: scalar (int / long / float /
/// double / boolean / String / byte[]), other `@ProtoMessage` types,
/// `@ProtoEnum`-marked enums, or `java.util.List` of any of the
/// above (interpreted as `repeated` in proto3).
///
/// Mirrors the design of [com.codename1.annotations.Mapped] for JSON
/// projection; you can carry both annotations on the same class to
/// support JSON and protobuf wire formats off the same POJO.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ProtoMessage {
}
