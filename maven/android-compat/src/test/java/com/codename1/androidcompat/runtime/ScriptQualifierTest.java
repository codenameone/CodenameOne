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
package com.codename1.androidcompat.runtime;

import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// `values-b+zh+Hans` and `values-b+zh+Hant` are told apart by the device's
/// script. The script used to be dropped while parsing, so both read as plain
/// `zh`, both matched, and table order picked Traditional or Simplified.
public class ScriptQualifierTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static DeviceConfig device(String locale) {
        String[] lr = ResourceManager.languageAndRegion(locale);
        DeviceConfig d = new DeviceConfig();
        d.language = lr[0];
        d.region = lr[1];
        d.script = ResourceManager.scriptOf(locale);
        return d;
    }

    @Test
    public void theDeviceScriptSelectsTheVariant() {
        ResConfigSpec hans = new ResConfigSpec("b+zh+Hans");
        ResConfigSpec hant = new ResConfigSpec("b+zh+Hant");
        ResConfigSpec plain = new ResConfigSpec("zh");

        DeviceConfig taiwan = device("zh_TW");
        assertTrue(hant.matches(taiwan));
        assertFalse(hans.matches(taiwan));

        DeviceConfig china = device("zh-Hans_CN");
        assertTrue(hans.matches(china));
        assertFalse(hant.matches(china));
        // A script beats no script.
        assertTrue(plain.matches(china));
        assertTrue(hans.isBetterThan(plain, china));
        assertFalse(plain.isBetterThan(hans, china));

        // A device whose script is unknown still matches either.
        DeviceConfig unknown = device("zh_CN");
        unknown.script = null;
        assertTrue(hans.matches(unknown));
        assertTrue(hant.matches(unknown));
    }

    @Test
    public void readsTheScriptFromEveryLocaleSpelling() {
        assertEquals("Hans", ResourceManager.scriptOf("zh-Hans_CN"));
        assertEquals("Hant", ResourceManager.scriptOf("zh_Hant_TW"));
        assertEquals("Hans", ResourceManager.scriptOf("zh_CN_#Hans"));
        assertEquals("Hant", ResourceManager.scriptOf("zh_HK"));
        assertEquals("Hans", ResourceManager.scriptOf("zh"));
        assertEquals("Latn", ResourceManager.scriptOf("sr-latn-RS"));
        assertNull(ResourceManager.scriptOf("en_US"));
        // The script is not mistaken for the language or the region.
        assertArrayEquals(new String[] {"zh", "CN"}, ResourceManager.languageAndRegion("zh-Hans_CN"));
        assertArrayEquals(new String[] {"zh", "TW"}, ResourceManager.languageAndRegion("zh_Hant_TW"));
        assertArrayEquals(new String[] {"zh", "CN"}, ResourceManager.languageAndRegion("zh_CN_#Hans"));
    }
}
