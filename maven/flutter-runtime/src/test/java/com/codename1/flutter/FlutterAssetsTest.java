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

import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The asset-flattening contract: Codename One resources are flat on every port
 * ({@code getResourceAsStream} rejects nested names), so Flutter asset paths
 * must encode into root-level resource names — unambiguously, and using only
 * characters that were already legal in the path.
 */
class FlutterAssetsTest {

    @Test
    void resourceNameIsFlatAndAbsolute() {
        String r = FlutterAssets.resourceName("assets/studies/reply_card.png");
        assertTrue(r.startsWith("/"), r);
        assertEquals(-1, r.indexOf('/', 1), "resource name must not be nested: " + r);
    }

    @Test
    void separatorAndEscape() {
        assertEquals("/cn1f_assets_sstudies_sreply__card.png",
                FlutterAssets.resourceName("assets/studies/reply_card.png"));
    }

    @Test
    void packageAssetPath() {
        assertEquals("/cn1f_packages_sgallery__assets_sassets_sicons_smaterial_smaterial.png",
                FlutterAssets.resourceName("packages/gallery_assets/assets/icons/material/material.png"));
    }

    @Test
    void leadingSlashesIgnored() {
        assertEquals(FlutterAssets.flatName("assets/a.png"), FlutterAssets.flatName("/assets/a.png"));
    }

    @Test
    void extensionSurvivesForNativeBundlers() {
        assertTrue(FlutterAssets.flatName("assets/fonts/Roboto_Bold.ttf").endsWith(".ttf"));
    }

    /** The doubling rule is what keeps '_' as a separator unambiguous. */
    @Test
    void underscoreVersusSeparatorDoNotCollide() {
        Set<String> seen = new HashSet<String>();
        String[] paths = {
            "a/b.png",      // separator
            "a_b.png",      // literal underscore at root
            "a/b/c.png",
            "a_b/c.png",
            "a/b_c.png",
            // Adjacent underscore and separator: the doubling rule encoded both
            // of these as "a___b.png", and one asset overwrote the other.
            "a_/b.png",
            "a/_b.png",
            "a__/b.png",
            "a/__b.png",
            "a_/_b.png",
        };
        for (String p : paths) {
            assertTrue(seen.add(FlutterAssets.flatName(p)),
                    "collision on " + p + " -> " + FlutterAssets.flatName(p));
        }
    }

    @Test
    void neverStartsWithReservedRawPrefix() {
        assertTrue(FlutterAssets.flatName("raw/thing.png").startsWith("cn1f_"));
    }

    // ------------------------------------------------------------------
    // Density variants
    // ------------------------------------------------------------------

    @Test
    void variantsPreferTheSmallestAtLeastAsDenseAsTheScreen() {
        String[] c = FlutterAssets.variantCandidates("assets/icons/material.png", 3);
        assertEquals("assets/icons/3.0x/material.png", c[0]);
        assertEquals("assets/icons/4.0x/material.png", c[1]);
    }

    @Test
    void variantsFallBackDownwardThenToTheUnscaledAsset() {
        String[] c = FlutterAssets.variantCandidates("assets/icons/material.png", 3);
        assertEquals("assets/icons/2.0x/material.png", c[2]);
        assertEquals("assets/icons/1.5x/material.png", c[3]);
        assertEquals("assets/icons/material.png", c[c.length - 1],
                "the unscaled asset is always the last resort");
    }

    @Test
    void aDenserScreenThanAnyVariantTakesTheDensestAvailable() {
        String[] c = FlutterAssets.variantCandidates("a/b.png", 5);
        assertEquals("a/4.0x/b.png", c[0]);
        assertEquals("a/3.0x/b.png", c[1]);
    }

    @Test
    void anUnscaledScreenSkipsVariantsFirst() {
        String[] c = FlutterAssets.variantCandidates("a/b.png", 1);
        assertEquals("a/1.5x/b.png", c[0], "1.5x is the smallest variant at least as dense as 1x");
        assertEquals("a/b.png", c[c.length - 1]);
    }

    @Test
    void variantOfARootLevelAsset() {
        assertEquals("2.0x/b.png", FlutterAssets.variantCandidates("b.png", 2)[0]);
    }

    @Test
    void ratioOfIdentifiesTheLoadedDensity() {
        assertEquals(3.0, FlutterAssets.ratioOf("a/3.0x/b.png", "a/b.png"), 0.001);
        assertEquals(1.5, FlutterAssets.ratioOf("a/1.5x/b.png", "a/b.png"), 0.001);
        assertEquals(1.0, FlutterAssets.ratioOf("a/b.png", "a/b.png"), 0.001);
    }

    // ------------------------------------------------------------------
    // The manifest read guard
    // ------------------------------------------------------------------

    /**
     * Regression: {@code variantCandidates()}/{@code open()} running before
     * {@link com.codename1.ui.Display} is initialized used to mark the
     * manifest as read even though the guarded load never ran -- the
     * manifest was then never read for the rest of the process, even once
     * Display came up, so a manifest-only variant (one the fixed ratio
     * probe never tries, like {@code 2.5x} here) was never found. The
     * manifest and read-flag are process-wide static state, so both are
     * reset around this test regardless of what other tests already did
     * with them.
     */
    @Test
    void manifestReadIsRetriedOnceDisplayInitializes() throws Exception {
        resetManifestState();
        try {
            // Headless: Display is not initialized, so the guarded load must
            // not run -- and must not be remembered as having run either.
            String[] before = FlutterAssets.variantCandidates("assets/icons/material.png", 3);
            assertEquals("assets/icons/material.png", before[before.length - 1],
                    "no manifest could be read yet, so this must be the fixed-ratio fallback");
            for (String c : before) {
                assertTrue(!c.contains("2.5x"), "the fixed ratio probe never tries 2.5x: " + c);
            }

            RasterDisplay impl = RasterDisplay.install();
            // RasterDisplay.install() swaps Display's implementation field
            // directly, bypassing the ordinary Display.initImpl() path that
            // also wires com.codename1.io.Util's own implementation
            // reference -- needed here for Util.readInputStream(), which
            // FlutterAssets' manifest load uses. Save/restore so this test
            // leaves no state behind for others.
            Object previousUtilImpl = currentUtilImplementation();
            com.codename1.io.Util.setImplementation(impl);
            try {
                String[] after = FlutterAssets.variantCandidates("assets/icons/material.png", 3);
                assertEquals(
                        java.util.Arrays.asList("assets/icons/material.png", "assets/icons/2.5x/material.png"),
                        java.util.Arrays.asList(after),
                        "now that Display is initialized the manifest must actually be read, "
                                + "finding the 2.5x variant it (and only it) declares");
            } finally {
                com.codename1.io.Util.setImplementation(
                        (com.codename1.impl.CodenameOneImplementation) previousUtilImpl);
                RasterDisplay.uninstall();
            }
        } finally {
            resetManifestState();
        }
    }

    /**
     * Regression: {@code open()} ran its candidate-probing loop even when
     * {@link com.codename1.ui.Display} had not been initialized yet -- unlike
     * {@code variantCandidates()}/{@code manifest()}, which guard exactly this case,
     * nothing stopped {@code open()} from calling
     * {@code Display.getInstance().getResourceAsStream(...)} while the static
     * {@code impl} field was still null, throwing a {@code NullPointerException}
     * instead of answering a clean miss.
     */
    @Test
    void openBeforeDisplayInitIsACleanMissNotANpe() {
        assertTrue(!com.codename1.ui.Display.isInitialized(), "test assumes a headless start");
        FlutterAssets.Resolved r = FlutterAssets.open(FlutterAssetsTest.class, "assets/does/not/exist.png");
        assertNull(r, "no Display implementation exists yet to resolve anything from");
    }

    private static Object currentUtilImplementation() throws Exception {
        Field f = com.codename1.io.Util.class.getDeclaredField("implInstance");
        f.setAccessible(true);
        return f.get(null);
    }

    private static void resetManifestState() throws Exception {
        Field read = FlutterAssets.class.getDeclaredField("manifestRead");
        read.setAccessible(true);
        read.setBoolean(null, false);
        Field manifest = FlutterAssets.class.getDeclaredField("manifest");
        manifest.setAccessible(true);
        manifest.set(null, null);
    }
}
