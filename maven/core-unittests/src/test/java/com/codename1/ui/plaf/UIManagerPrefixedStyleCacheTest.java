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
