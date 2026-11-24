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
package com.codename1.impl.rive;

import com.codename1.impl.rive.Mat2D;
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

/// Text layout and glyph drawing, on rive-runtime's text fixtures (both embed
/// SIL OFL fonts): the runtime shapes with the embedded TrueType font and
/// draws every glyph as a path with the style's paints.
class RiveTextTest {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[][] FILES = {
        {"hello_world.riv", "HelloWorldRive"},
        {"text_effect_opacity_buckets.riv", "OpacityBucketsRive"},
    };

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
        for (String[] f : FILES) {
            byte[] riv = Files.readAllBytes(new File(FIXTURES + f[0]).toPath());
            File src = new File(pkg, f[1] + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "gen", f[1]).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveTextTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static Artboard load(String className) throws Exception {
        RiveFile file = (RiveFile) loader.loadClass("gen." + className).getDeclaredConstructor().newInstance();
        Artboard a = file.instance(0);
        a.advance(0);
        return a;
    }

    private static <T> ArrayList<T> find(Artboard a, Class<T> type) {
        ArrayList<T> out = new ArrayList<T>();
        for (Core c : a.getObjects()) {
            if (type.isInstance(c)) {
                out.add(type.cast(c));
            }
        }
        return out;
    }

    @Test
    void helloWorldLaysOutOneLineOfTwelveGlyphs() throws Exception {
        Artboard a = load("HelloWorldRive");
        // text_test.cpp "simple text loads": one text, one run.
        assertEquals(1, find(a, Text.class).size());
        ArrayList<TextValueRun> runs = find(a, TextValueRun.class);
        assertEquals(1, runs.size());
        assertEquals("Hello World!", runs.get(0).getText());
        Text text = find(a, Text.class).get(0);
        assertEquals(12, text.getGlyphCount());
        assertEquals(1, text.getLineCount());
        assertTrue(text.getLineWidth(0) > 0f);
        assertTrue(text.getLineBaseline(0) > 0f);

        Recorder r = new Recorder();
        a.draw(r);
        assertTrue(r.paths > 0, "no glyphs drawn");
        // Eleven visible glyphs (the space has no outline), each closed at
        // least once.
        assertTrue(r.closes >= 11, "closes " + r.closes);
    }

    @Test
    void newlinesStartParagraphsAndNarrowBoxesWrap() throws Exception {
        Artboard a = load("HelloWorldRive");
        Text text = find(a, Text.class).get(0);
        TextValueRun run = find(a, TextValueRun.class).get(0);
        float oneLine = text.getLineWidth(0);

        run.setText("Hello\nWorld!");
        a.advance(0);
        assertEquals(2, text.getLineCount());
        assertTrue(text.getLineBaseline(1) > text.getLineBaseline(0));

        run.setText("Hello World!");
        text.setSizingValue(2);
        text.setWrapValue(0);
        text.setWidth(oneLine * 0.75f);
        text.setHeight(1000f);
        a.advance(0);
        assertEquals(2, text.getLineCount());
        assertTrue(text.getLineWidth(0) < oneLine * 0.75f);
        assertTrue(text.getLineWidth(1) < oneLine * 0.75f);
    }

    @Test
    void opacityModifiersFadeSomeGlyphs() throws Exception {
        Artboard a = load("OpacityBucketsRive");
        Recorder r = new Recorder();
        a.draw(r);
        assertTrue(r.paths > 0, "no glyphs drawn");
        assertTrue(r.faded > 0, "no glyph drawn faded");
        assertTrue(r.opaque > 0, "no glyph drawn opaque");
    }

    private static final class Recorder implements RiveRenderer {
        int paths;
        int closes;
        int faded;
        int opaque;

        @Override
        public void save() {
        }

        @Override
        public void restore() {
        }

        @Override
        public void transform(Mat2D m) {
        }

        @Override
        public void clipPath(RivePath path) {
        }

        @Override
        public void drawPath(RivePath path, RivePaint paint) {
            paths++;
            for (int i = 0; i < path.verbCount; i++) {
                if (path.verbs[i] == RivePath.CLOSE) {
                    closes++;
                }
            }
            int alpha = paint.color >>> 24;
            if (alpha == 255) {
                opaque++;
            } else if (alpha > 0) {
                faded++;
            }
        }

        @Override
        public void drawImage(Object image, int blendMode, float opacity) {
        }

        @Override
        public void drawImageMesh(Object image, float[] vertices, float[] uvs, int vertexCount,
                int[] indices, int indexCount, int blendMode, float opacity) {
        }

        @Override
        public boolean supportsMeshes() {
            return false;
        }
    }
}
