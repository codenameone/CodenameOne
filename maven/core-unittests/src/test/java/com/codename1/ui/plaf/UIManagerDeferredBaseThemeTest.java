/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A deferred base theme (UIManager.setDeferredBaseTheme) must be invisible in
/// what an application sees: installed before anything reads the theme, never
/// installed when a theme replaces it outright, and installed first when a theme is
/// layered on it. The Linux and Windows ports defer their native theme this way
/// instead of installing it while the display initializes.
public class UIManagerDeferredBaseThemeTest extends UITestBase {

    /// Counts its runs and installs a base theme with one recognisable key.
    private static final class Base implements Runnable {
        private final UIManager manager;
        int runs;

        Base(UIManager manager) {
            this.manager = manager;
        }

        @Override
        public void run() {
            runs++;
            Hashtable<String, Object> base = new Hashtable<String, Object>();
            base.put("@deferredBaseMarker", "base");
            base.put("name", "DeferredBase");
            base.put("DeferredProbe.fgColor", "112233");
            manager.setThemeProps(base);
        }
    }

    @Test
    public void aReadInstallsTheBaseThemeOnceBeforeAnswering() {
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        assertEquals(0, base.runs, "registering must not install");
        assertEquals("base", m.getThemeConstant("deferredBaseMarker", "missing"),
                "the first read has to see the base theme");
        assertEquals(0x112233, m.getComponentStyle("DeferredProbe").getFgColor());
        assertEquals(1, base.runs, "installed exactly once");
    }

    @Test
    public void everyKindOfReadTriggersIt() {
        String[] kinds = {"style", "selected", "constant", "boolean", "image", "laf", "name", "zoom"};
        for (String kind : kinds) {
            UIManager m = UIManager.createInstance();
            Base base = new Base(m);
            m.setDeferredBaseTheme(base);
            if ("style".equals(kind)) {
                m.getComponentStyle("Label");
            } else if ("selected".equals(kind)) {
                m.getComponentSelectedStyle("Label");
            } else if ("constant".equals(kind)) {
                m.getThemeConstant("x", 0);
            } else if ("boolean".equals(kind)) {
                m.isThemeConstant("x", false);
            } else if ("image".equals(kind)) {
                m.getThemeImageConstant("x");
            } else if ("name".equals(kind)) {
                assertEquals("DeferredBase", m.getThemeName(), "the name of the theme that will be installed");
            } else if ("zoom".equals(kind)) {
                // Zooming must scale the base theme, not an empty table it then replaces.
                m.zoomFonts(1.5f);
            } else {
                m.getLookAndFeel();
            }
            assertEquals(1, base.runs, "a " + kind + " read must install the deferred base theme");
        }
    }

    @Test
    public void aThemeThatReplacesEverythingSupersedesIt() {
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        Hashtable<String, Object> app = new Hashtable<String, Object>();
        app.put("@appMarker", "app");
        m.setThemeProps(app);
        assertEquals("app", m.getThemeConstant("appMarker", "missing"));
        assertEquals("missing", m.getThemeConstant("deferredBaseMarker", "missing"),
                "setThemeProps resets the table, so the base theme must not reappear");
        assertEquals(0, base.runs, "a superseded base theme is never installed");
    }

    @Test
    public void aLayeredThemeGetsTheBaseUnderneath() {
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        Hashtable<String, Object> layer = new Hashtable<String, Object>();
        layer.put("@layerMarker", "layer");
        m.addThemeProps(layer);
        assertEquals(1, base.runs);
        assertEquals("base", m.getThemeConstant("deferredBaseMarker", "missing"));
        assertEquals("layer", m.getThemeConstant("layerMarker", "missing"));
    }

    @Test
    public void aPendingBaseThemeCountsAsInstalled() {
        // FullScreenAdService installs the native theme when none was installed;
        // a deferred one is as good as installed and must not look missing.
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        assertTrue(m.wasThemeInstalled());
        assertEquals(0, base.runs, "asking must not install");
    }

    /// Before the base theme was deferred, a port installed it eagerly while the
    /// display initialized, so app code calling setComponentStyle always ran
    /// AFTER the base was in place. Deferred, the first call into any of these
    /// setters can now run BEFORE the base installs. Each setter must install
    /// the pending base first (the same ensure call the getters use), or the
    /// override it records is layered onto an empty table and then wiped out
    /// -- not merely overwritten -- by setThemePropsImpl's clearStyleCaches the
    /// first time something actually reads the theme.
    @Test
    public void aComponentStyleSetBeforeAnyReadSurvivesTheDeferredBaseInstall() {
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        Style override = new Style();
        override.setFgColor(0xabcdef);
        m.setComponentStyle("Override", override);
        assertEquals(0xabcdef, m.getComponentStyle("Override").getFgColor(),
                "a style set before any getter must survive the deferred base install");
        assertEquals(1, base.runs, "the base theme must still install exactly once");
    }

    @Test
    public void aTypedComponentStyleSetBeforeAnyReadSurvivesTheDeferredBaseInstall() {
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        Style override = new Style();
        override.setFgColor(0xabcdef);
        m.setComponentStyle("Override", override, "press");
        assertEquals(0xabcdef, m.getComponentCustomStyle("Override", "press").getFgColor(),
                "a typed style set before any getter must survive the deferred base install");
        assertEquals(1, base.runs, "the base theme must still install exactly once");
    }

    @Test
    public void aComponentSelectedStyleSetBeforeAnyReadSurvivesTheDeferredBaseInstall() {
        UIManager m = UIManager.createInstance();
        Base base = new Base(m);
        m.setDeferredBaseTheme(base);
        Style override = new Style();
        override.setFgColor(0xabcdef);
        m.setComponentSelectedStyle("Override", override);
        assertEquals(0xabcdef, m.getComponentSelectedStyle("Override").getFgColor(),
                "a selected style set before any getter must survive the deferred base install");
        assertEquals(1, base.runs, "the base theme must still install exactly once");
    }
}
