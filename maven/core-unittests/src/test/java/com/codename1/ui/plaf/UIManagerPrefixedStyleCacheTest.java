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
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prefixed styles (press, dis, or any custom type) are cached on prefix + id.
 * A style installed programmatically can be the base such a style derives from,
 * so the cache has to be dropped when one is installed -- otherwise the prefixed
 * lookup keeps answering with the prototype built from the previous base and the
 * newly installed style is silently discarded.
 */
public class UIManagerPrefixedStyleCacheTest extends UITestBase {

    private static Hashtable derivedFromBase() {
        Hashtable theme = new Hashtable();
        theme.put("Base.fgColor", "111111");
        theme.put("Child.press#derive", "Base");
        return theme;
    }

    @Test
    public void installedBaseIsVisibleToAPrefixedStyleResolvedEarlier() {
        UIManager manager = UIManager.getInstance();
        manager.setThemeProps(derivedFromBase());

        // Resolve first, so the prefixed prototype is cached against the old base.
        assertEquals(0x111111, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "precondition: the prefixed style derives its colour from Base");

        Style replacement = new Style();
        replacement.setFgColor(0x222222);
        manager.setComponentStyle("Base", replacement);

        assertEquals(0x222222, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "a prefixed style must rebuild through the newly installed base");
    }

    @Test
    public void installedSelectedBaseIsVisibleToAPrefixedStyleResolvedEarlier() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = derivedFromBase();
        theme.put("Child.press#derive", "Base.sel#");
        theme.put("Base.sel#fgColor", "111111");
        manager.setThemeProps(theme);

        assertEquals(0x111111, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "precondition: the prefixed style derives from the selected Base");

        Style replacement = new Style();
        replacement.setFgColor(0x333333);
        manager.setComponentSelectedStyle("Base", replacement);

        assertEquals(0x333333, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "a prefixed style must rebuild through the newly installed selected base");
    }

    /// The caches are bounded, so an application generating uiids or style types
    /// at run time cannot retain a prototype, a key bucket and a prefix string
    /// per combination for the lifetime of the theme. What matters as much as the
    /// bound is that the answers stay right once it is reached: past the limit
    /// the lookup rebuilds every time, exactly as it did before the cache.
    @Test
    public void styleLookupStaysCorrectPastTheCacheBound() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Base.fgColor", "111111");
        for (int i = 0; i < 700; i++) {
            // A generated id on a fixed type, and a generated TYPE on a fixed
            // id: the first fills the prototype map, the second fills the prefix
            // buckets. A prefixed style inherits nothing implicitly, so each one
            // has to name its base or it legitimately resolves to the default.
            theme.put("Gen" + i + ".press#derive", "Base");
            theme.put("Base.t" + i + "#derive", "Base");
        }
        manager.setThemeProps(theme);

        // Well past KEY_CACHE_LIMIT, with a distinct id AND a distinct type, so
        // both the prototype map and the prefix buckets are pushed over.
        for (int i = 0; i < 700; i++) {
            assertEquals(0x111111,
                    manager.getComponentCustomStyle("Gen" + i, "press").getFgColor(),
                    "prefixed style " + i + " must resolve correctly past the bound");
        }
        for (int i = 0; i < 700; i++) {
            assertEquals(0x111111,
                    manager.getComponentCustomStyle("Base", "t" + i).getFgColor(),
                    "generated type " + i + " must resolve correctly past the bound");
        }
    }

    @Test
    public void mutatingAnInstalledBaseIsVisibleToAPrefixedStyle() {
        UIManager manager = UIManager.getInstance();
        manager.setThemeProps(derivedFromBase());

        Style installed = new Style();
        installed.setFgColor(0x222222);
        manager.setComponentStyle("Base", installed);

        assertEquals(0x222222, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "precondition: the prefixed style follows the installed base");

        // The caller still owns this object and can change it without going
        // through UIManager at all, so nothing can invalidate a cached copy.
        installed.setFgColor(0x555555);

        assertEquals(0x555555, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "a prefixed style must follow later mutations of an installed base");
    }

    @Test
    public void installedTypedStyleIsVisibleToAPrefixedStyleResolvedEarlier() {
        UIManager manager = UIManager.getInstance();
        manager.setThemeProps(derivedFromBase());

        assertEquals(0x111111, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "precondition: the prefixed style derives its colour from Base");

        Style replacement = new Style();
        replacement.setFgColor(0x444444);
        // The three-argument setter writes the same map through a typed key.
        manager.setComponentStyle("Base", replacement, "");

        assertEquals(0x444444, manager.getComponentCustomStyle("Child", "press").getFgColor(),
                "a prefixed style must rebuild after the typed setter installs a base");
    }
}
