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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code res/values/colors.xml} feeds the generated Android theme: every color
 * in it becomes an {@code android:<name>} item, which aapt2 resolves as
 * {@code android:attr/<name>}.
 *
 * <p>A name that is not an Android theme attribute therefore fails resource
 * linking with "style attribute 'android:attr/<name>' not found" and produces
 * no APK at all. That is issue #5837: the adaptive launcher icon background
 * was written into that file under the name {@code ic_launcher_background},
 * which is not an attribute, so {@code android.enableAdaptiveIcons=true} broke
 * the build outright.</p>
 */
class AndroidAdaptiveIconBackgroundTest {

    private static final Set<String> FRAMEWORK_ATTRIBUTES = new HashSet<String>(Arrays.asList(
            "colorPrimary", "statusBarColor", "windowLightStatusBar", "windowActionBar"));

    @Test
    void backgroundColorStaysOutOfTheGeneratedTheme(@TempDir File valsDir) throws Exception {
        AndroidGradleBuilder.writeAdaptiveIconBackgroundColor(valsDir, "#000000");

        File ownFile = new File(valsDir, "ic_launcher_background.xml");
        assertTrue(ownFile.exists(), "the background color needs a values file of its own");
        assertTrue(read(ownFile).contains("<color name=\"ic_launcher_background\">#000000</color>"),
                "the requested background color is what gets written");
        assertFalse(new File(valsDir, "colors.xml").exists(),
                "colors.xml is the developer's theme-attribute file, not somewhere to stash our own resources");

        assertEquals("", items(new File(valsDir, "colors.xml")),
                "nothing the builder generates for itself may be promoted into the theme");
    }

    @Test
    void developerColorsAreStillPromotedToTheTheme(@TempDir File valsDir) throws Exception {
        File colors = write(valsDir, "<color name=\"colorPrimary\">#ff00ff00</color>\n"
                + "    <color name=\"statusBarColor\">#80ff0000</color>");

        String items = items(colors);
        assertTrue(items.contains("<item name=\"android:colorPrimary\">@color/colorPrimary</item>"),
                "documented behavior: a color name that is a theme attribute reaches the theme");
        assertTrue(items.contains("<item name=\"android:statusBarColor\">@color/statusBarColor</item>"),
                "documented behavior: a color name that is a theme attribute reaches the theme");
    }

    @Test
    void aNameThatIsNotAnAttributeIsLeftOutAndReported(@TempDir File valsDir) throws Exception {
        // The failure this whole class exists for. An Android Studio project
        // template declares ic_launcher_background in colors.xml, so a
        // developer who copies one in would otherwise lose the entire build to
        // an aapt2 message that never mentions colors.xml.
        File colors = write(valsDir, "<color name=\"colorPrimary\">#ff00ff00</color>\n"
                + "    <color name=\"ic_launcher_background\">#000000</color>");

        AndroidGradleBuilder.ThemeColors themeColors =
                AndroidGradleBuilder.buildThemeColorItems(colors, FRAMEWORK_ATTRIBUTES);

        assertFalse(themeColors.items.contains("ic_launcher_background"),
                "a name aapt2 cannot resolve as android:attr must not reach the theme");
        assertTrue(themeColors.items.contains("<item name=\"android:colorPrimary\">@color/colorPrimary</item>"),
                "the developer's real theme attributes are unaffected");
        assertEquals(Arrays.asList("ic_launcher_background"), themeColors.skipped,
                "what was left out is reported, so a misspelled attribute is not silently ignored");
    }

    @Test
    void aBooleanValuedAttributeIsWrittenLiterally(@TempDir File valsDir) throws Exception {
        // android:windowLightStatusBar is a boolean theme item. "@color/x"
        // would not resolve to a boolean, so the value passes through as-is.
        File colors = write(valsDir, "<color name=\"windowLightStatusBar\">true</color>");

        assertTrue(items(colors).contains("<item name=\"android:windowLightStatusBar\">true</item>"),
                "a true/false value is a boolean theme item, not a color reference");
    }

    @Test
    void anUnreadablePlatformLeavesEveryNameAlone(@TempDir File valsDir) throws Exception {
        // With no platform to check against, dropping names would silently
        // strip a developer's theming. Passing them through keeps the behavior
        // that shipped before the check existed.
        File colors = write(valsDir, "<color name=\"colorPrimary\">#ff00ff00</color>\n"
                + "    <color name=\"somethingUnknown\">#000000</color>");

        AndroidGradleBuilder.ThemeColors themeColors =
                AndroidGradleBuilder.buildThemeColorItems(colors, null);

        assertTrue(themeColors.items.contains("<item name=\"android:somethingUnknown\">@color/somethingUnknown</item>"),
                "an unchecked build passes every name through, as it always did");
        assertTrue(themeColors.skipped.isEmpty(), "nothing was checked, so nothing was skipped");
    }

    @Test
    void aDeveloperOwnedBackgroundColorIsNotDuplicated(@TempDir File valsDir) throws Exception {
        // Two files declaring one color name is a duplicate resource, which
        // fails linking just as surely as the attribute promotion does.
        write(valsDir, "<color name=\"ic_launcher_background\">#123456</color>");

        AndroidGradleBuilder.writeAdaptiveIconBackgroundColor(valsDir, "#000000");

        assertFalse(new File(valsDir, "ic_launcher_background.xml").exists(),
                "the developer's own declaration wins rather than colliding with ours");
    }

    @Test
    void theAttributeSetComesFromThePlatformJar(@TempDir File sdkDir) throws Exception {
        File platform = new File(sdkDir, "platforms/android-99");
        assertTrue(platform.mkdirs(), "test platform directory");
        AndroidAttrJar.write(new File(platform, "android.jar"), "colorPrimary", "statusBarColor");

        Set<String> attributes = AndroidGradleBuilder.frameworkThemeAttributes(sdkDir);
        assertTrue(attributes.contains("colorPrimary"), "read android.R$attr out of the platform jar");
        assertTrue(attributes.contains("statusBarColor"), "read android.R$attr out of the platform jar");
        assertFalse(attributes.contains("ic_launcher_background"),
                "the platform is the authority on what is an attribute, not a list we maintain");
    }

    @Test
    void anSdkWithNoPlatformAnswersNull(@TempDir File sdkDir) {
        assertEquals(null, AndroidGradleBuilder.frameworkThemeAttributes(sdkDir),
                "no platform means no answer, which is what makes the caller pass names through unchecked");
    }

    private static String items(File colorsFile) throws Exception {
        return AndroidGradleBuilder.buildThemeColorItems(colorsFile, FRAMEWORK_ATTRIBUTES).items;
    }

    private static File write(File valsDir, String colorElements) throws IOException {
        File colors = new File(valsDir, "colors.xml");
        Files.write(colors.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "    " + colorElements + "\n"
                + "</resources>\n").getBytes(StandardCharsets.UTF_8));
        return colors;
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
