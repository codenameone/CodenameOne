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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The adaptive launcher icon background color must never reach the generated
 * theme as a framework attribute.
 *
 * <p>Every color in {@code res/values/colors.xml} is promoted to an
 * {@code android:<name>} item of {@code CustomTheme}, which aapt2 resolves as
 * {@code android:attr/<name>}. {@code ic_launcher_background} is not an
 * Android theme attribute, so a build that writes it there dies at resource
 * linking with "style attribute 'android:attr/ic_launcher_background' not
 * found" and produces no APK at all -- issue #5837, which is what
 * {@code android.enableAdaptiveIcons=true} did on the build server.</p>
 */
class AndroidAdaptiveIconBackgroundTest {

    @Test
    void backgroundColorStaysOutOfTheGeneratedTheme(@TempDir File valsDir) throws Exception {
        AndroidGradleBuilder.writeAdaptiveIconBackgroundColor(valsDir, "#000000");

        File ownFile = new File(valsDir, "ic_launcher_background.xml");
        assertTrue(ownFile.exists(), "the background color needs a values file of its own");
        assertTrue(read(ownFile).contains("<color name=\"ic_launcher_background\">#000000</color>"),
                "the requested background color is what gets written");
        assertFalse(new File(valsDir, "colors.xml").exists(),
                "colors.xml is the developer's theme-attribute file, not somewhere to stash our own resources");

        assertEquals("", AndroidGradleBuilder.buildThemeColorItems(new File(valsDir, "colors.xml")),
                "nothing the builder generates for itself may be promoted into the theme");
    }

    @Test
    void developerColorsAreStillPromotedToTheTheme(@TempDir File valsDir) throws Exception {
        File colors = new File(valsDir, "colors.xml");
        write(colors, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "    <color name=\"colorPrimary\">#ff00ff00</color>\n"
                + "    <color name=\"statusBarColor\">#80ff0000</color>\n"
                + "</resources>\n");

        String items = AndroidGradleBuilder.buildThemeColorItems(colors);
        assertTrue(items.contains("<item name=\"android:colorPrimary\">@color/colorPrimary</item>"),
                "documented behavior: a color name that is a theme attribute reaches the theme");
        assertTrue(items.contains("<item name=\"android:statusBarColor\">@color/statusBarColor</item>"),
                "documented behavior: a color name that is a theme attribute reaches the theme");
    }

    @Test
    void aDeveloperOwnedBackgroundColorIsNotDuplicated(@TempDir File valsDir) throws Exception {
        // An Android Studio project template declares ic_launcher_background in
        // colors.xml, so a developer may well have copied one in. Writing ours
        // beside theirs is a duplicate resource, which fails linking just as
        // surely as the attribute promotion does.
        File colors = new File(valsDir, "colors.xml");
        write(colors, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "    <color name=\"ic_launcher_background\">#123456</color>\n"
                + "</resources>\n");

        AndroidGradleBuilder.writeAdaptiveIconBackgroundColor(valsDir, "#000000");

        assertFalse(new File(valsDir, "ic_launcher_background.xml").exists(),
                "the developer's own declaration wins rather than colliding with ours");
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static void write(File f, String content) throws IOException {
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
