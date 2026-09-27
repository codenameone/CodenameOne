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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Where the JavaScript port's build hints go: the launcher, the translator, and the choice of
/// native themes the bundle carries.
class JavaScriptBuildHintsTest {

    private static BuildRequest request(String... keyValues) {
        BuildRequest r = new BuildRequest();
        for (int i = 0; i < keyValues.length; i += 2) {
            r.putArgument(keyValues[i], keyValues[i + 1]);
        }
        return r;
    }

    private static Set<String> ship(BuildRequest r) {
        return JavaScriptBuildHints.themesToShip(r, Collections.<String>emptySet());
    }

    private static Set<String> set(String... names) {
        return new HashSet<String>(Arrays.asList(names));
    }

    @Test
    void launcherPublishesOnlyTheHintsThatAreSet() {
        String src = JavaScriptBuildHints.launcherProperties(request(
                "nativeTheme", "native", "javascript.titleBar", "html", "unrelated.hint", "x"));
        assertTrue(src.contains("Display.getInstance().setProperty(\"nativeTheme\", \"native\");"), src);
        assertTrue(src.contains("Display.getInstance().setProperty(\"javascript.titleBar\", \"html\");"), src);
        assertFalse(src.contains("unrelated.hint"), src);
        assertFalse(src.contains("javascript.textSelection"), src);
    }

    @Test
    void launcherValuesAreJavaStringLiterals() {
        String src = JavaScriptBuildHints.launcherProperties(request("javascript.native.theme", "a\"b\\c\n"));
        assertTrue(src.contains("\"a\\\"b\\\\c\\n\""), src);
    }

    @Test
    void translatorGetsTheTitleAndOnlyTheOptOutsThatWereSet() {
        BuildRequest r = request();
        r.setDisplayName("My App");
        assertEquals(Collections.singletonList("-Dcodename1.javascript.title=My App"),
                JavaScriptBuildHints.translatorOptions(r));

        r = request("javascript.allowBrowserTranslation", "TRUE", "javascript.darkreaderLock", "false");
        List<String> opts = JavaScriptBuildHints.translatorOptions(r);
        assertTrue(opts.contains("-Dcodename1.javascript.allowBrowserTranslation=true"), opts.toString());
        assertTrue(opts.contains("-Dcodename1.javascript.darkreaderLock=false"), opts.toString());
    }

    @Test
    void noHintsShipsOnlyTheLegacyPair() {
        assertEquals(set("iOS7Theme", "android_holo_light"), ship(request()));
    }

    @Test
    void nativeShipsTheModernMobileThemesAndEveryDesktopTheme() {
        assertEquals(set("iOSModernTheme", "AndroidMaterialTheme",
                "WindowsFluentTheme", "MacOSAquaTheme", "GnomeAdwaitaTheme"),
                ship(request("nativeTheme", "native")));
    }

    @Test
    void modernStaysOffTheDesktopThemes() {
        assertEquals(set("iOSModernTheme", "AndroidMaterialTheme"), ship(request("nativeTheme", "modern")));
    }

    @Test
    void aPinnedDesktopThemeShipsOnlyThatDesktopTheme() {
        assertEquals(set("iOSModernTheme", "AndroidMaterialTheme", "MacOSAquaTheme"),
                ship(request("nativeTheme", "native", "javascript.desktopTheme", "aqua")));
        assertEquals(set("iOSModernTheme", "AndroidMaterialTheme"),
                ship(request("nativeTheme", "native", "javascript.desktopTheme", "none")));
    }

    @Test
    void anExplicitResourceShipsOnlyThatTheme() {
        assertEquals(set("WindowsFluentTheme"),
                ship(request("javascript.native.theme", "/WindowsFluentTheme.res", "nativeTheme", "native")));
        // Not one of ours: the port falls back to the legacy pair if it cannot open it.
        assertEquals(set("iOS7Theme", "android_holo_light"),
                ship(request("javascript.native.theme", "/mytheme.res")));
    }

    @Test
    void platformModesAreHonoured() {
        assertEquals(set("iPhoneTheme", "androidTheme"),
                ship(request("ios.themeMode", "legacy", "and.themeMode", "legacy")));
        assertEquals(set("iOS7Theme", "android_holo_light"), ship(request("nativeTheme", "legacy")));
    }

    @Test
    void whatTheApplicationNamesIsKept() {
        Set<String> refs = new LinkedHashSet<String>();
        JavaScriptBuildHints.scanBytes("xx/GnomeAdwaitaTheme.resyy".getBytes(StandardCharsets.ISO_8859_1), refs);
        JavaScriptBuildHints.scanBytes("cn1.modernThemeResource".getBytes(StandardCharsets.ISO_8859_1), refs);
        Set<String> shipped = JavaScriptBuildHints.themesToShip(request(), refs);
        assertTrue(shipped.containsAll(set("GnomeAdwaitaTheme", "iOSModernTheme", "AndroidMaterialTheme")),
                shipped.toString());
    }

    @Test
    void everyShippedNameIsAKnownTheme() {
        String[][] combos = {
            {}, {"nativeTheme", "native"}, {"nativeTheme", "modern"}, {"nativeTheme", "legacy"},
            {"nativeTheme", "native", "javascript.desktopTheme", "fluent"},
            {"ios.themeMode", "flat", "and.themeMode", "holo"},
        };
        for (String[] combo : combos) {
            for (String name : ship(request(combo))) {
                assertTrue(JavaScriptBuildHints.ALL_THEMES.contains(name), name);
            }
        }
    }
}
