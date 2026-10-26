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

/// Converts a value to and from its JSON form, for a field or class marked
/// [JsonAdapter].
///
/// Both directions use the parsed form of JSON: `null`, a `String`, a
/// `Boolean`, a `Number`, a `java.util.Map` with `String` keys for an object
/// and a `java.util.List` for an array, nested as deeply as needed. A parser may
/// hand a number over as any `Number` subclass, so read one through
/// `Number#longValue()` or `Number#doubleValue()` rather than casting it to a
/// particular class.
///
/// Throw `IllegalArgumentException` from [#fromJson(Object)] to refuse a value;
/// the generated code adds where in the document it was. `T` is the type
/// converted.
@com.codename1.impl.SharedWithBackend
public interface JsonTypeAdapter<T> {
    /// The JSON form of `value`, which may be `null`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value to write
    ///
    /// #### Returns
    ///
    /// its parsed JSON form
    Object toJson(T value);

    /// The value `json` is the form of; `json` may be `null`.
    ///
    /// #### Parameters
    ///
    /// - `json`: the parsed JSON value
    ///
    /// #### Returns
    ///
    /// the value it stands for
    T fromJson(Object json);
}
