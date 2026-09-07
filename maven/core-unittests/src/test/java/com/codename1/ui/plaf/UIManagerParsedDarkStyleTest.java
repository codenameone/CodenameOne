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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * hasStyleDefinition memoises "does the theme define this id" for a whole theme,
 * and indexes the $Dark keys alongside it. Both are keyed on themeGeneration,
 * which only a wholesale theme replacement bumps -- but parseStyle writes
 * straight into themeProps, so anything it adds has to invalidate them by hand.
 */
public class UIManagerParsedDarkStyleTest extends UITestBase {

    @AfterEach
    public void resetDarkMode() {
        display.setDarkMode(null);
    }

    @Test
    public void aDarkOverrideAddedByParsingIsSeenByAStyleResolvedLater() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "111111");
        theme.put("Label.fgColor", "222222");
        manager.setThemeProps(theme);
        display.setDarkMode(Boolean.TRUE);

        // Resolve something first: this builds the index, which records that the
        // theme carries no $Dark entries at all.
        assertEquals(0x111111, manager.getComponentStyle("Button").getFgColor(),
                "precondition: no dark override is defined yet");

        // The public parse API writes straight into themeProps.
        manager.parseComponentStyle(null, null, "$DarkLabel", "fgColor:333333");

        assertEquals(0x333333, manager.getComponentStyle("Label").getFgColor(),
                "a $Dark override added by parsing must be seen by an id resolved afterwards");
    }
}
