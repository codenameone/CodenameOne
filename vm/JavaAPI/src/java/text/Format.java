/*
 * Copyright (c) 2012, Eric Coolman, Codename One and/or its affiliates. All rights reserved.
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
package java.text;

/// The base of the formats that turn locale sensitive values into text and
/// back: [DateFormat], [NumberFormat] and [MessageFormat].
///
/// A subclass implements [#format(Object, StringBuffer, FieldPosition)] and
/// [#parseObject(String, ParsePosition)]; the one-argument forms are written
/// over them, as in the JDK.
///
/// #### Differences from the JDK
///
/// - No `clone` here: `Object.clone()` answers `null` on the device for
///   anything but an array, so every format of this package declares a
///   `clone` that copies its own fields.
/// - No `formatToCharacterIterator`: the device has no
///   `AttributedCharacterIterator`.
public abstract class Format implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    protected Format() {
    }

    /// Formats an object to text.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if this format cannot format `obj`
    public final String format(Object obj) {
        return format(obj, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /// Formats an object, appending the text to `toAppendTo`, and reports the
    /// range the field `pos` names took.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if this format cannot format `obj`
    public abstract StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos);

    /// Parses an object from `source` starting at `pos`. On success the index
    /// of `pos` is moved past what was read; on failure it is left alone, the
    /// error index is set and `null` is returned.
    public abstract Object parseObject(String source, ParsePosition pos);

    /// Parses an object from the beginning of `source`.
    ///
    /// #### Throws
    ///
    /// - `ParseException`: if nothing at the beginning of `source` parses
    public Object parseObject(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object result = parseObject(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Format.parseObject(String) failed", pos.getErrorIndex());
        }
        return result;
    }
}
