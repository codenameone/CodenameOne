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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Translates rive-runtime test files with the build-time transcoder, compiles
/// the generated classes with javac and runs them against the runtime: the
/// whole path a `.riv` next to the theme takes, minus the build plugin.
class RiveGeneratedClassTest {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[] FILES = {"juice.riv", "off_road_car.riv"};

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
            byte[] riv = Files.readAllBytes(new File(FIXTURES + f).toPath());
            String name = className(f);
            File src = new File(pkg, name + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "gen", name).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveGeneratedClassTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static String className(String file) {
        return file.startsWith("juice") ? "JuiceRive" : "OffRoadCarRive";
    }

    private static RiveFile load(String file) throws Exception {
        return (RiveFile) loader.loadClass("gen." + className(file)).getDeclaredConstructor().newInstance();
    }

    @Test
    void everyArtboardBuildsWithEveryReferenceResolved() throws Exception {
        for (String f : FILES) {
            RiveFile file = load(f);
            String[] names = file.getArtboardNames();
            assertTrue(names.length > 0, f);
            for (int i = 0; i < names.length; i++) {
                Artboard a = file.instance(i);
                assertNotNull(a, f + " artboard " + i);
                assertEquals(0, a.getInvalidObjectCount(), f + " artboard " + names[i]);
                assertEquals(0, a.getUnresolvedKeyedObjectCount(), f + " artboard " + names[i]);
                assertEquals(file.getAnimationNames(names[i]).length, a.getAnimations().size());
                assertEquals(file.getStateMachineNames(names[i]).length, a.getStateMachines().size());
            }
        }
    }

    @Test
    void instancesAreIndependent() throws Exception {
        RiveFile file = load("juice.riv");
        Artboard a = file.instance(0);
        Artboard b = file.instance(0);
        assertTrue(a != b);
        assertTrue(a.getObjects().get(0) != b.getObjects().get(0));
    }

    @Test
    void theFirstAnimationMovesWhatIsDrawn() throws Exception {
        for (String f : FILES) {
            RiveFile file = load(f);
            Artboard a = file.instance(0);
            ArrayList<LinearAnimation> anims = a.getAnimations();
            assertTrue(!anims.isEmpty(), f);
            LinearAnimationInstance anim = new LinearAnimationInstance(anims.get(0), a);
            anim.apply(1f);
            a.advance(0);
            Recorder start = new Recorder();
            a.draw(start);
            assertTrue(start.paths > 0, f + " draws nothing");
            anim.advance(anims.get(0).durationSeconds() / 2f);
            anim.apply(1f);
            a.advance(0);
            Recorder middle = new Recorder();
            a.draw(middle);
            assertNotEquals(start.signature, middle.signature, f + " did not animate");
        }
    }

    /// Folds every point drawn, in the transform it is drawn with, into one number.
    private static final class Recorder implements RiveRenderer {
        int paths;
        double signature;
        private final ArrayList<Mat2D> stack = new ArrayList<Mat2D>();
        private Mat2D current = new Mat2D();

        @Override
        public void save() {
            stack.add(new Mat2D().set(current));
        }

        @Override
        public void restore() {
            current = stack.remove(stack.size() - 1);
        }

        @Override
        public void transform(Mat2D m) {
            current = new Mat2D().multiply(current, m);
        }

        @Override
        public void clipPath(RivePath path) {
            fold(path, 7);
        }

        @Override
        public void drawPath(RivePath path, RivePaint paint) {
            paths++;
            fold(path, 1);
        }

        private void fold(RivePath path, int weight) {
            for (int i = 0; i + 1 < path.pointCount; i += 2) {
                float x = path.points[i];
                float y = path.points[i + 1];
                signature += weight * (current.mapX(x, y) + 3 * current.mapY(x, y));
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
