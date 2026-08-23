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
package com.codename1.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Both device builders classify a kind's families through this one class, so the rule it
/// encodes has to be pinned here rather than at either call site. The cases below are the ones
/// that have actually been got wrong.
class SurfaceKindFamiliesTest {

    private static Map<String, Object> kind(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", "k");
        if (key != null) {
            m.put(key, value);
        }
        return m;
    }

    /// The portable key is what a manifest should say now, so it wins outright.
    @Test
    void familiesWinsOverIosFamilies() {
        Map<String, Object> m = kind("families", Arrays.asList("watchCircular"));
        m.put("iosFamilies", Arrays.asList("small", "medium"));

        assertEquals(Arrays.asList("watchCircular"), SurfaceKindFamilies.read(m),
                "families is the portable spelling and must not be merged with the legacy one");
    }

    /// A manifest carrying both is far likelier to be mid-migration than to mean the union, and
    /// unioning would resurrect a family the author had just removed.
    @Test
    void iosFamiliesIsReadOnlyWhenFamiliesIsAbsent() {
        assertEquals(Arrays.asList("small"),
                SurfaceKindFamilies.read(kind("iosFamilies", Arrays.asList("small"))));
        assertEquals(0, SurfaceKindFamilies.read(kind(null, null)).size());
    }

    @Test
    void nonStringEntriesAreSkippedRatherThanFailing() {
        Map<String, Object> m = kind("families", Arrays.asList("small", Integer.valueOf(7), null));

        assertEquals(Arrays.asList("small"), SurfaceKindFamilies.read(m));
    }

    /// accessoryCorner is the ONLY WidgetKit spelling that names a watch-only family.
    @Test
    void onlyAccessoryCornerNormalizesToAWatchFamily() {
        assertEquals("watchCorner", SurfaceKindFamilies.normalize("accessoryCorner"));
        assertEquals("accessoryCircular", SurfaceKindFamilies.normalize("accessoryCircular"));
        assertEquals("small", SurfaceKindFamilies.normalize("small"));
    }

    /// The trap this class exists for, in both directions.
    ///
    /// accessoryCircular / accessoryInline / accessoryRectangular are the iPhone LOCK-SCREEN
    /// families as well as watch ones, so treating them as watch families withholds a
    /// lock-screen widget the manifest asked for. The portable watch* names mean "complication
    /// only" and must not be treated as phone families.
    @Test
    void widgetKitAccessorySpellingsAreNotWatchFamilies() {
        assertFalse(SurfaceKindFamilies.isWatch("accessoryCircular"));
        assertFalse(SurfaceKindFamilies.isWatch("accessoryInline"));
        assertFalse(SurfaceKindFamilies.isWatch("accessoryRectangular"));
        assertFalse(SurfaceKindFamilies.isWatch("lockscreen"));

        assertTrue(SurfaceKindFamilies.isWatch("watchCircular"));
        assertTrue(SurfaceKindFamilies.isWatch("watchRectangular"));
        assertTrue(SurfaceKindFamilies.isWatch("watchInline"));
        assertTrue(SurfaceKindFamilies.isWatch("watchCorner"));
        assertTrue(SurfaceKindFamilies.isWatch("accessoryCorner"));
    }

    @Test
    void hasWatchFamilyIsTrueForAMixedKindButWatchOnlyIsNot() {
        List<String> mixed = Arrays.asList("small", "watchCircular");

        assertTrue(SurfaceKindFamilies.hasWatchFamily(mixed));
        assertFalse(SurfaceKindFamilies.isWatchOnly(mixed),
                "a kind that also offers a home-screen widget is not watch-only");
        assertTrue(SurfaceKindFamilies.hasPhoneFamily(mixed));
    }

    @Test
    void watchOnlyKindHasNoPhoneFamily() {
        List<String> watchOnly = Arrays.asList("watchCircular", "watchInline");

        assertTrue(SurfaceKindFamilies.isWatchOnly(watchOnly));
        assertFalse(SurfaceKindFamilies.hasPhoneFamily(watchOnly));
    }

    /// An empty declaration means the kind took the default -- the three home-screen sizes --
    /// not that it opted out of every surface.
    @Test
    void emptyDeclarationIsAPhoneKind() {
        List<String> none = Arrays.<String>asList();

        assertFalse(SurfaceKindFamilies.isWatchOnly(none));
        assertFalse(SurfaceKindFamilies.hasWatchFamily(none));
        assertTrue(SurfaceKindFamilies.hasPhoneFamily(none));
    }

    /// The four names exactly. A prefix test made a mistyped "watchCircle" a watch family here
    /// while every mapping downstream recognised only the real four -- so the kind lost its phone
    /// widget, gained watch codegen, and produced no usable surface anywhere, in a build that
    /// went green.
    @Test
    void aMistypedWatchNameIsNotAWatchFamily() {
        assertTrue(SurfaceKindFamilies.isWatch("watchCircular"));
        assertTrue(SurfaceKindFamilies.isWatch("watchRectangular"));
        assertTrue(SurfaceKindFamilies.isWatch("watchInline"));
        assertTrue(SurfaceKindFamilies.isWatch("watchCorner"));

        assertFalse(SurfaceKindFamilies.isWatch("watchCircle"));
        assertFalse(SurfaceKindFamilies.isWatch("watch"));
        assertFalse(SurfaceKindFamilies.isWatch("watchSquare"));
        assertFalse(SurfaceKindFamilies.isWatch("watchcircular"));
    }

    /// And a typo is not a phone family either, so the builder can tell the author which of the
    /// two answers it has rather than quietly rendering a widget nobody asked for.
    @Test
    void anUnknownNameIsNeitherWatchNorPhone() {
        assertTrue(SurfaceKindFamilies.isKnown("small"));
        assertTrue(SurfaceKindFamilies.isKnown("lockscreen"));
        assertTrue(SurfaceKindFamilies.isKnown("accessoryCorner"));
        assertTrue(SurfaceKindFamilies.isKnown("watchCircular"));

        assertFalse(SurfaceKindFamilies.isKnown("watchCircle"));
        assertFalse(SurfaceKindFamilies.isKnown("enormous"));
        assertFalse(SurfaceKindFamilies.isKnown(null));
    }

    @Test
    void nullsAreTolerated() {
        assertEquals(0, SurfaceKindFamilies.read(null).size());
        assertFalse(SurfaceKindFamilies.hasWatchFamily(null));
        assertFalse(SurfaceKindFamilies.isWatchOnly(null));
        assertFalse(SurfaceKindFamilies.isWatch(null));
    }

    /// The portable key wins when PRESENT, not merely when well-formed. A manifest mid-migration
    /// is the one case carrying both keys, so falling through to the legacy list on a malformed
    /// portable value silently built the surface the author had just replaced.
    @Test
    void aMalformedFamiliesValueDoesNotResurrectTheLegacyList() {
        Map<String, Object> kind = new LinkedHashMap<String, Object>();
        kind.put("id", "status");
        kind.put("families", Integer.valueOf(7));
        kind.put("iosFamilies", Arrays.asList("small"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SurfaceKindFamilies.read(kind));
        assertTrue(ex.getMessage().contains("status"), ex.getMessage());
        assertTrue(ex.getMessage().contains("families"), ex.getMessage());
    }

    /// A bare string is the obvious shorthand and the obvious mistype, so it is read the way it
    /// was plainly meant rather than refused.
    @Test
    void aSingleFamilyNameIsReadAsOneFamily() {
        Map<String, Object> kind = new LinkedHashMap<String, Object>();
        kind.put("id", "status");
        kind.put("families", "watchCircular");
        kind.put("iosFamilies", Arrays.asList("small"));

        assertEquals(Arrays.asList("watchCircular"), SurfaceKindFamilies.read(kind));
    }

    /// The legacy key keeps its old tolerance: manifests carrying it predate this check, and
    /// refusing one now would fail a build that has always worked.
    @Test
    void aMalformedLegacyValueStillDegradesQuietly() {
        Map<String, Object> kind = new LinkedHashMap<String, Object>();
        kind.put("id", "status");
        kind.put("iosFamilies", Integer.valueOf(7));

        assertTrue(SurfaceKindFamilies.read(kind).isEmpty());
    }
}
