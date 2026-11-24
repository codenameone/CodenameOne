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
package com.codename1.wordprocessing;

/// A field: an instruction such as `PAGE`, `DATE \@ "d MMMM yyyy"` or `=SUM(ABOVE)` with the
/// result it last produced. Readers keep the result a file stored, so a document shows what its
/// author saw even when this library cannot evaluate the instruction.
public final class Field implements Inline {
    /// The instruction, without the surrounding braces.
    public final String instruction;

    /// The last computed result.
    public final String result;

    /// Whether the result is frozen against updates.
    public final boolean locked;

    /// Creates a field.
    ///
    /// #### Parameters
    ///
    /// - `instruction`: the instruction
    ///
    /// - `result`: the cached result, or null for none
    ///
    /// - `locked`: whether updates leave it alone
    public Field(String instruction, String result, boolean locked) {
        this.instruction = instruction == null ? "" : instruction.trim();
        this.result = result == null ? "" : result;
        this.locked = locked;
    }

    /// The first word of the instruction, upper-cased in ASCII: `PAGE`, `DATE`, `=` ...
    ///
    /// #### Returns
    ///
    /// the field type
    public String type() {
        String s = instruction;
        if (s.startsWith("=")) {
            return "=";
        }
        int end = 0;
        while (end < s.length() && s.charAt(end) > ' ') {
            end++;
        }
        char[] c = s.substring(0, end).toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - ('a' - 'A'));
            }
        }
        return new String(c);
    }

    /// A copy with a new result.
    ///
    /// #### Parameters
    ///
    /// - `value`: the result
    ///
    /// #### Returns
    ///
    /// the field
    public Field withResult(String value) {
        return new Field(instruction, value, locked);
    }

    @Override
    public String displayText() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Field)) {
            return false;
        }
        Field f = (Field) o;
        return instruction.equals(f.instruction) && result.equals(f.result) && locked == f.locked;
    }

    @Override
    public int hashCode() {
        return instruction.hashCode() * 31 + result.hashCode() + (locked ? 1 : 0);
    }

    @Override
    public String toString() {
        return "{" + instruction + "}";
    }
}
