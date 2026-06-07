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

/// Binds a [ProtoMessage] field to a protobuf field tag. Tags must
/// be positive, unique within the message, and correspond to the
/// tag declared in the upstream `.proto` file.
///
/// Optional [#wireType()] forces a non-default scalar encoding for
/// integer fields. Defaults to [WireKind#DEFAULT], which selects
/// varint for `int32` / `int64` / `bool`, fixed32 / fixed64 for
/// `float` / `double`, and length-delimited for strings, byte arrays,
/// nested messages, and `repeated` packed scalars. Specify
/// [WireKind#SINT] for ZigZag-encoded signed integers, or
/// [WireKind#FIXED] for fixed-width unsigned integers (matches
/// `fixed32` / `fixed64` in proto3).
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ProtoField {
    /// Protobuf field tag (positive integer, unique per message).
    int tag();

    /// Optional override of the Java field name when the generator
    /// had to rename it to a valid identifier. Carries the original
    /// `.proto` name so introspection tooling can recover it.
    String name() default "";

    /// Non-default integer encoding selector. Has no effect for
    /// non-integer fields.
    WireKind wireType() default WireKind.DEFAULT;

    /// Integer encoding selectors. `DEFAULT` matches `int32` /
    /// `int64` / `uint32` / `uint64` (varint). `SINT` matches
    /// `sint32` / `sint64` (ZigZag-encoded varint). `FIXED` matches
    /// `fixed32` / `fixed64` / `sfixed32` / `sfixed64` (fixed-width).
    enum WireKind {
        DEFAULT, SINT, FIXED
    }
}
