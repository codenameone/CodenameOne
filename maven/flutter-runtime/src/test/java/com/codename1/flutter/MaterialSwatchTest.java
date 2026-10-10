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
package com.codename1.flutter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.generated.flutter.MaterialAccentColor;
import com.codename1.generated.flutter.MaterialColor;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/// Pins the material palettes to their real per-shade values.
///
/// Every shade used to resolve to the swatch's primary, so the colors demo drew
/// each palette as one flat block of ten identical rows and nothing reported a
/// problem. A swatch that answers the same colour for every key is the failure
/// this guards against, which is why the distinctness assertions matter as much
/// as the spot values.
class MaterialSwatchTest {

    @Test
    void redSwatchCarriesTheMaterialShades() {
        MaterialColor red = Colors.red;
        assertEquals(0xFFF44336L, red.value(), "primary");
        assertEquals(0xFFFFEBEEL, red.idx(50).value());
        assertEquals(0xFFE57373L, red.idx(300).value());
        assertEquals(0xFFF44336L, red.idx(500).value(), "500 is the primary");
        assertEquals(0xFFB71C1CL, red.idx(900).value());
    }

    @Test
    void greyCarriesItsTwoExtraShades() {
        // Grey is the one swatch with 350 and 850; a table built for exactly ten
        // keys would silently answer the primary for those two.
        assertEquals(0xFFEEEEEEL, Colors.grey.idx(200).value());
        assertEquals(0xFFD6D6D6L, Colors.grey.idx(350).value());
        assertEquals(0xFF303030L, Colors.grey.idx(850).value());
    }

    @Test
    void accentSwatchCarriesItsFourShades() {
        MaterialAccentColor pink = Colors.pinkAccent;
        assertEquals(0xFFFF80ABL, pink.idx(100).value());
        assertEquals(0xFFFF4081L, pink.idx(200).value(), "200 is the primary");
        assertEquals(0xFFF50057L, pink.idx(400).value());
        assertEquals(0xFFC51162L, pink.idx(700).value());
    }

    @Test
    void valueIsTheUnsignedArgbWordDartWouldPrint() {
        // The colors demo renders color.value.toRadixString(16); a signed word
        // makes every opaque colour negative and the demo prints "#000-1412".
        assertTrue(Colors.red.value() > 0, "an opaque colour is a positive int in Dart");
        assertEquals("fff44336", Long.toString(Colors.red.value(), 16));
    }

    @Test
    void everyShadeOfAPaletteIsDistinct() {
        for (MaterialColor swatch : new MaterialColor[] {
                Colors.red, Colors.pink, Colors.purple, Colors.deepPurple,
                Colors.indigo, Colors.blue, Colors.lightBlue, Colors.cyan,
                Colors.teal, Colors.green, Colors.lightGreen, Colors.lime,
                Colors.yellow, Colors.amber, Colors.orange, Colors.deepOrange,
                Colors.brown, Colors.grey, Colors.blueGrey }) {
            Set<Long> seen = new HashSet<Long>();
            long[] keys = new long[] { 50, 100, 200, 300, 400, 500, 600, 700, 800, 900 };
            for (long key : keys) {
                seen.add(Long.valueOf(swatch.idx(key).value()));
            }
            assertEquals((long) keys.length, (long) seen.size(),
                    "a palette must not repeat a colour across its shades");
        }
    }

    @Test
    void shadesGetDarkerAsTheKeyGrows() {
        // Not a colour-science claim, just the Material invariant the demo shows:
        // 50 is the lightest tint and 900 the darkest.
        assertTrue(luminance(Colors.blue.idx(50)) > luminance(Colors.blue.idx(900)));
        assertTrue(luminance(Colors.green.idx(100)) > luminance(Colors.green.idx(800)));
        assertNotEquals(Colors.blue.idx(50).value(), Colors.blue.value());
    }

    private static long luminance(Color c) {
        long v = c.value();
        return ((v >> 16) & 0xFF) * 3 + ((v >> 8) & 0xFF) * 6 + (v & 0xFF);
    }
}
