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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The iOS app icon lives only in the AppIcon asset catalog.
///
/// The builder used to copy every icon size, Icon.png and iTunesArtwork into the bundle
/// root as well, for the top-level CFBundleIconFiles of iOS 6 and older -- about 0.6MB
/// that nothing on a supported iOS reads. These pin the replacement: every catalog image
/// is written, into the catalog and nowhere else, each is one the catalog's Contents.json
/// names, and the Info.plist template no longer lists the loose files, which would
/// otherwise be a dangling reference App Store validation reports.
public class IPhoneBuilderIconsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void everyCatalogIconIsWrittenAtItsSizeAndNothingElse() throws Exception {
        File dir = tmp.newFolder("AppIcon.appiconset");
        BufferedImage source = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        new IPhoneBuilder().writeAppIcons(source, dir);

        Set<String> expected = new HashSet<String>();
        for (Object[] icon : IPhoneBuilder.APP_ICONS) {
            String name = (String) icon[0];
            expected.add(name);
            BufferedImage written = ImageIO.read(new File(dir, name));
            int size = ((Integer) icon[1]).intValue();
            assertEquals(name + " width", size, written.getWidth());
            assertEquals(name + " height", size, written.getHeight());
        }
        assertEquals(new HashSet<String>(Arrays.asList(dir.list())), expected);
    }

    @Test
    public void everyCatalogIconIsNamedByTheCatalog() throws Exception {
        String contents = new String(Files.readAllBytes(
                Paths.get("..", "..", "vm", "ByteCodeTranslator", "src", "Icons.json")), StandardCharsets.UTF_8);
        Set<String> named = new HashSet<String>();
        Matcher m = Pattern.compile("\"filename\"\\s*:\\s*\"([^\"]+)\"").matcher(contents);
        while (m.find()) {
            named.add(m.group(1));
        }
        for (Object[] icon : IPhoneBuilder.APP_ICONS) {
            assertTrue(icon[0] + " is written but the asset catalog does not use it",
                    named.contains((String) icon[0]));
        }
    }

    @Test
    public void theInfoPlistTemplateNamesNoLooseIcons() throws Exception {
        String plist = new String(Files.readAllBytes(Paths.get("..", "..", "vm", "ByteCodeTranslator", "src",
                "template", "template", "template-Info.plist")), StandardCharsets.UTF_8);
        // CFBundleIcons / CFBundleIconName come from actool for the catalog; the
        // top-level list is the one that named the loose files.
        assertFalse("the top-level CFBundleIconFiles names files the bundle no longer carries",
                plist.contains("<key>CFBundleIconFiles</key>"));
        for (Object[] icon : IPhoneBuilder.APP_ICONS) {
            assertFalse(icon[0] + " is still named by the Info.plist template",
                    plist.contains(">" + icon[0] + "<"));
        }
    }
}
