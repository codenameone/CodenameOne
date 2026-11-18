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
import com.codename1.impl.rive.GpuRiveRenderer;
import com.codename1.impl.rive.Image;
import com.codename1.impl.rive.Mat2D;
import com.codename1.impl.rive.Mesh;
import com.codename1.impl.rive.NestedArtboard;
import com.codename1.impl.rive.RivePaint;
import com.codename1.impl.rive.RivePath;
import com.codename1.impl.rive.RiveRenderer;
import com.codename1.impl.rive.TrimPath;
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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Images, meshes, nested artboards and path effects, with the expectations
/// of rive-runtime's image_mesh_test, trim_test and nested_artboard tests on
/// the same files, and the layer batching of the GPU renderer.
class RiveAssetsTest {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[] FILES = {
        "tape.riv", "trim.riv", "stacked_path_effects.riv", "nested_mesh_pointers.riv",
        "jellyfish_test.riv"};
    private static final float EPS = 0.01f;

    @TempDir
    static java.nio.file.Path temporary;
    private static URLClassLoader loader;

    @BeforeAll
    static void translateAndCompile() throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(temporary.toString());
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        File pkg = temporary.resolve("assets").toFile();
        assertTrue(pkg.mkdirs());
        for (String f : FILES) {
            byte[] riv = Files.readAllBytes(new File(FIXTURES + f).toPath());
            String name = className(f);
            File src = new File(pkg, name + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "assets", name).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveAssetsTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static String className(String file) {
        StringBuilder sb = new StringBuilder("Fixture");
        for (String part : file.substring(0, file.length() - 4).split("_")) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static Artboard artboard(String file) throws Exception {
        RiveFile f = (RiveFile) loader.loadClass("assets." + className(file)).getDeclaredConstructor().newInstance();
        Artboard a = f.instance(0);
        assertNotNull(a);
        return a;
    }

    private static <T> T find(Artboard a, Class<T> type, String name) {
        for (Core o : a.getObjects()) {
            if (type.isInstance(o) && (name == null || name.equals(((com.codename1.impl.rive.Component) o).getName()))) {
                return type.cast(o);
            }
        }
        return null;
    }

    /// image_mesh_test "image with mesh loads correctly".
    @Test
    void tapeImageHasItsMesh() throws Exception {
        Artboard a = artboard("tape.riv");
        Image tape = find(a, Image.class, "Tape body.png");
        assertNotNull(tape);
        assertNotNull(tape.getImageAsset());
        assertTrue(tape.getImageAsset().getContents().length > 0);
        Mesh mesh = tape.getMesh();
        assertNotNull(mesh);
        assertEquals(24, mesh.getVertices().size());
        assertEquals(31 * 3, mesh.getIndexCount());
        assertTrue(mesh.hasTriangles());
    }

    /// RiveView takes the GPU path only for an artboard with a mesh, nested
    /// ones included.
    @Test
    void meshArtboardsAreFoundForTheGpuPath() throws Exception {
        assertTrue(RiveView.containsMesh(artboard("jellyfish_test.riv"), 0));
        assertTrue(RiveView.containsMesh(artboard("tape.riv"), 0));
        assertTrue(RiveView.containsMesh(artboard("nested_mesh_pointers.riv"), 0));
        assertFalse(RiveView.containsMesh(artboard("trim.riv"), 0));
    }

    /// The skinned jellyfish records frame after frame for the GPU and sends
    /// no mesh to the CPU layer (its meshes are source-over and unclipped).
    /// Without a Display its images are not decoded, so no mesh is drawn
    /// here; the triangle count is covered where images decode.
    @Test
    void skinnedMeshIsRecordedForTheGpu() throws Exception {
        Artboard a = artboard("jellyfish_test.riv");
        GpuRiveRenderer r = new GpuRiveRenderer();
        for (int i = 0; i < 3; i++) {
            a.advance(1 / 60f);
            r.beginFrame(0, 0, new Mat2D());
            a.draw(r);
            r.endFrame(0);
            assertEquals(0, r.getCpuMeshCount());
            assertTrue(r.hasPendingFrame());
        }
    }

    /// trim_test "A 0 scale path will trim with no crash", and the trim
    /// drawing nothing for an empty span.
    @Test
    void trimFileAdvancesAndDraws() throws Exception {
        Artboard a = artboard("trim.riv");
        assertEquals(0, a.getInvalidObjectCount());
        Counter c = new Counter();
        for (int i = 0; i < 10; i++) {
            a.advance(1 / 60f);
            a.draw(c);
        }
        assertNotNull(find(a, TrimPath.class, null));
    }

    @Test
    void sequentialTrimKeepsTheMiddleOfALine() {
        RivePath line = new RivePath();
        line.moveTo(0, 0);
        line.lineTo(100, 0);
        TrimPath trim = new TrimPath();
        trim.setModeValue(TrimPath.MODE_SEQUENTIAL);
        trim.setStart(0.25f);
        trim.setEnd(0.75f);
        RivePath out = trim.effectPath(line, false);
        assertEquals(RivePath.MOVE, out.verbs[0]);
        assertEquals(25f, out.points[0], EPS);
        assertEquals(75f, out.points[out.pointCount - 2], EPS);
        trim.setOffset(0.5f);
        out = trim.effectPath(line, false);
        // Offset past the end wraps: 0.75..1.25 is 75..100 then the start.
        assertEquals(75f, out.points[0], EPS);
    }

    /// Dashes cut each stroke into several contours.
    @Test
    void dashesSplitStrokes() throws Exception {
        Artboard a = artboard("stacked_path_effects.riv");
        Counter c = new Counter();
        a.advance(0f);
        a.draw(c);
        assertTrue(c.maxMoves > 4, "dashed strokes start many contours: " + c.maxMoves);
    }

    /// nested_artboard tests: each nested artboard is its own instance, and
    /// the mesh it hosts stays valid.
    @Test
    void nestedArtboardsAreInstanced() throws Exception {
        Artboard a = artboard("nested_mesh_pointers.riv");
        assertEquals(0, a.getInvalidObjectCount());
        ArrayList<Artboard> instances = new ArrayList<Artboard>();
        int withMesh = 0;
        for (Core o : a.getObjects()) {
            if (o instanceof NestedArtboard) {
                NestedArtboard n = (NestedArtboard) o;
                assertNotNull(n.getArtboardInstance());
                for (Artboard other : instances) {
                    assertTrue(other != n.getArtboardInstance());
                }
                instances.add(n.getArtboardInstance());
                if (n.getMesh() != null) {
                    withMesh++;
                }
            }
        }
        assertTrue(instances.size() >= 2);
        assertTrue(withMesh >= 1);
        Counter c = new Counter();
        a.advance(1 / 60f);
        a.draw(c);
        assertTrue(c.paths > 0);
    }

    /// Vector draws between meshes batch into one layer each, in order.
    @Test
    void gpuRendererBatchesVectorLayers() {
        GpuRiveRenderer r = new GpuRiveRenderer();
        RivePath p = new RivePath();
        RivePaint paint = new RivePaint();
        float[] v = {0, 0, 10, 0, 0, 10};
        float[] uv = {0, 0, 1, 0, 0, 1};
        int[] idx = {0, 1, 2};
        r.begin(null, 0, 0, new Mat2D());
        r.drawPath(p, paint);
        r.drawPath(p, paint);
        r.drawImageMesh(null, v, uv, 3, idx, 3, 3, 1f);
        r.drawImageMesh(null, v, uv, 3, idx, 3, 3, 1f);
        r.drawPath(p, paint);
        r.drawImageMesh(null, v, uv, 3, idx, 3, 3, 1f);
        r.drawPath(p, paint);
        r.end();
        assertEquals(3, r.getMeshCount());
        assertEquals(3, r.getLayerCount());
        assertTrue(r.supportsMeshes());
        r.begin(null, 0, 0, new Mat2D());
        r.end();
        assertEquals(0, r.getLayerCount());
    }

    private static final class Counter implements RiveRenderer {
        int paths;
        int maxMoves;

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
            int moves = 0;
            for (int i = 0; i < path.verbCount; i++) {
                if (path.verbs[i] == RivePath.MOVE) {
                    moves++;
                }
            }
            maxMoves = Math.max(maxMoves, moves);
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
