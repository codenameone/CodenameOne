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
package com.codename1.rive;

import com.codename1.impl.rive.Artboard;
import com.codename1.impl.rive.Core;
import com.codename1.impl.rive.LayoutComponent;
import com.codename1.rive.transcoder.RiveTranscoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Ports the position expectations of rive-runtime's
/// `tests/unit_tests/runtime/layout_test.cpp` onto the flex layout pass.
class RiveLayoutTest {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[] FILES = {"layout_horizontal", "layout_vertical", "layout_horizontal_gaps",
        "layout_horizontal_wrap", "layout_center", "layout_complex1", "layout_alignment", "layout_paint",
        "layout_hug_artboard"};

    @TempDir
    static Path temporary;
    private static URLClassLoader loader;

    @BeforeAll
    static void translateAndCompile() throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(temporary.toString());
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        File pkg = temporary.resolve("gen").toFile();
        assertTrue(pkg.mkdirs());
        for (String f : FILES) {
            byte[] riv = Files.readAllBytes(new File(FIXTURES + f + ".riv").toPath());
            File src = new File(pkg, className(f) + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "gen", className(f)).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveLayoutTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static String className(String file) {
        StringBuilder b = new StringBuilder();
        for (String part : file.split("_")) {
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.append("Rive").toString();
    }

    private static Artboard artboard(String file) throws Exception {
        RiveFile f = (RiveFile) loader.loadClass("gen." + className(file)).getDeclaredConstructor().newInstance();
        Artboard a = f.instance(0);
        a.advance(0);
        return a;
    }

    private static LayoutComponent find(Artboard a, String name) {
        for (Core o : a.getObjects()) {
            if (o instanceof LayoutComponent && name.equals(((LayoutComponent) o).getName())) {
                return (LayoutComponent) o;
            }
        }
        throw new AssertionError("no layout " + name);
    }

    private static void at(Artboard a, String name, float x, float y) {
        LayoutComponent c = find(a, name);
        assertEquals(x, c.getWorldTransform().mapX(0, 0), 0.001f, name + " x");
        assertEquals(y, c.getWorldTransform().mapY(0, 0), 0.001f, name + " y");
    }

    @Test
    void rowColumnAndGap() throws Exception {
        Artboard a = artboard("layout_horizontal");
        at(a, "LayoutComponent1", 0, 0);
        at(a, "LayoutComponent2", 100, 0);
        at(a, "LayoutComponent3", 200, 0);
        a = artboard("layout_vertical");
        at(a, "LayoutComponent1", 0, 0);
        at(a, "LayoutComponent2", 0, 100);
        at(a, "LayoutComponent3", 0, 200);
        a = artboard("layout_horizontal_gaps");
        at(a, "LayoutComponent1", 0, 0);
        at(a, "LayoutComponent2", 110, 0);
        at(a, "LayoutComponent3", 220, 0);
    }

    @Test
    void wrapAndCenter() throws Exception {
        at(artboard("layout_horizontal_wrap"), "LayoutComponent6", 0, 100);
        at(artboard("layout_center"), "LayoutComponent1", 200, 200);
    }

    @Test
    void paddingMarginAndFill() throws Exception {
        Artboard a = artboard("layout_complex1");
        at(a, "LayoutLeft", 0, 0);
        at(a, "LayoutLeftChild1", 20, 20);
        at(a, "LayoutLeftChild2", 130, 20);
        at(a, "LayoutRight", 250, 0);
        at(a, "LayoutRightChild1", 285, 35);
        at(a, "LayoutRightChild2", 285, 215);
        assertEquals(460, find(a, "LayoutLeftChild1").getLayoutHeight(), 0.001f);
    }

    @Test
    void alignmentFollowsStyleChanges() throws Exception {
        Artboard a = artboard("layout_alignment");
        LayoutComponent container = find(a, "LayoutContainer");
        container.getStyle().setLayoutAlignmentType(9);
        a.advance(0);
        at(a, "Layout1", 0, 0);
        at(a, "Layout2", 200, 0);
        at(a, "Layout3", 400, 0);
        container.getStyle().setLayoutAlignmentType(10);
        a.advance(0);
        at(a, "Layout1", 0, 200);
        at(a, "Layout2", 200, 200);
        at(a, "Layout3", 400, 200);
        container.getStyle().setFlexDirectionValue(0);
        a.advance(0);
        at(a, "Layout1", 200, 0);
        at(a, "Layout2", 200, 200);
        at(a, "Layout3", 200, 400);
        container.getStyle().setLayoutAlignmentType(11);
        a.advance(0);
        at(a, "Layout1", 400, 0);
        at(a, "Layout2", 400, 200);
        at(a, "Layout3", 400, 400);
    }

    // Upstream: layout_test.cpp "Top-level hug artboard computes size from
    // children" expects 302.63 x 100. The width comes from the text's
    // measured size, so it is held to font-shaping tolerance.
    @Test
    void hugArtboardIsSizedByItsContent() throws Exception {
        RiveFile f = (RiveFile) loader.loadClass("gen." + className("layout_hug_artboard")).getDeclaredConstructor().newInstance();
        String[] names = f.getArtboardNames();
        int index = -1;
        for (int i = 0; i < names.length; i++) {
            if ("HugArtboard".equals(names[i])) {
                index = i;
            }
        }
        assertTrue(index >= 0, "HugArtboard");
        Artboard a = f.instance(index);
        a.advance(0);
        assertEquals(302.63f, a.getLayoutWidth(), 0.5f);
        assertEquals(100f, a.getLayoutHeight(), 0.001f);
    }
}
