/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.vault;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The binding's serialization, and the ambiguity it has to not have.
class AssociatedDataTest extends UITestBase {

    private static String hex(byte[] data) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < data.length; iter++) {
            int v = data[iter] & 0xff;
            b.append("0123456789abcdef".charAt(v >>> 4));
            b.append("0123456789abcdef".charAt(v & 0x0f));
        }
        return b.toString();
    }

    @Test
    void formatIsPinned() {
        // A field count, then a length-prefixed UTF-8 field each. Pinned because an
        // implementation in another language has to produce these exact bytes or nothing it
        // writes can be opened here.
        byte[] bytes = AssociatedData.of("a", "b", "c", "d").serialize();
        assertEquals("00000004"
                + "00000001" + "61"
                + "00000001" + "62"
                + "00000001" + "63"
                + "00000001" + "64", hex(bytes));
    }

    @Test
    void fieldBoundariesAreUnambiguous() {
        // The failure a separator character would have: ("ab","c") and ("a","bc") must not
        // serialize alike, or the binding stops distinguishing the cases it exists to
        // distinguish.
        assertFalse(java.util.Arrays.equals(
                AssociatedData.of("ab", "c", "", "").serialize(),
                AssociatedData.of("a", "bc", "", "").serialize()));
    }

    @Test
    void emptyFieldsStillCarryTheirLength() {
        byte[] bytes = AssociatedData.of("", "", "", "").serialize();
        assertEquals("00000004" + "00000000" + "00000000" + "00000000" + "00000000", hex(bytes));
    }

    @Test
    void nullIsTheEmptyString() {
        assertArrayEquals(AssociatedData.of(null, null, null).serialize(),
                AssociatedData.of("", "", "", "").serialize());
    }

    @Test
    void derivedCopiesChangeOneFieldOnly() {
        AssociatedData base = AssociatedData.of("app", "vault", "rec", "record");
        assertEquals("other", base.withPurpose("other").getPurpose());
        assertEquals("rec", base.withPurpose("other").getRecord());
        assertEquals("rec2", base.withRecord("rec2").getRecord());
        assertEquals("record", base.withRecord("rec2").getPurpose());
    }
}
