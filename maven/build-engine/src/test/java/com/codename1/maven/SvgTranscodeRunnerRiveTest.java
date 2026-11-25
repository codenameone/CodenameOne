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
package com.codename1.maven;

import com.codename1.build.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `.riv` files sit where SVGs do -- next to the theme CSS in `src/main/css` --
/// and become `RiveFile` subclasses registered under their file name, so
/// `Resources.getImage("juice.riv")` and CSS `url(juice.riv)` find them.
public class SvgTranscodeRunnerRiveTest {
    private static final String REGISTERED =
            "Resources.registerGeneratedImage(\"juice.riv\", new " + SvgTranscodeRunner.RIVE_IMAGE_CLASS
                    + "(new com.codename1.generated.rive.JuiceRive()));";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void aRiveFileNextToTheThemeIsTranslatedAndRegistered() throws Exception {
        File basedir = tmp.newFolder("app");
        File css = new File(basedir, "src/main/css");
        copyFixture(new File(css, "fx"), "juice.riv", "juice.riv");
        Files.write(new File(css, "theme.css").toPath(),
                "Splash { background: url(juice.riv); }\n".getBytes("US-ASCII"));
        File out = new File(basedir, "target/generated-sources/svg");
        File pkg = new File(out, "com/codename1/generated/rive");
        assertTrue(pkg.mkdirs());
        File stale = new File(pkg, "GoneRive.java");
        Files.write(stale.toPath(), new byte[]{'x'});

        assertTrue(SvgTranscodeRunner.hasRiveSources(basedir));
        SvgTranscodeRunner runner = new SvgTranscodeRunner(basedir, null, out,
                new File(basedir, "target/css-resources"), null, new SystemStreamLog());
        assertTrue(runner.hasVectorSources());
        runner.run();

        File generated = new File(pkg, "JuiceRive.java");
        String src = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        assertTrue(src.contains("package com.codename1.generated.rive;"));
        assertTrue(src.contains("class JuiceRive extends "));
        assertFalse(stale.exists());
        assertEquals(1, runner.getTranscodedCount());
        // a Rive file alone is enough for a registry, and it is registered by file name
        String registry = new String(Files.readAllBytes(
                SvgTranscodeRunner.registrySourceFile(out, null).toPath()), "UTF-8");
        assertTrue(registry, registry.contains(REGISTERED));
        // the CSS compiler resolves url(juice.riv) through a placeholder, like an SVG
        assertTrue(new File(basedir, "target/css-resources/juice.riv").isFile());
    }

    @Test
    public void rivAndSvgShareOneRegistry() throws Exception {
        File basedir = tmp.newFolder("mixed");
        File css = new File(basedir, "src/main/css");
        copyFixture(css, "juice.riv", "juice.riv");
        Files.write(new File(css, "dot.svg").toPath(), ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"4\""
                + " height=\"4\"><rect width=\"4\" height=\"4\"/></svg>").getBytes("US-ASCII"));
        File out = new File(basedir, "target/generated-sources/svg");
        transcode(basedir, out);
        String registry = new String(Files.readAllBytes(
                SvgTranscodeRunner.registrySourceFile(out, null).toPath()), "UTF-8");
        assertTrue(registry, registry.contains(REGISTERED));
        assertTrue(registry, registry.contains("\"dot.svg\""));
    }

    @Test
    public void aModuleWithoutRiveFilesHasNone() throws Exception {
        File basedir = tmp.newFolder("none");
        assertTrue(new File(basedir, "src/main/css").mkdirs());
        assertFalse(SvgTranscodeRunner.hasRiveSources(basedir));
    }

    @Test
    public void classNamesComeFromTheFileName() {
        assertEquals("CoinRive", SvgTranscodeRunner.riveClassNameFor("coin.riv"));
        assertEquals("OffRoadCarRive", SvgTranscodeRunner.riveClassNameFor("off_road_car.riv"));
        assertEquals("HeroRive", SvgTranscodeRunner.riveClassNameFor("hero-rive.riv"));
        // generated sources are ASCII only: a letter outside ASCII folds to '_'
        String folded = SvgTranscodeRunner.riveClassNameFor("caf\u00e9.riv");
        assertEquals("Caf_Rive", folded);
    }

    @Test
    public void oneNameInTwoDirectoriesIsOneImage() throws Exception {
        // registered by file name, so the first source directory wins, as for SVG
        File basedir = tmp.newFolder("dup");
        copyFixture(new File(basedir, "src/main/svg"), "juice.riv", "juice.riv");
        copyFixture(new File(basedir, "src/main/css/a"), "juice.riv", "tape.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        transcode(basedir, out);
        File pkg = new File(out, "com/codename1/generated/rive");
        assertTrue(new File(pkg, "JuiceRive.java").isFile());
        assertFalse(new File(pkg, "JuiceRive2.java").exists());
        String src = new String(Files.readAllBytes(new File(pkg, "JuiceRive.java").toPath()), "US-ASCII");
        assertTrue(src.contains(SvgTranscodeRunner.riveSourceLine("/src/main/svg/juice.riv",
                Files.readAllBytes(new File(basedir, "src/main/svg/juice.riv").toPath()))));
    }

    @Test
    public void aClassFromAnotherTranscoderIsRegenerated() throws Exception {
        File basedir = tmp.newFolder("stamp");
        File riv = copyFixture(new File(basedir, "src/main/css"), "juice.riv", "juice.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        File pkg = new File(out, "com/codename1/generated/rive");
        assertTrue(pkg.mkdirs());
        File generated = new File(pkg, "JuiceRive.java");
        Files.write(generated.toPath(), "// Generated\n// rive-transcoder: old\n".getBytes("US-ASCII"));
        assertTrue(generated.setLastModified(riv.lastModified() + 60000L));
        transcode(basedir, out);
        String src = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        assertTrue(src.contains("// rive-transcoder: "
                + com.codename1.rive.transcoder.RiveTranscoder.stamp()));
        assertTrue(src.contains("class JuiceRive extends"));
    }

    @Test
    public void aSourceReplacedByAnOlderFileIsRegenerated() throws Exception {
        File basedir = tmp.newFolder("older");
        File css = new File(basedir, "src/main/css");
        copyFixture(css, "anim.riv", "juice.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        transcode(basedir, out);
        File generated = new File(out, "com/codename1/generated/rive/AnimRive.java");
        String first = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        File riv = copyFixture(css, "anim.riv", "tape.riv");
        assertTrue(riv.setLastModified(generated.lastModified() - 3600000L));
        transcode(basedir, out);
        String second = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        byte[] tape = Files.readAllBytes(riv.toPath());
        assertFalse(first.equals(second));
        assertTrue(second.contains(SvgTranscodeRunner.riveSourceLine("/src/main/css/anim.riv", tape)));
    }

    @Test
    public void theRuntimeIsCopiedIntoTheClassesDirectory() throws Exception {
        File jar = new File(tmp.getRoot(), "rive-runtime.jar");
        String image = SvgTranscodeRunner.RIVE_IMAGE_CLASS.replace('.', '/') + ".class";
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar))) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes("US-ASCII"));
            zip.putNextEntry(new ZipEntry("com/codename1/impl/rive/"));
            zip.putNextEntry(new ZipEntry(image));
            zip.write(new byte[]{(byte) 0xCA, (byte) 0xFE});
        }
        File classes = tmp.newFolder("classes");
        assertEquals(1, SvgTranscodeRunner.installRuntime(jar, classes));
        assertTrue(new File(classes, image).isFile());
        assertFalse(new File(classes, "META-INF/MANIFEST.MF").exists());
        // the same jar again is a no-op; a class removed by a clean build is restored
        assertEquals(0, SvgTranscodeRunner.installRuntime(jar, classes));
        assertTrue(new File(classes, image).delete());
        assertEquals(1, SvgTranscodeRunner.installRuntime(jar, classes));
    }

    @Test
    public void aClassTheNextRuntimeDroppedIsRemoved() throws Exception {
        String image = SvgTranscodeRunner.RIVE_IMAGE_CLASS.replace('.', '/') + ".class";
        String gone = "com/codename1/impl/rive/Dropped.class";
        File older = runtimeJar("older.jar", image, gone);
        File newer = runtimeJar("newer.jar", image);
        assertTrue(newer.setLastModified(older.lastModified() + 10000));
        File classes = tmp.newFolder("upgrade");
        assertEquals(2, SvgTranscodeRunner.installRuntime(older, classes));
        assertTrue(new File(classes, gone).isFile());
        assertEquals(1, SvgTranscodeRunner.installRuntime(newer, classes));
        assertFalse(new File(classes, gone).exists());
        assertTrue(new File(classes, image).isFile());
    }

    private File runtimeJar(String name, String... classes) throws Exception {
        File jar = new File(tmp.getRoot(), name);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar))) {
            for (String c : classes) {
                zip.putNextEntry(new ZipEntry(c));
                zip.write(new byte[]{(byte) 0xCA, (byte) 0xFE});
            }
        }
        return jar;
    }

    private File copyFixture(File dir, String name, String fixture) throws Exception {
        assertTrue(dir.isDirectory() || dir.mkdirs());
        File riv = new File(dir, name);
        // the fixtures are rive-runtime test assets (MIT); see the transcoder's test resources
        Files.copy(new File("../rive-transcoder/src/test/resources/rive/" + fixture).toPath(), riv.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        return riv;
    }

    private static void transcode(File basedir, File out) throws Exception {
        new SvgTranscodeRunner(basedir, null, out, new File(basedir, "target/css-resources"), null,
                new SystemStreamLog()).run();
    }
}
