/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.services;

import com.codename1.ui.TextArea;

/// Flutter's TextInputType. A class, not an enum, because
/// TextInputType.numberWithOptions(signed:, decimal:) makes values beyond the
/// named ones. The named values are singletons, so identity comparison of two
/// TextInputType.number reads still holds.
public final class TextInputType {
    public static final TextInputType text = new TextInputType(0, "text", null, null);
    public static final TextInputType multiline = new TextInputType(1, "multiline", null, null);
    public static final TextInputType number = new TextInputType(2, "number", Boolean.FALSE, Boolean.FALSE);
    public static final TextInputType phone = new TextInputType(3, "phone", null, null);
    public static final TextInputType datetime = new TextInputType(4, "datetime", null, null);
    public static final TextInputType emailAddress = new TextInputType(5, "emailAddress", null, null);
    public static final TextInputType url = new TextInputType(6, "url", null, null);
    public static final TextInputType visiblePassword = new TextInputType(7, "visiblePassword", null, null);
    public static final TextInputType name = new TextInputType(8, "name", null, null);
    public static final TextInputType streetAddress = new TextInputType(9, "streetAddress", null, null);
    public static final TextInputType none = new TextInputType(10, "none", null, null);

    private final int index;
    private final String label;
    private final Boolean signed;
    private final Boolean decimal;

    private TextInputType(int index, String label, Boolean signed, Boolean decimal) {
        this.index = index;
        this.label = label;
        this.signed = signed;
        this.decimal = decimal;
    }

    /// TextInputType.numberWithOptions({bool? signed = false, bool? decimal = false}).
    public static TextInputType numberWithOptions(Boolean signed, Boolean decimal) {
        return new TextInputType(2, "number", signed == null ? Boolean.FALSE : signed,
                decimal == null ? Boolean.FALSE : decimal);
    }

    public int index() {
        return index;
    }

    public Boolean signed() {
        return signed;
    }

    public Boolean decimal() {
        return decimal;
    }

    /// The Codename One TextArea constraint this keyboard asks for.
    public int constraint() {
        switch (index) {
            case 2:
                return Boolean.TRUE.equals(decimal) ? TextArea.DECIMAL : TextArea.NUMERIC;
            case 3:
                return TextArea.PHONENUMBER;
            case 5:
                return TextArea.EMAILADDR;
            case 6:
                return TextArea.URL;
            case 7:
                return TextArea.NON_PREDICTIVE;
            default:
                return TextArea.ANY;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextInputType)) {
            return false;
        }
        TextInputType t = (TextInputType) o;
        return t.index == index && eq(t.signed, signed) && eq(t.decimal, decimal);
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public int hashCode() {
        return index * 31 + (Boolean.TRUE.equals(signed) ? 2 : 0) + (Boolean.TRUE.equals(decimal) ? 1 : 0);
    }

    @Override
    public String toString() {
        if (signed != null) {
            return "TextInputType(name: TextInputType." + label + ", signed: " + signed + ", decimal: " + decimal + ")";
        }
        return "TextInputType(name: TextInputType." + label + ", signed: null, decimal: null)";
    }
}
