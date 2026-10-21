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
package com.codename1.unity.scenecompiler;

import com.codename1.cil.UnityToolchain;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Assert;
import org.junit.Test;

/// `Resources.Load`, from the files of a project to the answers a running
/// application gives.
///
/// A project is written here with a `Resources` folder holding one of each
/// thing that has gone wrong or could: a text with every kind of line end,
/// a quote, a backslash, a byte order mark and a character outside ASCII; a
/// text longer than one string constant may be; a `.bytes` file with every
/// awkward byte; a second `Resources` folder deeper in the tree; a polygon
/// sprite; and a file of a kind nothing loads. The scene compiler is run on
/// it, what it generated is compiled against the runtime, and a small main
/// class asks for each asset by path and prints what it got.
public class ResourcesTest {
    private static void write(File file, byte[] bytes) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), bytes);
    }

    private static void write(File file, String text) throws IOException {
        write(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void meta(File file, String guid, String importer) throws IOException {
        write(new File(file.getParentFile(), file.getName() + ".meta"), "fileFormatVersion: 2\nguid: " + guid + "\n"
                + importer);
    }

    private static byte[] whiteSquare(int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, 0xffffffff);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    public void resourcesAreFoundByPathAndHoldWhatTheFilesDo() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("resources");
        File sample = new File(UnityToolchain.samples(), "scene2d");
        UnityToolchain.dotnetBuild(new File(sample, "Scene2d.csproj"), work);
        File csharp = new File(UnityToolchain.runtimeModule(), "src/main/csharp");
        File valuesBin = new File(csharp, "UnityEngine.Values/bin/Release/netstandard2.1");
        File netstandard = new File(valuesBin, "netstandard-ref/netstandard.dll");
        File values = new File(valuesBin, "Codename1.UnityValues.dll");
        File engine = new File(csharp, "UnityEngine/bin/Release/netstandard2.1/UnityEngine.dll");
        File scripts = new File(sample, "bin/Release/netstandard2.1/Assembly-CSharp.dll");

        File project = new File(work, "project");
        File resources = new File(project, "Assets/Resources");
        String text = "\ufeffone,two\r\nthree \"quoted\" back\\slash\rtab\there caf\u00e9 \u4e2d\n";
        File level = new File(resources, "Levels/First.txt");
        write(level, text);
        meta(level, "d1000000000000000000000000000001", "TextScriptImporter:\n  userData:\n");
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            longText.append("row ").append(i).append('\n');
        }
        File big = new File(resources, "big.json");
        write(big, longText.toString());
        meta(big, "d1000000000000000000000000000002", "TextScriptImporter:\n  userData:\n");
        byte[] blob = new byte[256];
        for (int i = 0; i < blob.length; i++) {
            blob[i] = (byte) i;
        }
        File bytes = new File(resources, "blob.bytes");
        write(bytes, blob);
        meta(bytes, "d1000000000000000000000000000003", "TextScriptImporter:\n  userData:\n");
        File deep = new File(project, "Assets/Game/Resources/Deep/Down.xml");
        write(deep, "<deep/>");
        meta(deep, "d1000000000000000000000000000004", "TextScriptImporter:\n  userData:\n");
        File unsupported = new File(resources, "model.fbx");
        write(unsupported, "not a model");
        meta(unsupported, "d1000000000000000000000000000005", "ModelImporter:\n  userData:\n");
        File outside = new File(project, "Assets/Data/outside.txt");
        write(outside, "not under Resources");
        meta(outside, "d1000000000000000000000000000006", "TextScriptImporter:\n  userData:\n");
        // A triangle, in pixels from the centre of an 8 by 8 image, y up.
        File triangle = new File(resources, "Icons/tri.png");
        write(triangle, whiteSquare(8));
        meta(triangle, "d1000000000000000000000000000007", "TextureImporter:\n"
                + "  spriteMode: 3\n"
                + "  spritePixelsToUnits: 16\n"
                + "  spritePivot: {x: 0.5, y: 0.5}\n"
                + "  alignment: 0\n"
                + "  spriteSheet:\n"
                + "    sprites: []\n"
                + "    outline:\n"
                + "    - - {x: 0, y: 4}\n"
                + "      - {x: -3.464, y: -2}\n"
                + "      - {x: 3.464, y: -2}\n"
                + "    physicsShape: []\n");

        SceneCompiler compiler = new SceneCompiler();
        String source = compiler.compile(project, Arrays.asList(scripts, values), Arrays.asList(engine, netstandard));
        Assert.assertEquals("warnings", "[]", compiler.warnings().toString());
        String notes = compiler.notes().toString();
        Assert.assertTrue(notes, notes.contains("Assets/Resources/model.fbx is under a Resources folder and is of a kind"
                + " that is not supported"));
        Assert.assertTrue(notes, notes.contains("tri.png is a polygon sprite: it was cut to its outline"));
        // A line end is never a unicode escape, which javac would read as
        // the end of the line it is on.
        Assert.assertFalse(source, source.contains("\\u000d") || source.contains("\\u000a"));
        Assert.assertTrue(source, source.contains("one,two\\015\\nthree \\\"quoted\\\" back\\\\slash\\015tab\\there"
                + " caf\\u00e9 \\u4e2d\\n"));
        Assert.assertFalse("a file outside Resources was made loadable", source.contains("\"outside\""));

        // The triangle: nothing in the corners, the image in the middle.
        byte[] cut = compiler.generatedImages().get("unity-polygon-tri.png");
        Assert.assertNotNull("no image was cut for the polygon sprite: " + compiler.generatedImages().keySet(), cut);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(cut));
        Assert.assertEquals(256, image.getWidth());
        Assert.assertEquals(0, image.getRGB(2, 2) >>> 24);
        Assert.assertEquals(0, image.getRGB(253, 2) >>> 24);
        Assert.assertEquals(0, image.getRGB(128, 250) >>> 24);
        Assert.assertEquals(0xffffffff, image.getRGB(128, 128));
        Assert.assertEquals(0xffffffff, image.getRGB(128, 20));
        Assert.assertEquals(0xffffffff, image.getRGB(40, 180));

        // Compiled against the runtime and asked.
        File javaSources = new File(UnityToolchain.runtimeModule(), "src/main/java/com/codename1/unitycompat");
        File core = UnityToolchain.core();
        String translator = "com.codename1.cil.translate.Translator";
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(javaSources, "system"));
        File valueClasses = new File(work, "values");
        UnityToolchain.tool(work, "translate-values", translator, Arrays.asList("--out",
                valueClasses.getAbsolutePath(), "--runtime", runtime.getAbsolutePath(), "--ref",
                netstandard.getAbsolutePath(), values.getAbsolutePath()));
        UnityToolchain.javac(work, "javac-engine", runtime, UnityToolchain.path(runtime, valueClasses, core),
                new File(javaSources, "unityengine"),
                new File(javaSources, "tmpro"), new File(javaSources, "cinemachine"));
        File app = new File(work, "app");
        UnityToolchain.tool(work, "translate-scripts", translator, Arrays.asList("--out", app.getAbsolutePath(),
                "--runtime", runtime.getAbsolutePath(), "--ref", engine.getAbsolutePath(), "--ref",
                netstandard.getAbsolutePath(), values.getAbsolutePath(), scripts.getAbsolutePath()));
        File generated = new File(work, "generated");
        write(new File(generated, "com/codename1/generated/unity/UnityAppImpl.java"),
                source.getBytes(StandardCharsets.US_ASCII));
        String e = "com.codename1.unitycompat.unityengine.";
        write(new File(generated, "probe/Probe.java"), "package probe;\n"
                + "public class Probe {\n"
                + "    private static String codes(String s) {\n"
                + "        StringBuilder sb = new StringBuilder();\n"
                + "        for (int i = 0; i < s.length(); i++) {\n"
                + "            sb.append((int) s.charAt(i)).append(' ');\n"
                + "        }\n"
                + "        return sb.toString();\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        com.codename1.generated.unity.UnityAppImpl.install();\n"
                + "        " + e + "TextAsset first = (" + e + "TextAsset) " + e + "Resources.Load(\"levels/FIRST\", "
                + e + "TextAsset.class);\n"
                + "        System.out.println(\"first \" + first.get_name() + \" \" + codes(first.get_text()));\n"
                + "        System.out.println(\"first bytes \" + first.get_bytes().length);\n"
                + "        System.out.println(\"same \" + (first == " + e + "Resources.Load(\"Levels/First\")));\n"
                + "        " + e + "TextAsset big = (" + e + "TextAsset) " + e + "Resources.Load(\"big\");\n"
                + "        System.out.println(\"big \" + big.get_text().length() + \" \" + big.get_text().hashCode());\n"
                + "        byte[] blob = ((" + e + "TextAsset) " + e + "Resources.Load(\"blob\")).get_bytes();\n"
                + "        int sum = 0;\n"
                + "        for (int i = 0; i < blob.length; i++) {\n"
                + "            sum += (blob[i] & 255) == i ? 1 : 0;\n"
                + "        }\n"
                + "        System.out.println(\"blob \" + blob.length + \" \" + sum);\n"
                + "        System.out.println(\"deep \" + ((" + e + "TextAsset) " + e
                + "Resources.Load(\"Deep/Down\")).get_text());\n"
                + "        " + e + "Sprite tri = (" + e + "Sprite) " + e + "Resources.Load(\"Icons/tri\", " + e
                + "Sprite.class);\n"
                + "        System.out.println(\"tri \" + tri.get_name() + \" \" + tri.$resource() + \" \" + tri.$width()"
                + " + \" \" + tri.get_pixelsPerUnit());\n"
                + "        System.out.println(\"wrong type \" + " + e + "Resources.Load(\"Icons/tri\", " + e
                + "TextAsset.class));\n"
                + "        System.out.println(\"missing \" + " + e + "Resources.Load(\"model\") + \" \" + " + e
                + "Resources.Load(\"outside\") + \" \" + " + e + "Resources.Load(\"Levels/First.txt\") + \" \" + " + e
                + "Resources.Load(\"Resources/big\") + \" \" + " + e + "Resources.Load((String) null));\n"
                + "    }\n"
                + "}\n");
        File main = new File(work, "main");
        UnityToolchain.javac(work, "javac-generated", main, UnityToolchain.path(runtime, valueClasses, app, core),
                generated);
        UnityToolchain.Result run = UnityToolchain.java(work, "run",
                UnityToolchain.path(main, app, valueClasses, runtime, core), "probe.Probe");
        run.assertOk("the probe");
        List<String> out = UnityToolchain.lines(run.out);
        StringBuilder codes = new StringBuilder();
        String shown = text.substring(1);
        for (int i = 0; i < shown.length(); i++) {
            codes.append((int) shown.charAt(i)).append(' ');
        }
        Assert.assertEquals(Arrays.asList(
                "first First " + codes,
                "first bytes " + shown.getBytes(StandardCharsets.UTF_8).length,
                "same true",
                "big " + longText.length() + " " + longText.toString().hashCode(),
                "blob 256 256",
                "deep <deep/>",
                "tri tri unity-polygon-tri.png 256 512.0",
                "wrong type null",
                "missing null null null null null"), out);
    }
}
