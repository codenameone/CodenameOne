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
import android.content.res.Resources;
import android.util.TypedValue;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
public class ResourceSelectionRegressionTest {
    @org.junit.Before public void initialize() { com.codename1.androidcompat.testing.AndroidTestSupport.context(); }
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void combinedDimensionsUseTotalDistance() {
        DeviceConfig d = new DeviceConfig(); d.widthDp = 600; d.heightDp = 800;
        ResConfigSpec near = new ResConfigSpec("w400dp-h700dp");
        ResConfigSpec far = new ResConfigSpec("w500dp-h100dp");
        assertTrue(near.matches(d)); assertTrue(far.matches(d));
        assertTrue(near.isBetterThan(far, d)); assertFalse(far.isBetterThan(near, d));
    }
    @Test public void exactDensityBeatsNodpiInEitherOrder() {
        DeviceConfig d = new DeviceConfig(); d.densityDpi = 240;
        ResConfigSpec exact = new ResConfigSpec("hdpi"), none = new ResConfigSpec("nodpi");
        assertTrue(exact.isBetterThan(none, d)); assertFalse(none.isBetterThan(exact, d));
    }
    private Resources plurals(int[] keys) {
        ResValue[] values = new ResValue[keys.length];
        for (int i=0; i<keys.length; i++) values[i] = new ResValue(TypedValue.TYPE_STRING, 0, "category"+keys[i]);
        final ResTable.Bag bag = new ResTable.Bag(0, keys, values);
        return new Resources(ResourceManager.get()) { @Override public Object item(int id) { return bag; } };
    }
    @Test public void missingSelectedCategoryUsesOnlyOther() {
        DeviceConfig d = ResourceManager.get().device(); String old = d.language;
        try {
            d.language = "fr";
            assertEquals("category5", plurals(new int[]{PluralRules.ZERO, PluralRules.OTHER}).getQuantityText(1,0));
            try { plurals(new int[]{PluralRules.ZERO}).getQuantityText(1,0); fail("missing other"); }
            catch (Resources.NotFoundException expected) { }
        } finally { d.language = old; }
    }
    @Test public void portugueseZeroUsesTheDeviceRegion() {
        DeviceConfig d = ResourceManager.get().device(); String lang = d.language, region = d.region;
        try {
            d.language = "pt"; d.region = "PT";
            Resources r = plurals(new int[]{PluralRules.ONE, PluralRules.OTHER});
            assertEquals("category5", r.getQuantityText(1,0));
            assertEquals("category1", r.getQuantityText(1,1));
            d.region = "BR"; assertEquals("category1", r.getQuantityText(1,0));
        } finally { d.language = lang; d.region = region; }
    }
}
