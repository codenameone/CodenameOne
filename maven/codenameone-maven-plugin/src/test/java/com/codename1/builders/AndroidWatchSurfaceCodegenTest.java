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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The decisions the Wear complication codegen makes, as pure functions so they can be pinned
/// without generating a project.
class AndroidWatchSurfaceCodegenTest {

    private static BuildRequest request() {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.setPackageName("com.mycompany.myapp");
        req.setDisplayName("My App");
        req.setVersion("1.0");
        return req;
    }

    // --- which module is the watch product ------------------------------------

    /// A project that has not asked for a watch must be left completely alone, which is what
    /// every branch downstream keys on.
    @Test
    void noWatchMainMeansNoWatchModule() {
        assertNull(AndroidGradleBuilder.watchModuleName(request()));
    }

    /// Standalone: the single APK IS the watch app, so the services go in the phone module's
    /// place -- there is no phone app to keep them out of.
    @Test
    void standaloneMakesTheAppModuleTheWatchProduct() {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.Watch");
        req.putArgument("watchStandalone", "true");

        assertEquals("app", AndroidGradleBuilder.watchModuleName(req));
    }

    /// Companion: a second module beside the phone one.
    @Test
    void companionGeneratesASeparateWearModule() {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.Watch");

        assertEquals("wear", AndroidGradleBuilder.watchModuleName(req));
    }

    /// A project that wants the wearable link but no watch app of its own can say so, and its
    /// phone build is then exactly what it was.
    @Test
    void theWearModuleCanBeDeclined() {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.Watch");
        req.putArgument("android.watchModule", "false");

        assertNull(AndroidGradleBuilder.watchModuleName(req));
    }

    // --- family to ComplicationData mapping ------------------------------------

    /// A watch face asks a data source for ONE type and gets nothing if the source does not
    /// offer it, so this list is what decides whether a complication can be placed at all.
    @Test
    void circularOffersAGaugeAGlyphAndAReadout() {
        assertEquals("RANGED_VALUE,MONOCHROMATIC_IMAGE,SHORT_TEXT",
                AndroidGradleBuilder.complicationTypes("watchCircular"));
    }

    /// Wear OS has no corner slot at all; a corner complication is round, so it offers exactly
    /// what the circular family does -- which is what WidgetSize already documents.
    @Test
    void cornerIsTreatedAsCircular() {
        assertEquals(AndroidGradleBuilder.complicationTypes("watchCircular"),
                AndroidGradleBuilder.complicationTypes("watchCorner"));
    }

    @Test
    void inlineIsShortTextOnly() {
        assertEquals("SHORT_TEXT", AndroidGradleBuilder.complicationTypes("watchInline"));
    }

    @Test
    void rectangularIsTheRoomyOne() {
        assertEquals("LONG_TEXT,SHORT_TEXT",
                AndroidGradleBuilder.complicationTypes("watchRectangular"));
    }

    /// Declaring several families offers the union, deduped, so one data source can fill every
    /// slot the developer designed for.
    @Test
    void severalFamiliesOfferTheUnionWithoutRepeats() {
        String types = AndroidGradleBuilder.complicationTypes(
                "watchCircular,watchRectangular,watchInline");

        assertTrue(types.contains("RANGED_VALUE"), types);
        assertTrue(types.contains("LONG_TEXT"), types);
        assertEquals(types.indexOf("SHORT_TEXT"), types.lastIndexOf("SHORT_TEXT"), types);
    }

    /// A phone family contributes nothing: a home-screen size is not a watch-face slot.
    @Test
    void phoneFamiliesContributeNoComplicationTypes() {
        assertEquals("", AndroidGradleBuilder.complicationTypes("small,medium,large,lockscreen"));
    }

    // --- tiles ------------------------------------------------------------------

    /// Only the rectangular family is roomy enough for a layout rather than a readout, so it is
    /// the only one that earns a Tile.
    @Test
    void onlyTheRectangularFamilyEarnsATile() {
        assertTrue(AndroidGradleBuilder.declaresTile("watchRectangular"));
        assertTrue(AndroidGradleBuilder.declaresTile("small,watchRectangular"));
        assertFalse(AndroidGradleBuilder.declaresTile("watchCircular,watchInline,watchCorner"));
        assertFalse(AndroidGradleBuilder.declaresTile("small,medium"));
    }
}
