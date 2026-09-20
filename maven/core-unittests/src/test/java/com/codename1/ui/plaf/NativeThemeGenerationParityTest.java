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

package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.util.Resources;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two iOS generations are built from one `common.css` plus a generation
 * layer, so they can differ only where `gen27.css` says so. These are the
 * invariants that stay true no matter what lands in that layer.
 *
 * <p>The interesting one is the key SUPERSET. The classic way to break a
 * generation split is to promote a declaration into `gen27.css` and forget its
 * dual in `gen26.css`: generation 26 quietly loses the property, and because
 * `verify-native-theme-split.sh` only runs when someone remembers to run it,
 * nothing else would notice. A UIID or key that exists in 26 and not in 27 is
 * the same mistake mirrored.</p>
 *
 * <p>Loads the `.res` straight from `Themes/` and skips when absent, the same
 * convention as the neighbouring native-theme tests -- except for the pairing
 * itself: if ONE of the two is present the other must be too, because a build
 * that produced only one of them is a build script that stopped emitting the
 * second.</p>
 */
public class NativeThemeGenerationParityTest extends UITestBase {

    private static final String GEN26 = "iOSModernTheme.res";
    private static final String GEN27 = "iOSModern27Theme.res";

    @Test
    public void bothGenerationsAreBuiltOrNeitherIs() {
        File a = locateNativeTheme(GEN26);
        File b = locateNativeTheme(GEN27);
        if (a == null && b == null) {
            return;
        }
        assertNotNull(a, GEN26 + " is missing while " + GEN27 + " was built");
        assertNotNull(b, GEN27 + " is missing while " + GEN26 + " was built");
    }

    @Test
    public void generation27CarriesEveryKeyGeneration26Has() throws Exception {
        Hashtable gen26 = loadTheme(GEN26);
        Hashtable gen27 = loadTheme(GEN27);
        if (gen26 == null || gen27 == null) {
            return;
        }
        Set<String> missing = new TreeSet<String>();
        for (Enumeration e = gen26.keys(); e.hasMoreElements();) {
            String key = String.valueOf(e.nextElement());
            if (!gen27.containsKey(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(),
                "generation 27 is missing keys generation 26 has, which means a"
                        + " declaration left common.css without a dual in gen27.css: "
                        + missing);
    }

    @Test
    public void bothGenerationsKeepTheMandatoryConstants() throws Exception {
        Hashtable gen26 = loadTheme(GEN26);
        Hashtable gen27 = loadTheme(GEN27);
        if (gen26 == null || gen27 == null) {
            return;
        }
        // A native theme that inherits from the native theme recurses at load
        // time, and $Dark resolution is gated on darkModeBool -- so these two
        // are not style choices, they are load-bearing.
        for (Hashtable theme : new Hashtable[]{gen26, gen27}) {
            assertEquals("false", String.valueOf(theme.get("@includeNativeBool")),
                    "a native theme must not inherit from the native theme");
            assertEquals("true", String.valueOf(theme.get("@darkModeBool")),
                    "darkModeBool gates the whole $Dark palette");
        }
    }

    @Test
    public void theAccentPaletteIsSharedByBothGenerations() throws Exception {
        Hashtable gen26 = loadTheme(GEN26);
        Hashtable gen27 = loadTheme(GEN27);
        if (gen26 == null || gen27 == null) {
            return;
        }
        // gen27.css must not declare --* at all: var() is resolved at parse
        // time, so a redeclaration would retune only the uses textually after
        // it AND overwrite the exported @accent-color constant that the runtime
        // override path reads. Every accent constant and every @cn1-bind entry
        // therefore has to be identical across the two.
        for (Enumeration e = gen26.keys(); e.hasMoreElements();) {
            String key = String.valueOf(e.nextElement());
            boolean shared = key.startsWith("@cn1-bind:")
                    || (key.startsWith("@") && key.contains("accent"));
            if (!shared) {
                continue;
            }
            assertEquals(String.valueOf(gen26.get(key)), String.valueOf(gen27.get(key)),
                    key + " must be identical across generations -- a generation"
                            + " layer must not declare --* custom properties");
        }
    }

    private static Hashtable loadTheme(String fileName) throws Exception {
        File themeFile = locateNativeTheme(fileName);
        if (themeFile == null) {
            return null;
        }
        InputStream stream = new FileInputStream(themeFile);
        try {
            Resources res = Resources.open(stream);
            String[] names = res.getThemeResourceNames();
            assertNotNull(names, fileName + " carries no theme");
            assertTrue(names.length > 0, fileName + " carries no theme");
            return res.getTheme(names[0]);
        } finally {
            stream.close();
        }
    }

    private static File locateNativeTheme(String fileName) {
        File cwd = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && cwd != null; i++) {
            File candidate = new File(cwd, "Themes/" + fileName);
            if (candidate.isFile()) {
                return candidate;
            }
            cwd = cwd.getParentFile();
        }
        return null;
    }
}
