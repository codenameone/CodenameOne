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
package com.codename1.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds the values [JSONParser#parseStrict(String, JSONValueFactory)] reads.
///
/// The strict reader calls these methods as it walks a document, and whatever
/// they return is what the containers hold, so a caller can parse straight into
/// its own types instead of converting a tree of maps and lists afterwards. A
/// language runtime builds its own dictionaries and numbers this way, and a data
/// binder can build its objects without a second pass.
///
/// This class answers exactly what [JSONParser#parseStrict(String)] answers: a
/// `LinkedHashMap` per object, an `ArrayList` per array, `String`, `Boolean`,
/// `null`, `Long` for an integer and `Double` for any other number. Override the
/// methods whose result should differ. Containers are opened, filled in document
/// order and closed; a value is complete when it is handed to its parent.
///
/// Two relaxations of RFC 8259 are off unless a subclass turns them on:
/// [#allowNonFinite()] for `NaN`, `Infinity`, `-Infinity` and numbers too large
/// for a double, and [#allowControlCharacters()] for unescaped control
/// characters inside strings.
@com.codename1.impl.SharedWithBackend
public class JSONValueFactory {
    /// Opens an object.
    ///
    /// #### Returns
    ///
    /// the new, empty container, handed back to [#put(Object, String, Object)]
    /// and [#endObject(Object)]
    public Object newObject() {
        return new LinkedHashMap<String, Object>();
    }

    /// Adds one member to an object opened by [#newObject()]. A key may repeat;
    /// the reader passes every occurrence in order.
    ///
    /// #### Parameters
    ///
    /// - `object`: the container
    ///
    /// - `key`: the member name
    ///
    /// - `value`: the member value
    @SuppressWarnings("unchecked")
    public void put(Object object, String key, Object value) {
        ((Map<String, Object>) object).put(key, value);
    }

    /// Closes an object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the container
    ///
    /// #### Returns
    ///
    /// the value the parent receives, normally the container itself
    public Object endObject(Object object) {
        return object;
    }

    /// Opens an array.
    ///
    /// #### Returns
    ///
    /// the new, empty container
    public Object newArray() {
        return new ArrayList<Object>();
    }

    /// Appends one element to an array opened by [#newArray()].
    ///
    /// #### Parameters
    ///
    /// - `array`: the container
    ///
    /// - `value`: the element
    @SuppressWarnings("unchecked")
    public void add(Object array, Object value) {
        ((List<Object>) array).add(value);
    }

    /// Closes an array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the container
    ///
    /// #### Returns
    ///
    /// the value the parent receives, normally the container itself
    public Object endArray(Object array) {
        return array;
    }

    /// A string value. Member names are not passed here.
    ///
    /// #### Parameters
    ///
    /// - `value`: the decoded string
    ///
    /// #### Returns
    ///
    /// the value to store
    public Object string(String value) {
        return value;
    }

    /// An integer that fits a `long`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the integer
    ///
    /// #### Returns
    ///
    /// the value to store
    public Object integer(long value) {
        return Long.valueOf(value);
    }

    /// An integer too large for a `long`.
    ///
    /// #### Parameters
    ///
    /// - `digits`: the integer as written, with its sign
    ///
    /// #### Returns
    ///
    /// the value to store
    ///
    /// #### Throws
    ///
    /// - `IOException`: to refuse it, which this class does
    public Object bigInteger(String digits) throws java.io.IOException {
        throw new java.io.IOException("Malformed number '" + digits + "'");
    }

    /// A number with a fraction or an exponent, or a non-finite one when
    /// [#allowNonFinite()] is on.
    ///
    /// #### Parameters
    ///
    /// - `value`: the number
    ///
    /// #### Returns
    ///
    /// the value to store
    public Object decimal(double value) {
        return Double.valueOf(value);
    }

    /// `true` or `false`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the literal
    ///
    /// #### Returns
    ///
    /// the value to store
    public Object bool(boolean value) {
        return value ? Boolean.TRUE : Boolean.FALSE;
    }

    /// `null`.
    ///
    /// #### Returns
    ///
    /// the value to store
    public Object nullValue() {
        return null;
    }

    /// Whether `NaN`, `Infinity` and `-Infinity` are read, and a number such
    /// as `1e999` is read as infinite rather than refused. JavaScript's and
    /// Python's JSON writers emit these.
    ///
    /// #### Returns
    ///
    /// false, as RFC 8259 requires
    public boolean allowNonFinite() {
        return false;
    }

    /// Whether a control character below U+0020 may appear unescaped inside a
    /// string.
    ///
    /// #### Returns
    ///
    /// false, as RFC 8259 requires
    public boolean allowControlCharacters() {
        return false;
    }
}
