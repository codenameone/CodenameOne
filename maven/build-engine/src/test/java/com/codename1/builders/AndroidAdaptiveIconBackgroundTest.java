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
    void theGeneratedIconBackgroundIsNeverPromoted(@TempDir File valsDir) throws Exception {
        // Issue #5837 itself, and it must hold with no platform to check
        // against -- which is every build whose compile SDK this process
        // cannot read, including the build server. The adaptive icon
        // references this color, so it is an icon resource, not an attribute.
        File colors = write(valsDir, "<color name=\"colorPrimary\">#ff00ff00</color>\n"
                + "    <color name=\"ic_launcher_background\">#000000</color>");

        AndroidGradleBuilder.ThemeColors unchecked =
                AndroidGradleBuilder.buildThemeColorItems(colors, null);

        assertFalse(unchecked.items.contains("ic_launcher_background"),
                "promoting it is the resource-linking failure this whole change exists to stop");
        assertTrue(unchecked.items.contains("<item name=\"android:colorPrimary\">@color/colorPrimary</item>"),
                "every other name is still passed through unchecked");
        assertEquals(Arrays.asList("ic_launcher_background"), unchecked.skipped,
                "and the build log says which color was left out");
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
        assertTrue(themeColors.items.contains("<item name=\"android:colorPrimary\">@color/colorPrimary</item>"),
                "including the ones that are attributes");
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
        platform(sdkDir, "android-36", "colorPrimary", "statusBarColor");

        Set<String> attributes = AndroidGradleBuilder.frameworkThemeAttributes(sdkDir, 36);
        assertTrue(attributes.contains("colorPrimary"), "read android.R$attr out of the platform jar");
        assertTrue(attributes.contains("statusBarColor"), "read android.R$attr out of the platform jar");
        assertFalse(attributes.contains("ic_launcher_background"),
                "the platform is the authority on what is an attribute, not a list we maintain");
    }

    @Test
    void anSdkWithNoPlatformAnswersNull(@TempDir File sdkDir) {
        assertEquals(null, AndroidGradleBuilder.frameworkThemeAttributes(sdkDir, 1),
                "no platform means no answer, which is what makes the caller pass names through unchecked");
    }

    @Test
    void aPlatformOlderThanTheBuildTargetAnswersNull(@TempDir File sdkDir) throws Exception {
        // The build links against a platform this process cannot see, so an
        // attribute introduced after android-35 is absent from what we read
        // and would be dropped although aapt2 resolves it. Refusing to answer
        // is what makes the caller pass every name through unchecked.
        platform(sdkDir, "android-35", "colorPrimary");

        assertEquals(null, AndroidGradleBuilder.frameworkThemeAttributes(sdkDir, 37),
                "evidence older than the compile platform cannot rule a name out");
        assertTrue(AndroidGradleBuilder.frameworkThemeAttributes(sdkDir, 35).contains("colorPrimary"),
                "evidence that reaches the target is conclusive");
    }

    @Test
    void anSdkThisProcessCannotReadAnswersNull() {
        // The build server runs gradle inside a container with its own SDK.
        assertEquals(null, AndroidGradleBuilder.frameworkThemeAttributes(null, 1),
                "no readable SDK means no answer");
    }

    @Test
    void aMinorVersionedPlatformReducesToItsApiLevel(@TempDir File sdkDir) throws Exception {
        // android-37.2 is API 37. Gathering the digits instead answers 372,
        // which compares greater than every level there is.
        assertEquals(37, AndroidGradleBuilder.platformApiLevel("android-37.2"));
        assertEquals(35, AndroidGradleBuilder.platformApiLevel("android-35"));
        assertEquals(-1, AndroidGradleBuilder.platformApiLevel("android-UpsideDownCake"));
        assertEquals(-1, AndroidGradleBuilder.platformApiLevel("build-tools"));

        platform(sdkDir, "android-37.2", "colorPrimary");
        assertTrue(AndroidGradleBuilder.frameworkThemeAttributes(sdkDir, 37).contains("colorPrimary"),
                "a minor-versioned platform counts as its API level");
    }

    @Test
    void aColorDeclaredAsATypedItemCounts(@TempDir File valsDir) throws Exception {
        // <item type="color" name="x"> is an equally valid color declaration.
        // Missing it would put a second declaration of the same name in
        // another file, which aapt2 rejects as a duplicate resource.
        write(valsDir, "<item type=\"color\" name=\"ic_launcher_background\">#123456</item>");

        assertTrue(AndroidGradleBuilder.declaresColor(new File(valsDir, "colors.xml"), "ic_launcher_background"),
                "a color declared as a typed item is still a declaration of that color");

        AndroidGradleBuilder.writeAdaptiveIconBackgroundColor(valsDir, "#000000");
        assertFalse(new File(valsDir, "ic_launcher_background.xml").exists(),
                "so ours must not be written beside it");
    }

    @Test
    void anItemOfAnotherTypeIsNotAColor(@TempDir File valsDir) throws Exception {
        write(valsDir, "<item type=\"dimen\" name=\"ic_launcher_background\">4dp</item>");

        assertFalse(AndroidGradleBuilder.declaresColor(new File(valsDir, "colors.xml"), "ic_launcher_background"),
                "only a color-typed item declares a color");
    }

    private static void platform(File sdkDir, String name, String... attributes) throws IOException {
        File platform = new File(sdkDir, "platforms/" + name);
        assertTrue(platform.mkdirs(), "test platform directory");
        AndroidAttrJar.write(new File(platform, "android.jar"), attributes);
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
