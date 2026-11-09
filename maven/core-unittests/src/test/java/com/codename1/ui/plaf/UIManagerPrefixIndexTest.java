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
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies observable prefix-query semantics and invalidation against real UIManager themes.
public class UIManagerPrefixIndexTest extends UITestBase {
    /// Queries every prefix of actual keys plus near misses, including Unicode and
    /// overlapping identifiers. The oracle is the former public-theme prefix contract,
    /// independent of the index order, binary-search boundaries and result cache.
    @FormTest
    public void allPrefixesAndNearMissesMatchThemeDeclarations() throws Exception {
        assertTrue(display.isEdt(), "Theme operations must run on the EDT");
        UIManager manager = UIManager.getInstance();
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        String[] ids = {"A", "AA", "AB", "A.B", "Button", "ButtonExtra", "$DarkButton", "Caff\u00E8", "\u65E5\u672C\u8A9E", "\uD83D\uDE00", "\uFFFF"};
        for (String id : ids) {
            theme.put(id + ".fgColor", "112233");
            theme.put(id + ".hover#bgColor", "445566");
        }
        manager.setThemeProps(theme);
        Set<String> prefixes = new HashSet<String>();
        prefixes.add("");
        prefixes.add("~no-definition");
        for (String key : manager.getThemeProps().keySet()) {
            for (int length = 1; length <= key.length(); length++) {
                String prefix = key.substring(0, length);
                prefixes.add(prefix);
                prefixes.add(prefix + "\uFFFF");
            }
        }
        for (String prefix : prefixes) {
            boolean expected = false;
            for (String key : manager.getThemeProps().keySet()) {
                if (key.startsWith(prefix)) {
                    expected = true;
                    break;
                }
            }
            assertEquals(expected, query(manager, prefix), prefix);
            assertEquals(expected, query(manager, prefix), "cached: " + prefix);
        }
    }

    /// A replacement must discard both positive and negative answers from the old theme.
    @FormTest
    public void themeReplacementDropsPositiveAndNegativeAnswers() throws Exception {
        assertTrue(display.isEdt(), "Theme operations must run on the EDT");
        UIManager manager = UIManager.getInstance();
        Hashtable<String, Object> first = new Hashtable<String, Object>();
        first.put("ColdFirst.hover#fgColor", "123456");
        manager.setThemeProps(first);
        assertTrue(query(manager, "ColdFirst.hover#"));
        assertFalse(query(manager, "ColdSecond.hover#"));
        Hashtable<String, Object> second = new Hashtable<String, Object>();
        second.put("ColdSecond.hover#fgColor", "abcdef");
        manager.setThemeProps(second);
        assertFalse(query(manager, "ColdFirst.hover#"));
        assertTrue(query(manager, "ColdSecond.hover#"));
    }

    /// Inline parsing adds a declaration without necessarily changing theme generation;
    /// its existing explicit invalidation must make the next query see the new state.
    @FormTest
    public void inlineParsingMakesPreviouslyMissingStateVisible() throws Exception {
        assertTrue(display.isEdt(), "Theme operations must run on the EDT");
        UIManager manager = UIManager.getInstance();
        manager.setThemeProps(new Hashtable<String, Object>());
        assertFalse(query(manager, "ColdParsed.hover#"));
        manager.parseComponentCustomStyle(null, null, "ColdParsed", "hover", "fgColor:123456");
        assertTrue(query(manager, "ColdParsed.hover#"));
        assertTrue(manager.hasComponentCustomStyle("ColdParsed", "hover"));
    }

    /// Calls the real private resolver, avoiding a duplicate implementation in this test.
    private static boolean query(UIManager manager, String prefix) throws Exception {
        Method method = UIManager.class.getDeclaredMethod("hasStyleDefinition", String.class);
        method.setAccessible(true);
        return ((Boolean) method.invoke(manager, prefix)).booleanValue();
    }
}
