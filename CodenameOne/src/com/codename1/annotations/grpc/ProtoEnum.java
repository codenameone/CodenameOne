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

/// Marks a Java `enum` as a Protocol Buffers enum. The generator
/// emits the enum with a `public final int number` field and a
/// `static Xxx forNumber(int n)` lookup. On the wire enums are
/// encoded as varint -- the field's tag from [ProtoField] is read /
/// written like an `int32`, and the integer value is mapped back to
/// the enum constant via `forNumber`.
///
/// Unknown numbers map to `null` so callers can distinguish "no
/// such constant" from "constant with number 0". For proto3 the
/// zero constant is the default and is what the wire produces when
/// the field is absent.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ProtoEnum {
}
