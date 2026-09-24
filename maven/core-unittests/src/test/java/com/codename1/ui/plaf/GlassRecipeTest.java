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
 * Pins the named glass materials, and above all that the names the SHIPPED
 * themes ask for actually exist.
 *
 * <p>{@code GlassRecipe.named} answers the panel recipe for anything it does not
 * recognise. That is the right runtime behaviour -- a theme must not be able to
 * crash an application by misspelling a material -- but it means a typo in a
 * theme is completely silent: {@code ToolbarGlassRecipe: "chrome72"} compiles,
 * ships, and paints the toolbar with the panel material, which is a heavy wash
 * plus refraction and a specular rim where the bar should be nearly
 * pass-through. Nothing reports it. {@link #everyRecipeNamedByAShippedThemeResolves}
 * is the check that does.</p>
 */
public class GlassRecipeTest extends UITestBase {

    /** Every recipe name the lookup is documented to accept. */
    private static final Set<String> KNOWN = new TreeSet<String>();
    static {
        KNOWN.add("blur");
        KNOWN.add("chrome");
        KNOWN.add("pill");
        KNOWN.add("panel");
        KNOWN.add("chrome27");
        KNOWN.add("pill27");
        KNOWN.add("panel27");
    }

    @Test
    public void everyDocumentedNameResolvesToItsOwnRecipe() {
        // NOT an assertNotNull sweep. named() answers the panel recipe for
        // anything it does not recognise, and panel is not null -- so a non-null
        // assertion passes even with every chrome27/pill27/panel27 branch deleted
        // from the lookup, which is exactly the regression that would silently
        // paint the toolbar with the wrong material. Measured: with the chrome27
        // branch removed, all six tests in this class still passed. Each name
        // must therefore be compared against the recipe it is supposed to return,
        // on every parameter.
        for (int i = 0; i < 2; i++) {
            boolean dark = i == 1;
            assertSameRecipe(GlassRecipe.plainBlur(), GlassRecipe.named("blur", dark),
                    "blur/" + appearance(dark));
            assertSameRecipe(GlassRecipe.liquidChrome(dark), GlassRecipe.named("chrome", dark),
                    "chrome/" + appearance(dark));
            assertSameRecipe(GlassRecipe.liquidPill(dark), GlassRecipe.named("pill", dark),
                    "pill/" + appearance(dark));
            assertSameRecipe(GlassRecipe.liquidPanel(dark), GlassRecipe.named("panel", dark),
                    "panel/" + appearance(dark));
            assertSameRecipe(GlassRecipe.liquidChrome27(dark), GlassRecipe.named("chrome27", dark),
                    "chrome27/" + appearance(dark));
            assertSameRecipe(GlassRecipe.liquidPill27(dark), GlassRecipe.named("pill27", dark),
                    "pill27/" + appearance(dark));
            assertSameRecipe(GlassRecipe.liquidPanel27(dark), GlassRecipe.named("panel27", dark),
                    "panel27/" + appearance(dark));
        }
    }

    @Test
    public void everyKnownNameIsDistinguishableFromTheFallback() {
        // The dual of the test above, and what makes it airtight: if a name
        // happened to be materially equal to the panel fallback, comparing it to
        // its factory could not tell "the branch exists" from "the branch is
        // gone". Every name except panel itself must differ from the fallback in
        // at least one parameter.
        for (int i = 0; i < 2; i++) {
            boolean dark = i == 1;
            GlassRecipe fallback = GlassRecipe.named("no-such-material", dark);
            for (String name : KNOWN) {
                if ("panel".equals(name)) {
                    continue;
                }
                assertTrue(differs(fallback, GlassRecipe.named(name, dark)),
                        name + "/" + appearance(dark) + " is materially identical to the"
                                + " unknown-name fallback, so no test can prove its lookup"
                                + " branch is still wired up");
            }
        }
    }

    @Test
    public void unknownNamesFallBackToPanel() {
        // Documented behaviour, pinned so the fallback cannot be "fixed" into a
        // throw without someone deciding to -- a theme must not crash an app.
        for (int i = 0; i < 2; i++) {
            boolean dark = i == 1;
            assertEquals(GlassRecipe.liquidPanel(dark).getKind(),
                    GlassRecipe.named("no-such-material", dark).getKind(),
                    "an unknown material name should fall back to the panel recipe");
        }
    }

    @Test
    public void iOS27KeepsTheKindOfTheMaterialItRetunes() {
        // The 27 variants are the SAME materials with different constants, so a
        // caller switching generations must not get a different Kind -- the only
        // thing Component reads off the kind is "is this a plain blur".
        for (int i = 0; i < 2; i++) {
            boolean dark = i == 1;
            assertEquals(GlassRecipe.liquidChrome(dark).getKind(),
                    GlassRecipe.liquidChrome27(dark).getKind(), "chrome27 kind");
            assertEquals(GlassRecipe.liquidPill(dark).getKind(),
                    GlassRecipe.liquidPill27(dark).getKind(), "pill27 kind");
            assertEquals(GlassRecipe.liquidPanel(dark).getKind(),
                    GlassRecipe.liquidPanel27(dark).getKind(), "panel27 kind");
        }
    }

    @Test
    public void lightChromeAndPillAreMeasuredUnchangedInIOS27() {
        // Not an accident and not a stub: fitting the material against the
        // committed ios-27-metal goldens returned the iOS 26 constants back to
        // within 1.3% for both of these, so they are deliberately shared. Pinned
        // because "27 equals 26 here" is a MEASUREMENT -- if someone later edits
        // one of the four numbers, that edit has to be a deliberate re-measure
        // rather than a drive-by.
        assertSameMaterial(GlassRecipe.liquidChrome(false), GlassRecipe.liquidChrome27(false),
                "light chrome");
        assertSameMaterial(GlassRecipe.liquidPill(false), GlassRecipe.liquidPill27(false),
                "light pill");
    }

    @Test
    public void darkMaterialsActuallyMovedInIOS27() {
        // The dual of the test above: dark is where iOS 27 changed, so a 27 dark
        // recipe that still equals its 26 counterpart means the retune was lost.
        assertDifferentMaterial(GlassRecipe.liquidChrome(true), GlassRecipe.liquidChrome27(true),
                "dark chrome");
        assertDifferentMaterial(GlassRecipe.liquidPill(true), GlassRecipe.liquidPill27(true),
                "dark pill");
        assertDifferentMaterial(GlassRecipe.liquidPanel(true), GlassRecipe.liquidPanel27(true),
                "dark panel");
    }

    @Test
    public void onlyIOS27DarkChromeAndPillAreCurved() {
        // The curve term is what distinguishes iOS 27's dark chrome and pill,
        // whose measured residual against the best affine fit is a parabola in
        // backdrop luma. Everywhere else the material is affine and the recipe must
        // say so exactly -- including the dark panel, where the curve was measured
        // on device and made every affected tile worse (see liquidPanel27).
        for (int i = 0; i < 2; i++) {
            boolean dark = i == 1;
            assertEquals(0f, GlassRecipe.plainBlur().getCurve(), 0f, "blur curve");
            assertEquals(0f, GlassRecipe.liquidChrome(dark).getCurve(), 0f, "chrome curve");
            assertEquals(0f, GlassRecipe.liquidPill(dark).getCurve(), 0f, "pill curve");
            assertEquals(0f, GlassRecipe.liquidPanel(dark).getCurve(), 0f, "panel curve");
        }
        assertEquals(0f, GlassRecipe.liquidChrome27(false).getCurve(), 0f, "light chrome27 curve");
        assertEquals(0f, GlassRecipe.liquidPill27(false).getCurve(), 0f, "light pill27 curve");
        assertEquals(0f, GlassRecipe.liquidPanel27(false).getCurve(), 0f, "light panel27 curve");
        assertTrue(GlassRecipe.liquidChrome27(true).getCurve() != 0f, "dark chrome27 lost its curve");
        assertTrue(GlassRecipe.liquidPill27(true).getCurve() != 0f, "dark pill27 lost its curve");
        assertEquals(0f, GlassRecipe.liquidPanel27(true).getCurve(), 0f, "dark panel27 curve");
    }

    @Test
    public void onlyIOS27PanelAndPillDrawTheEdgeOutline() {
        // The dark edge line is new in iOS 27 -- the iOS 26 captures have none --
        // and it is measured on the panel and pill tiles in BOTH appearances.
        // Nothing measures the chrome bar's sides, so it gets none rather than a
        // guess. Keeping every other recipe at zero is what keeps the iOS 26
        // renders, and the ports that ignore the parameter, unchanged.
        for (int i = 0; i < 2; i++) {
            boolean dark = i == 1;
            assertEquals(0f, GlassRecipe.plainBlur().getOutline(), 0f, "blur outline");
            assertEquals(0f, GlassRecipe.liquidChrome(dark).getOutline(), 0f, "chrome outline");
            assertEquals(0f, GlassRecipe.liquidPill(dark).getOutline(), 0f, "pill outline");
            assertEquals(0f, GlassRecipe.liquidPanel(dark).getOutline(), 0f, "panel outline");
            assertEquals(0f, GlassRecipe.liquidChrome27(dark).getOutline(), 0f, "chrome27 outline");
            assertTrue(GlassRecipe.liquidPill27(dark).getOutline() > 0f, "pill27 lost its outline");
            assertTrue(GlassRecipe.liquidPanel27(dark).getOutline() > 0f, "panel27 lost its outline");
            assertTrue(GlassRecipe.liquidPanel27(dark).getOutline() <= 1f, "panel27 outline is a 0..1 strength");
        }
    }

    @Test
    public void everyRecipeNamedByAShippedThemeResolves() throws Exception {
        String[] themes = {"iOSModernTheme.res", "iOSModern27Theme.res"};
        int checked = 0;
        for (String theme : themes) {
            Hashtable table = loadTheme(theme);
            if (table == null) {
                continue;
            }
            for (Enumeration e = table.keys(); e.hasMoreElements();) {
                String key = String.valueOf(e.nextElement());
                if (!key.endsWith("GlassRecipe") && !key.endsWith("glassRecipeDefault")) {
                    continue;
                }
                String value = String.valueOf(table.get(key)).trim();
                assertTrue(KNOWN.contains(value),
                        theme + " asks for glass material '" + value + "' via " + key
                                + ", which GlassRecipe.named does not know -- it would"
                                + " silently fall back to the panel material. Known: " + KNOWN);
                checked++;
            }
        }
        // A check satisfiable by "nothing happened" is no check: if the themes are
        // not built the test returns above, but if they ARE built and name no
        // materials at all then the constants were lost from the theme.
        if (locateNativeTheme(themes[0]) != null) {
            assertTrue(checked > 0,
                    "the shipped iOS themes named no glass materials at all, so the"
                            + " *GlassRecipe constants were dropped from the theme");
        }
    }

    private static String appearance(boolean dark) {
        return dark ? "dark" : "light";
    }

    /// Full identity: kind and all eight material and optics parameters.
    private static void assertSameRecipe(GlassRecipe expected, GlassRecipe actual, String what) {
        assertNotNull(actual, what + " did not resolve");
        assertEquals(expected.getKind(), actual.getKind(), what + " kind");
        assertEquals(expected.getSaturation(), actual.getSaturation(), 0.0001f, what + " saturation");
        assertEquals(expected.getScale(), actual.getScale(), 0.0001f, what + " scale");
        assertEquals(expected.getOffset(), actual.getOffset(), 0.0001f, what + " offset");
        assertEquals(expected.getRefraction(), actual.getRefraction(), 0.0001f, what + " refraction");
        assertEquals(expected.getSpecular(), actual.getSpecular(), 0.0001f, what + " specular");
        assertEquals(expected.getCurve(), actual.getCurve(), 0.0001f, what + " curve");
        assertEquals(expected.getCurveMid(), actual.getCurveMid(), 0.0001f, what + " curveMid");
        assertEquals(expected.getOutline(), actual.getOutline(), 0.0001f, what + " outline");
    }

    private static boolean differs(GlassRecipe a, GlassRecipe b) {
        return a.getKind() != b.getKind()
                || a.getSaturation() != b.getSaturation()
                || a.getScale() != b.getScale()
                || a.getOffset() != b.getOffset()
                || a.getRefraction() != b.getRefraction()
                || a.getSpecular() != b.getSpecular()
                || a.getCurve() != b.getCurve()
                || a.getCurveMid() != b.getCurveMid()
                || a.getOutline() != b.getOutline();
    }

    private static void assertSameMaterial(GlassRecipe a, GlassRecipe b, String what) {
        assertEquals(a.getSaturation(), b.getSaturation(), 0.0001f, what + " saturation");
        assertEquals(a.getScale(), b.getScale(), 0.0001f, what + " scale");
        assertEquals(a.getOffset(), b.getOffset(), 0.0001f, what + " offset");
        assertEquals(a.getCurve(), b.getCurve(), 0.0001f, what + " curve");
    }

    private static void assertDifferentMaterial(GlassRecipe a, GlassRecipe b, String what) {
        boolean moved = a.getSaturation() != b.getSaturation()
                || a.getScale() != b.getScale()
                || a.getOffset() != b.getOffset();
        assertTrue(moved, what + " is identical to its iOS 26 counterpart, but iOS 27"
                + " was measured as having changed it");
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
