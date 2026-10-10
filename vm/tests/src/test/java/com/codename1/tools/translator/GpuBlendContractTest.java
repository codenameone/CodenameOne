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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Holds every GPU backend to `RenderState.BlendMode`'s contract: a fragment's
/// color is weighted by its alpha, and the framebuffer's alpha accumulates
/// coverage, so a frame cleared opaque stays opaque.
///
/// The contract is four blend factors, and a backend that gets the alpha pair
/// wrong draws a correct picture wherever nothing reads the frame's alpha. The
/// Linux and JavaScript ports used one `blendFunc` for all four channels: a
/// panel of black at 40% over a background left 0.76 in the frame's alpha,
/// and since both ports composite the frame with its alpha, the panel came
/// out at `0.6 * background / 0.76` in the browser and at
/// `0.76 * 0.6 * background + 0.24 * white` on Linux, lighter than the
/// background it was meant to darken. Android and iOS had the same factors
/// behind a surface without alpha.
///
/// No GPU is needed to hold it. Each backend's factors are read -- the
/// JavaScript port's from the class that decides them, compiled alone, the
/// others' from the call that sets them -- and the blend equation is run with
/// them on that panel.
class GpuBlendContractTest {
    private static final Path ROOT = Paths.get("..", "..").normalize().toAbsolutePath();

    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int SRC_ALPHA = 0x0302;
    private static final int ONE_MINUS_SRC_ALPHA = 0x0303;

    /// The factors of one blending mode: source and destination, of the color
    /// channels and then of the alpha channel.
    private static final class Factors {
        final int sourceColor;
        final int destinationColor;
        final int sourceAlpha;
        final int destinationAlpha;

        Factors(int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha) {
            this.sourceColor = sourceColor;
            this.destinationColor = destinationColor;
            this.sourceAlpha = sourceAlpha;
            this.destinationAlpha = destinationAlpha;
        }

        /// What the blend equation leaves of a `source` RGBA fragment drawn on
        /// a `destination` RGBA pixel, each channel in 0..1 and clamped as a
        /// normalized framebuffer clamps it.
        double[] blend(double[] source, double[] destination) {
            double[] out = new double[4];
            for (int i = 0; i < 4; i++) {
                double sf = factor(i < 3 ? sourceColor : sourceAlpha, source[3]);
                double df = factor(i < 3 ? destinationColor : destinationAlpha, source[3]);
                out[i] = Math.min(1.0, source[i] * sf + destination[i] * df);
            }
            return out;
        }

        private static double factor(int name, double sourceAlpha) {
            switch (name) {
                case ZERO:
                    return 0;
                case ONE:
                    return 1;
                case SRC_ALPHA:
                    return sourceAlpha;
                case ONE_MINUS_SRC_ALPHA:
                    return 1 - sourceAlpha;
                default:
                    throw new IllegalArgumentException("factor " + name);
            }
        }
    }

    /// A backend's name for a factor, whatever API it is written for.
    private static int factorNamed(String name) {
        if (name.equals("ZERO") || name.equals("Zero")) {
            return ZERO;
        }
        if (name.equals("ONE") || name.equals("One")) {
            return ONE;
        }
        if (name.equals("SRC_ALPHA") || name.equals("SourceAlpha")) {
            return SRC_ALPHA;
        }
        if (name.equals("ONE_MINUS_SRC_ALPHA") || name.equals("INV_SRC_ALPHA") || name.equals("OneMinusSourceAlpha")) {
            return ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("a blend factor this test does not know: " + name);
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(ROOT.resolve(path)), StandardCharsets.UTF_8);
    }

    /// The canvas panel of the Unity gallery: black at 40% on the camera's
    /// background must leave six tenths of the background, in an opaque frame.
    private static void holdsAlpha(String backend, Factors f) {
        double[] background = {38 / 255.0, 51 / 255.0, 77 / 255.0, 1};
        double[] out = f.blend(new double[] {0, 0, 0, 0.4}, background);
        for (int i = 0; i < 3; i++) {
            assertEquals(0.6 * background[i], out[i], 1e-9, backend + ": channel " + i
                    + " of black at 40% over an opaque background");
        }
        assertEquals(1.0, out[3], 1e-9, backend + ": an opaque frame is no longer opaque under a fragment at 40%"
                + " -- whatever is behind the view shows through it");

        // A tinted fragment: straight alpha, the color is not premultiplied.
        out = f.blend(new double[] {1, 0.5, 0.25, 0.5}, new double[] {0, 0, 0, 1});
        assertEquals(0.5, out[0], 1e-9, backend + ": red at 50% over black");
        assertEquals(0.25, out[1], 1e-9, backend);
        assertEquals(0.125, out[2], 1e-9, backend);
        assertEquals(1.0, out[3], 1e-9, backend);

        // An opaque fragment replaces what was there, and a clear one leaves it.
        out = f.blend(new double[] {0.2, 0.4, 0.6, 1}, background);
        assertEquals(0.2, out[0], 1e-9, backend);
        assertEquals(1.0, out[3], 1e-9, backend);
        out = f.blend(new double[] {0.2, 0.4, 0.6, 0}, background);
        assertEquals(background[0], out[0], 1e-9, backend);
        assertEquals(1.0, out[3], 1e-9, backend);

        // On a frame cleared transparent the alpha is the coverage.
        out = f.blend(new double[] {1, 1, 1, 0.4}, new double[] {0, 0, 0, 0});
        assertEquals(0.4, out[3], 1e-9, backend + ": coverage of one fragment at 40% on a transparent frame");
    }

    private static void holdsAdditive(String backend, Factors f) {
        double[] background = {38 / 255.0, 51 / 255.0, 77 / 255.0, 1};
        double[] out = f.blend(new double[] {0.5, 0.5, 0.5, 0.4}, background);
        for (int i = 0; i < 3; i++) {
            assertEquals(background[i] + 0.2, out[i], 1e-9, backend + ": channel " + i
                    + " of additive grey at 40%");
        }
        assertEquals(1.0, out[3], 1e-9, backend + ": an opaque frame is no longer opaque under additive light");
    }

    /// The JavaScript port's factors come from a class without dependencies,
    /// compiled here alone as `JavaScriptTransformToleranceTest` compiles its
    /// own.
    @Test
    void javaScriptPort() throws Exception {
        Path source = ROOT.resolve(Paths.get("Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1",
                "impl", "html5", "JavaScriptBlendFactors.java"));
        Path sourceDir = Files.createTempDirectory("js-blend-factors-src");
        Path classesDir = Files.createTempDirectory("js-blend-factors-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(source, packageDir.resolve("JavaScriptBlendFactors.java"));
        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptBlendFactors.java").toString()
        ));
        assertEquals(0, compileResult, "JavaScriptBlendFactors should compile standalone");
        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        try {
            Class<?> c = loader.loadClass("com.codename1.impl.html5.JavaScriptBlendFactors");
            Factors[] modes = new Factors[2];
            for (int i = 0; i < 2; i++) {
                Boolean additive = Boolean.valueOf(i == 1);
                modes[i] = new Factors(call(c, "sourceColor", additive), call(c, "destinationColor", additive),
                        call(c, "sourceAlpha", additive), call(c, "destinationAlpha", additive));
            }
            holdsAlpha("JavaScript", modes[0]);
            holdsAdditive("JavaScript", modes[1]);
        } finally {
            loader.close();
        }
        // And the device asks that class, of all four channels.
        String device = read("Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/HTML5GraphicsDevice.java");
        assertFalse(device.contains("gl.blendFunc("), "HTML5GraphicsDevice sets one factor pair for all four channels");
        for (String method : new String[] {"sourceColor", "destinationColor", "sourceAlpha", "destinationAlpha"}) {
            assertTrue(device.contains("JavaScriptBlendFactors." + method + "(additive)"),
                    "HTML5GraphicsDevice does not ask JavaScriptBlendFactors." + method);
        }
        assertTrue(device.contains("gl.blendFuncSeparate("), "HTML5GraphicsDevice does not call blendFuncSeparate");
    }

    private static int call(Class<?> c, String name, Boolean additive) throws Exception {
        Method m = c.getMethod(name, boolean.class);
        return ((Integer) m.invoke(null, additive)).intValue();
    }

    /// The backends over OpenGL name their factors in one call per mode, the
    /// additive one first.
    private static void openGl(String backend, String path) throws Exception {
        String source = read(path);
        assertFalse(Pattern.compile("\\bglBlendFunc\\s*\\(").matcher(source).find(),
                backend + " sets one factor pair for all four channels (glBlendFunc) in " + path);
        Matcher m = Pattern.compile("glBlendFuncSeparate\\s*\\(\\s*(?:\\w+\\.)?GL_(\\w+)\\s*,\\s*(?:\\w+\\.)?GL_(\\w+)\\s*,"
                + "\\s*(?:\\w+\\.)?GL_(\\w+)\\s*,\\s*(?:\\w+\\.)?GL_(\\w+)\\s*\\)").matcher(source);
        List<Factors> found = new ArrayList<Factors>();
        while (m.find()) {
            found.add(new Factors(factorNamed(m.group(1)), factorNamed(m.group(2)), factorNamed(m.group(3)),
                    factorNamed(m.group(4))));
        }
        assertEquals(2, found.size(), backend + ": glBlendFuncSeparate calls in " + path
                + " (the additive mode's, then the alpha mode's)");
        assertEquals(ONE, found.get(0).destinationColor, backend + ": the first call is no longer the additive mode's");
        holdsAdditive(backend, found.get(0));
        holdsAlpha(backend, found.get(1));
    }

    @Test
    void linuxPort() throws Exception {
        openGl("Linux", "Ports/LinuxPort/nativeSources/cn1_linux_gl.c");
    }

    @Test
    void androidPort() throws Exception {
        openGl("Android", "Ports/Android/src/com/codename1/impl/android/AndroidGraphicsDevice.java");
    }

    @Test
    void javaSePortOverOpenGl() throws Exception {
        openGl("JavaSE (JOGL)", "Ports/JavaSE/src/com/codename1/impl/javase/JavaSEGLDevice.java");
    }

    /// Metal: a pipeline descriptor per mode, the alpha mode's first.
    @Test
    void iosPort() throws Exception {
        String source = read("Ports/iOSPort/nativeSources/CN1GL3D.m");
        String[] fields = {"sourceRGBBlendFactor", "destinationRGBBlendFactor", "sourceAlphaBlendFactor",
            "destinationAlphaBlendFactor"};
        int[][] values = new int[2][4];
        for (int f = 0; f < 4; f++) {
            Matcher m = Pattern.compile("\\." + fields[f] + "\\s*=\\s*MTLBlendFactor(\\w+)\\s*;").matcher(source);
            int n = 0;
            while (m.find()) {
                assertTrue(n < 2, "more than two pipelines set " + fields[f] + " in CN1GL3D.m");
                values[n++][f] = factorNamed(m.group(1));
            }
            assertEquals(2, n, "pipelines setting " + fields[f] + " in CN1GL3D.m (alpha, then additive)");
        }
        assertEquals(ONE, values[1][1], "Metal: the second pipeline is no longer the additive mode's");
        holdsAlpha("Metal", new Factors(values[0][0], values[0][1], values[0][2], values[0][3]));
        holdsAdditive("Metal", new Factors(values[1][0], values[1][1], values[1][2], values[1][3]));
    }

    /// Direct3D: one blend description, whose destination color factor is
    /// chosen by the mode.
    @Test
    void windowsPort() throws Exception {
        String source = read("Ports/WindowsPort/nativeSources/cn1_windows_d3d.cpp");
        int sourceColor = d3d(source, "SrcBlend");
        int sourceAlpha = d3d(source, "SrcBlendAlpha");
        int destinationAlpha = d3d(source, "DestBlendAlpha");
        Matcher m = Pattern.compile("rb\\.DestBlend\\s*=\\s*\\(blendMode == 2\\)\\s*\\?\\s*D3D11_BLEND_(\\w+)\\s*:"
                + "\\s*D3D11_BLEND_(\\w+)\\s*;").matcher(source);
        assertTrue(m.find(), "cn1_windows_d3d.cpp no longer chooses DestBlend by the blend mode");
        holdsAdditive("Direct3D", new Factors(sourceColor, factorNamed(m.group(1)), sourceAlpha, destinationAlpha));
        holdsAlpha("Direct3D", new Factors(sourceColor, factorNamed(m.group(2)), sourceAlpha, destinationAlpha));
    }

    private static int d3d(String source, String field) {
        Matcher m = Pattern.compile("rb\\." + field + "\\s*=\\s*D3D11_BLEND_(\\w+)\\s*;").matcher(source);
        assertTrue(m.find(), "cn1_windows_d3d.cpp no longer sets " + field);
        int value = factorNamed(m.group(1));
        assertFalse(m.find(), "cn1_windows_d3d.cpp sets " + field + " twice");
        return value;
    }
}
