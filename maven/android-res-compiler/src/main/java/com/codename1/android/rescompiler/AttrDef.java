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
package com.codename1.android.rescompiler;

import java.util.LinkedHashMap;
import java.util.Map;

/// The declaration of one attribute: the formats its values may take and, for
/// enum and flag attributes, the symbolic names they may use. The value
/// encoder reads it to decide that `"vertical"` on `android:orientation` is the
/// integer 1, and that `"16dp"` on a string attribute is just text.
public final class AttrDef {

    public static final int FORMAT_ANY = 0xffff;
    public static final int FORMAT_REFERENCE = 1;
    public static final int FORMAT_STRING = 1 << 1;
    public static final int FORMAT_INTEGER = 1 << 2;
    public static final int FORMAT_BOOLEAN = 1 << 3;
    public static final int FORMAT_COLOR = 1 << 4;
    public static final int FORMAT_FLOAT = 1 << 5;
    public static final int FORMAT_DIMENSION = 1 << 6;
    public static final int FORMAT_FRACTION = 1 << 7;
    public static final int FORMAT_ENUM = 1 << 16;
    public static final int FORMAT_FLAGS = 1 << 17;

    /// The package-qualified name: `android:text` or `app:layout_behavior`
    /// style, except that the application's own attrs carry no prefix.
    public final String name;
    public int formats;
    public final Map<String, Integer> enums = new LinkedHashMap<String, Integer>();
    public final Map<String, Integer> flags = new LinkedHashMap<String, Integer>();

    public AttrDef(String name, int formats) {
        this.name = name;
        this.formats = formats;
    }

    public boolean allows(int format) {
        return formats == 0 || (formats & format) != 0;
    }

    public static int parseFormats(String spec) {
        if (spec == null || spec.trim().length() == 0) {
            return 0;
        }
        int f = 0;
        for (String part : spec.split("\\|")) {
            String p = part.trim();
            if (p.equals("reference")) {
                f |= FORMAT_REFERENCE;
            } else if (p.equals("string")) {
                f |= FORMAT_STRING;
            } else if (p.equals("integer")) {
                f |= FORMAT_INTEGER;
            } else if (p.equals("boolean")) {
                f |= FORMAT_BOOLEAN;
            } else if (p.equals("color")) {
                f |= FORMAT_COLOR;
            } else if (p.equals("float")) {
                f |= FORMAT_FLOAT;
            } else if (p.equals("dimension")) {
                f |= FORMAT_DIMENSION;
            } else if (p.equals("fraction")) {
                f |= FORMAT_FRACTION;
            } else if (p.equals("enum")) {
                f |= FORMAT_ENUM;
            } else if (p.equals("flags")) {
                f |= FORMAT_FLAGS;
            }
        }
        return f;
    }

    public static String formatsToString(int f) {
        if (f == 0) {
            return "any";
        }
        StringBuilder sb = new StringBuilder();
        String[] names = {"reference", "string", "integer", "boolean", "color", "float", "dimension", "fraction"};
        for (int i = 0; i < names.length; i++) {
            if ((f & (1 << i)) != 0) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(names[i]);
            }
        }
        if ((f & FORMAT_ENUM) != 0) {
            sb.append(sb.length() > 0 ? "|" : "").append("enum");
        }
        if ((f & FORMAT_FLAGS) != 0) {
            sb.append(sb.length() > 0 ? "|" : "").append("flags");
        }
        return sb.toString();
    }
}
