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
package com.codename1.androidcompat.runtime;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.codename1.android.rescompiler.ResourceCompiler;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// `getDrawableForDensity` picks the image variant for the density asked for
/// and scales it to that density, as a launcher asking for a larger icon
/// expects. It used to ignore the density and answer the device's drawable.
public class DrawableForDensityTest {

    private static final String TABLE = "density_probe_table.bin";

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void png(File f, int size) throws IOException {
        f.getParentFile().mkdirs();
        ImageIO.write(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB), "png", f);
    }

    @Test
    public void theRequestedDensityPicksAndScalesTheImage() throws Exception {
        File res = tmp.newFolder("res");
        png(new File(res, "drawable-mdpi/density_probe.png"), 8);
        png(new File(res, "drawable-xhdpi/density_probe.png"), 16);
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.res.add(new ResourceCompiler.ResSource(res, "com.t"));
        r.javaOut = tmp.newFolder("java");
        r.resourcesOut = tmp.newFolder("out");
        r.frameworkSymbols = getClass().getResourceAsStream("/" + ResourceCompiler.FRAMEWORK_SYMBOLS_RESOURCE);
        assertNotNull(r.frameworkSymbols);
        ResourceCompiler.Result result = new ResourceCompiler().compile(r);
        assertTrue(result.diagnostics.toString(), !result.hasErrors());
        ResTable table;
        InputStream in = new FileInputStream(new File(r.resourcesOut, ResourceCompiler.APP_TABLE));
        try {
            table = ResTable.read(in);
        } finally {
            in.close();
        }
        int id = table.get("drawable/density_probe").id;

        // The runtime reads the table and images from the class path.
        File classes = new File(DrawableForDensityTest.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI());
        List<File> copied = new ArrayList<File>();
        File[] outputs = r.resourcesOut.listFiles();
        assertNotNull(outputs);
        for (File f : outputs) {
            if (!f.isFile()) {
                continue;
            }
            String name = f.getName().equals(ResourceCompiler.APP_TABLE) ? TABLE : f.getName();
            File dest = new File(classes, name);
            Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            copied.add(dest);
        }
        Resources resources = AndroidTestSupport.context().getResources();
        ResourceManager rm = resources.getManager();
        boolean pixels = HeadlessImplementation.pixelImages;
        HeadlessImplementation.pixelImages = true;
        rm.setAppTable(TABLE);
        try {
            assertEquals(320, rm.device().densityDpi);
            Drawable xxxhdpi = resources.getDrawableForDensity(id, 640);
            assertEquals("the xhdpi image was not scaled up to 640 dpi", 32, xxxhdpi.getIntrinsicWidth());
            Drawable mdpi = resources.getDrawableForDensity(id, 160);
            assertEquals("the mdpi variant was not chosen for 160 dpi", 8, mdpi.getIntrinsicWidth());
            assertEquals("the device's own drawable changed", 16, resources.getDrawable(id).getIntrinsicWidth());
        } finally {
            rm.setAppTable(null);
            HeadlessImplementation.pixelImages = pixels;
            for (File f : copied) {
                Files.deleteIfExists(f.toPath());
            }
        }
    }
}
