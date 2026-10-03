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

/// The resource types the compiler knows, with the type byte each one gets in
/// a resource id (`0xPPTTEEEE`). The numbering is ours, not aapt's: only the
/// R classes the same build generates ever see an id, so it needs to be stable
/// within a build and deterministic across builds, nothing more.
public enum ResType {
    ATTR("attr", 0x01, false),
    ID("id", 0x02, false),
    STYLE("style", 0x03, false),
    STRING("string", 0x04, false),
    DIMEN("dimen", 0x05, false),
    COLOR("color", 0x06, true),
    BOOL("bool", 0x07, false),
    INTEGER("integer", 0x08, false),
    ARRAY("array", 0x09, false),
    PLURALS("plurals", 0x0a, false),
    DRAWABLE("drawable", 0x0b, true),
    MIPMAP("mipmap", 0x0c, true),
    LAYOUT("layout", 0x0d, true),
    MENU("menu", 0x0e, true),
    ANIM("anim", 0x0f, true),
    ANIMATOR("animator", 0x10, true),
    XML("xml", 0x11, true),
    RAW("raw", 0x12, true),
    FONT("font", 0x13, true),
    FRACTION("fraction", 0x14, false),
    INTERPOLATOR("interpolator", 0x15, true),
    TRANSITION("transition", 0x16, true),
    NAVIGATION("navigation", 0x17, true);

    public final String tag;
    public final int typeId;
    /// Whether the type has a directory of its own under res/ whose files are
    /// resources (layout/, drawable/, ...). `color` is both: a `<color>` in
    /// values/ and a color state list file in color/.
    public final boolean fileBased;

    ResType(String tag, int typeId, boolean fileBased) {
        this.tag = tag;
        this.typeId = typeId;
        this.fileBased = fileBased;
    }

    public static ResType fromTag(String tag) {
        for (ResType t : values()) {
            if (t.tag.equals(tag)) {
                return t;
            }
        }
        return null;
    }
}
