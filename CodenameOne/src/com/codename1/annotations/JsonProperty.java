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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Names a field in the JSON form, and says how a body may spell it.
///
/// The default JSON key is the field name; `value` lets a field map to
/// `snake_case` or any other spelling without touching the Java identifier.
/// `aliases` are further names a body may use for the same member -- an old
/// spelling a client still sends -- and are only ever read: the value is
/// always written under `value`. `required` makes a body that leaves the
/// member out a refused body, naming the member; an explicit `null` is still
/// a value, so it is accepted (and refused for a primitive, which cannot hold it).
///
/// On a parameter of a [JsonCreator] constructor or factory it names the member
/// that argument is read from, since a compiled class does not keep its
/// parameter names.
///
/// The server's generated codecs read it too, so a class shared between an app
/// and its backend has one JSON form on both sides.
@com.codename1.impl.SharedWithBackend
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface JsonProperty {
    /// The JSON key; empty for the field's own name.
    String value() default "";

    /// Other keys a body may carry the value under. Read only, never written.
    String[] aliases() default {};

    /// Whether a body that leaves the member out is refused.
    boolean required() default false;
}
