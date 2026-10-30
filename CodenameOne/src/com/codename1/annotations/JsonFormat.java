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

/// Picks the JSON shape a date or number field is written in.
///
/// - A `java.util.Date` is milliseconds since the epoch by default. With
///   [Shape#STRING] it is an ISO-8601 instant in UTC,
///   `2026-10-25T08:30:00.000Z`.
/// - A number is a JSON number by default. With [Shape#STRING] it is the same
///   digits in quotes -- what a JavaScript client needs for a `long` past 2^53,
///   which its numbers cannot hold exactly.
///
/// Reading accepts either shape whatever the annotation says, so a field can
/// change its format without breaking the clients that still send the old one.
/// Any other combination is refused by the build.
@com.codename1.impl.SharedWithBackend
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface JsonFormat {
    /// The shape the value is written in.
    Shape value();

    /// A JSON shape for a scalar value.
    enum Shape {
        /// The type's own form: a number, or a date as epoch milliseconds.
        DEFAULT,
        /// A string: a number's digits, or a date in ISO-8601.
        STRING,
        /// A number: a date as epoch milliseconds.
        NUMBER
    }
}
