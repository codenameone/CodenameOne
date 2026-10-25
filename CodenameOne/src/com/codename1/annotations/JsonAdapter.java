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

/// Hands a type's JSON form to the application's own [JsonTypeAdapter].
///
/// On a field it applies to that field; on a class, to every field, element
/// and body of that class, which is how a type the generated codecs cannot
/// map -- a money value, an identifier wrapper -- gets one form everywhere.
/// A field's adapter wins over its class's.
///
/// ```java
/// public final class EpochSecondsAdapter implements JsonTypeAdapter<java.util.Date> {
///     public Object toJson(java.util.Date d) {
///         return d == null ? null : Long.valueOf(d.getTime() / 1000);
///     }
///     public java.util.Date fromJson(Object json) {
///         if (json == null) {
///             return null;
///         }
///         // instanceof, never a bare cast: a failed cast does not throw on iOS.
///         if (!(json instanceof Number)) {
///             throw new IllegalArgumentException("expected epoch seconds");
///         }
///         return new java.util.Date(((Number) json).longValue() * 1000);
///     }
/// }
///
/// @Mapped
/// public class Invoice {
///     @JsonAdapter(EpochSecondsAdapter.class)
///     public java.util.Date issued;
/// }
/// ```
///
/// The adapter needs a public constructor without arguments. The generated code
/// creates one instance per adapter class and calls it directly, so nothing is
/// found by name at run time and the translated binary keeps the class.
@com.codename1.impl.SharedWithBackend
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface JsonAdapter {
    /// The adapter class.
    Class<? extends JsonTypeAdapter> value();
}
