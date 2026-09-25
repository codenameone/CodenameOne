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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The builders ship only the native theme the runtime will install.
public class NativeThemesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void theIosChainPicksWhatInstallNativeThemeLoads() {
        assertEquals("iOS7Theme", NativeThemes.themeFor(null, null, false));
        assertEquals("iOS7Theme", NativeThemes.themeFor("auto", "26", false));
        assertEquals("iOS7Theme", NativeThemes.themeFor("FLAT", "26", false));
        assertEquals("iOSModernTheme", NativeThemes.themeFor("modern", "26", false));
        assertEquals("iOSModernTheme", NativeThemes.themeFor("liquid", null, false));
        assertEquals("iOSModern27Theme", NativeThemes.themeFor("modern", "27", false));
        assertEquals("iPhoneTheme", NativeThemes.themeFor("legacy", "26", false));
        assertEquals("iPhoneTheme", NativeThemes.themeFor("material", "26", false));
    }

    @Test
    public void macGetsAquaUnlessItAsksForAnIosLook() {
        assertEquals("MacOSAquaTheme", NativeThemes.themeFor("aqua", null, true));
        assertEquals("MacOSAquaTheme", NativeThemes.themeFor("auto", null, true));
        assertEquals("MacOSAquaTheme", NativeThemes.themeFor("native", null, true));
        // The macOS stub never sets a generation, so the modern theme there is 26.
        assertEquals("iOSModernTheme", NativeThemes.themeFor("modern", null, true));
        assertEquals("iOS7Theme", NativeThemes.themeFor("ios7", null, true));
    }

    @Test
    public void onlyTheLoadedThemeSurvivesAndOtherFilesAreUntouched() throws IOException {
        File dir = portDir();
        List<String> removed = NativeThemes.removeUnused(dir, "MacOSAquaTheme", tmp.newFolder("classes"));
        assertEquals(Arrays.asList("iPhoneTheme", "iOS7Theme", "iOSModernTheme", "iOSModern27Theme"), removed);
        assertTrue(new File(dir, "MacOSAquaTheme.res").isFile());
        assertTrue("not a native theme", new File(dir, "CN1Resource.res").isFile());
        assertTrue("not a theme at all", new File(dir, "cn1_globals.m").isFile());
    }

    @Test
    public void aThemeTheApplicationOpensItselfIsKept() throws IOException {
        File dir = portDir();
        File classes = tmp.newFolder("appclasses");
        File cls = new File(classes, "com/example/App.class");
        cls.getParentFile().mkdirs();
        Files.write(cls.toPath(), bytesWith("/iPhoneTheme.res"));
        NativeThemes.removeUnused(dir, "iOS7Theme", classes);
        assertTrue(new File(dir, "iPhoneTheme.res").isFile());
        assertTrue(new File(dir, "iOS7Theme.res").isFile());
        assertFalse(new File(dir, "iOSModernTheme.res").isFile());
    }

    @Test
    public void thePortsOwnReferencesDoNotKeepAnything() throws IOException {
        // IOSImplementation names every theme; that must not count as a use.
        File dir = portDir();
        File classes = tmp.newFolder("merged");
        File impl = new File(classes, "com/codename1/impl/ios/IOSImplementation.class");
        impl.getParentFile().mkdirs();
        Files.write(impl.toPath(), bytesWith("/iPhoneTheme.res /iOSModern27Theme.res"));
        NativeThemes.removeUnused(dir, "iOS7Theme", classes);
        assertFalse(new File(dir, "iPhoneTheme.res").isFile());
        assertFalse(new File(dir, "iOSModern27Theme.res").isFile());
    }

    @Test
    public void anAppThatAsksThePortForItsModernThemeKeepsIt() throws IOException {
        // The screenshot suite installs the theme cn1.nativeThemeResource names,
        // in an app built in the default auto mode. Filtering by the mode alone
        // removed the modern theme, and every dark capture rendered light.
        File dir = portDir();
        File classes = tmp.newFolder("suite");
        File cls = new File(classes, "com/example/tests/DualAppearanceBaseTest.class");
        cls.getParentFile().mkdirs();
        Files.write(cls.toPath(), bytesWith("cn1.nativeThemeResource"));
        NativeThemes.removeUnusedApple(dir, "auto", "27", false, classes);
        assertTrue(new File(dir, "iOS7Theme.res").isFile());
        assertTrue(new File(dir, "iOSModern27Theme.res").isFile());
        assertFalse(new File(dir, "iOSModernTheme.res").isFile());
        assertFalse(new File(dir, "iPhoneTheme.res").isFile());
    }

    @Test
    public void anAppThatDoesNotAskKeepsOnlyTheModeTheme() throws IOException {
        File dir = portDir();
        NativeThemes.removeUnusedApple(dir, "auto", "27", false, tmp.newFolder("plain"));
        assertTrue(new File(dir, "iOS7Theme.res").isFile());
        assertFalse(new File(dir, "iOSModern27Theme.res").isFile());
    }

    @Test
    public void androidKeepsTheResolvedThemeAndTheOneHasNativeThemeProbes() {
        assertEquals(set("android_holo_light"), NativeThemes.androidThemesFor(null, null, null, null, null));
        assertEquals(set("AndroidMaterialTheme", "android_holo_light"),
                NativeThemes.androidThemesFor(null, null, "modern", null, null));
        assertEquals(set("AndroidMaterialTheme", "android_holo_light"),
                NativeThemes.androidThemesFor("Material", null, null, null, null));
        assertEquals(set("androidTheme", "android_holo_light"),
                NativeThemes.androidThemesFor("legacy", null, null, null, null));
        assertEquals(set("androidTheme", "android_holo_light"),
                NativeThemes.androidThemesFor(null, null, null, null, "true"));
        // and.themeMode wins over the cross-platform hint, as it does at run time.
        assertEquals(set("android_holo_light"),
                NativeThemes.androidThemesFor("hololight", null, "modern", null, null));
    }

    @Test
    public void androidRemovesOnlyThePortThemesItWillNotLoad() throws IOException {
        File dir = tmp.newFolder("assets");
        for (String name : NativeThemes.ANDROID_ALL) {
            Files.write(new File(dir, name + ".res").toPath(), new byte[] {1});
        }
        Files.write(new File(dir, "theme.res").toPath(), new byte[] {1});
        List<String> removed = NativeThemes.removeUnusedAndroid(dir,
                NativeThemes.androidThemesFor(null, null, "modern", null, null), tmp.newFolder("c1"));
        assertEquals(Arrays.asList("androidTheme"), removed);
        assertTrue(new File(dir, "AndroidMaterialTheme.res").isFile());
        assertTrue(new File(dir, "android_holo_light.res").isFile());
        assertTrue("the application's own theme", new File(dir, "theme.res").isFile());
    }

    @Test
    public void anAndroidAppThatSetsTheModeItselfKeepsEveryTheme() throws IOException {
        File dir = tmp.newFolder("assets2");
        for (String name : NativeThemes.ANDROID_ALL) {
            Files.write(new File(dir, name + ".res").toPath(), new byte[] {1});
        }
        File classes = tmp.newFolder("c2");
        File cls = new File(classes, "com/example/App.class");
        cls.getParentFile().mkdirs();
        Files.write(cls.toPath(), bytesWith("and.themeMode"));
        assertTrue(NativeThemes.removeUnusedAndroid(dir,
                NativeThemes.androidThemesFor(null, null, null, null, null), classes).isEmpty());
        assertTrue(new File(dir, "androidTheme.res").isFile());
    }

    @Test
    public void aBuildHintDeclarationIsNotAUse() throws IOException {
        // com.codename1.annotations.buildhints names the properties to document them.
        File classes = tmp.newFolder("c3");
        File cls = new File(classes, "com/codename1/annotations/buildhints/Android.class");
        cls.getParentFile().mkdirs();
        Files.write(cls.toPath(), bytesWith("and.themeMode nativeTheme"));
        assertFalse(NativeThemes.namesAny(classes, NativeThemes.ANDROID_MODE_PROPERTIES));
    }

    private static java.util.Set<String> set(String... names) {
        return new java.util.LinkedHashSet<String>(Arrays.asList(names));
    }

    private File portDir() throws IOException {
        File dir = tmp.newFolder();
        for (String name : NativeThemes.ALL) {
            Files.write(new File(dir, name + ".res").toPath(), new byte[] {1});
        }
        Files.write(new File(dir, "CN1Resource.res").toPath(), new byte[] {1});
        Files.write(new File(dir, "cn1_globals.m").toPath(), new byte[] {1});
        return dir;
    }

    private static byte[] bytesWith(String s) {
        byte[] head = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 52};
        byte[] body = s.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[head.length + body.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(body, 0, out, head.length, body.length);
        return out;
    }
}
