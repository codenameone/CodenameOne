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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueEncoderTest {

    @Test
    public void complexRoundTripsCommonDimensions() {
        float[] values = {0, 1, 16, 0.5f, 2.25f, 300, -4, 1234.5f};
        for (float v : values) {
            int c = Value.floatToComplex(v, Value.COMPLEX_UNIT_DIP);
            assertEquals("unit", Value.COMPLEX_UNIT_DIP, c & Value.COMPLEX_UNIT_MASK);
            assertEquals("value " + v, v, Value.complexToFloat(c), Math.abs(v) / 1000f + 0.0001f);
        }
    }

    @Test
    public void dimensionsColorsAndNumbers() {
        Value d = ValueEncoder.encodeDimension("16dp", "16dp");
        assertEquals(Value.TYPE_DIMENSION, d.type);
        assertEquals(Value.COMPLEX_UNIT_DIP, d.data & 0xf);
        assertEquals(Value.COMPLEX_UNIT_SP, ValueEncoder.encodeDimension("14sp", "").data & 0xf);
        assertEquals(0xff3f51b5, ValueEncoder.encodeColor("#3F51B5", "").data);
        assertEquals(0x80ff0000, ValueEncoder.encodeColor("#80FF0000", "").data);
        assertEquals(0xffaabbcc, ValueEncoder.encodeColor("#abc", "").data);
        assertEquals(42, ValueEncoder.encodeInteger("42", "").data);
        assertEquals(255, ValueEncoder.encodeInteger("0xff", "").data);
    }

    @Test
    public void attrFormatsDecideTheType() {
        SymbolTable st = new SymbolTable();
        ValueEncoder enc = new ValueEncoder(st, "app");
        AttrDef str = new AttrDef("android:text", AttrDef.FORMAT_STRING);
        assertEquals(Value.TYPE_STRING, enc.encode("16dp", str, null).type);
        AttrDef orient = new AttrDef("android:orientation", AttrDef.FORMAT_ENUM);
        orient.enums.put("horizontal", 0);
        orient.enums.put("vertical", 1);
        Value v = enc.encode("vertical", orient, null);
        assertEquals(Value.TYPE_INT_DEC, v.type);
        assertEquals(1, v.data);
        AttrDef gravity = new AttrDef("android:gravity", AttrDef.FORMAT_FLAGS);
        gravity.flags.put("center_vertical", 0x10);
        gravity.flags.put("left", 0x03);
        assertEquals(0x13, enc.encode("center_vertical|left", gravity, null).data);
        AttrDef size = new AttrDef("android:layout_width", AttrDef.FORMAT_DIMENSION | AttrDef.FORMAT_ENUM);
        size.enums.put("match_parent", -1);
        assertEquals(-1, enc.encode("match_parent", size, null).data);
    }

    @Test
    public void stringsUnescapeLikeAapt() {
        assertEquals("a b", AndroidStrings.unescape("  a \n  b  "));
        assertEquals("  keep  ", AndroidStrings.unescape("\"  keep  \""));
        assertEquals("it's", AndroidStrings.unescape("it\\'s"));
        assertEquals("line\nbreak", AndroidStrings.unescape("line\\nbreak"));
        assertEquals("\u00e9", AndroidStrings.unescape("\\u00e9"));
    }
}
