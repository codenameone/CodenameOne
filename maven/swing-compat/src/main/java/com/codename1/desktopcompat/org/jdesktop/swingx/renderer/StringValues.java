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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import java.text.DateFormat;
import java.util.Date;

/// The usual converters.
///
/// `FILE_NAME`, `FILE_TYPE` and the look and feel variants are absent.
/// `NUMBER_TO_STRING` shows a number by its `toString`, because the
/// device's number formats are not `java.text.Format`s; its
/// `getFormat()` answers `null`.
public final class StringValues {

    /// The empty string for everything.
    public static final StringValue EMPTY = new StringValue() {
        @Override
        public String getString(Object value) {
            return "";
        }
    };

    /// The value's `toString`, the empty string for `null`. A value that
    /// is itself a [StringValue] is asked to convert itself.
    public static final StringValue TO_STRING = new StringValue() {
        @Override
        public String getString(Object value) {
            return value != null ? value.toString() : "";
        }
    };

    /// A date in the device's short date format; anything else by
    /// `toString`.
    public static final FormatStringValue DATE_TO_STRING = new FormatStringValue() {
        private DateFormat dates;

        @Override
        public String getString(Object value) {
            if (value instanceof Date) {
                if (dates == null) {
                    dates = DateFormat.getDateInstance();
                }
                return dates.format((Date) value);
            }
            return TO_STRING.getString(value);
        }
    };

    /// A number, or anything else, by `toString`.
    public static final FormatStringValue NUMBER_TO_STRING = new FormatStringValue() {
        @Override
        public String getString(Object value) {
            return TO_STRING.getString(value);
        }
    };

    private StringValues() {
    }
}
