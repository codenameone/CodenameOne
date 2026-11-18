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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `.riv` files under `src/main/rive` become `RiveFile` subclasses in the
/// transcode step both build tools run.
public class SvgTranscodeRunnerRiveTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void translatesEveryRiveFileAndRemovesStaleOutput() throws Exception {
        File basedir = tmp.newFolder("app");
        File rive = new File(basedir, "src/main/rive/fx");
        assertTrue(rive.mkdirs());
        File riv = new File(rive, "juice.riv");
        // juice.riv is a rive-runtime test asset (MIT); see the transcoder's test resources
        InputStream in = new java.io.FileInputStream(new File("../rive-transcoder/src/test/resources/rive/juice.riv"));
        try {
            Files.copy(in, riv.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            in.close();
        }
        File out = new File(basedir, "target/generated-sources/svg");
        File pkg = new File(out, "com/codename1/generated/rive");
        assertTrue(pkg.mkdirs());
        File stale = new File(pkg, "GoneRive.java");
        Files.write(stale.toPath(), new byte[]{'x'});

        SvgTranscodeRunner runner = new SvgTranscodeRunner(basedir, null, out,
                new File(basedir, "target/css-resources"), null, new SystemStreamLog());
        assertTrue(runner.hasVectorSources());
        runner.run();

        File generated = new File(pkg, "JuiceRive.java");
        assertTrue(generated.isFile());
        String src = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        assertTrue(src.contains("package com.codename1.generated.rive;"));
        assertTrue(src.contains("class JuiceRive extends com.codename1.rive.RiveFile"));
        assertFalse(stale.exists());
        assertEquals(1, runner.getTranscodedCount());
        // no SVG sources: the SVG registry must not appear
        assertFalse(SvgTranscodeRunner.registrySourceFile(out, null).exists());
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

    private File copyJuice(File dir, String name) throws Exception {
        assertTrue(dir.isDirectory() || dir.mkdirs());
        File riv = new File(dir, name);
        InputStream in = new java.io.FileInputStream(new File("../rive-transcoder/src/test/resources/rive/juice.riv"));
        try {
            Files.copy(in, riv.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            in.close();
        }
        return riv;
    }

    @Test
    public void sameNameInTwoFoldersIsNamedByRelativePath() throws Exception {
        File basedir = tmp.newFolder("dup");
        File riveDir = new File(basedir, "src/main/rive");
        copyJuice(new File(riveDir, "b"), "juice.riv");
        copyJuice(new File(riveDir, "a"), "juice.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        new SvgTranscodeRunner(basedir, null, out, new File(basedir, "target/css-resources"), null,
                new SystemStreamLog()).run();
        File pkg = new File(out, "com/codename1/generated/rive");
        assertTrue(new File(pkg, "JuiceRive.java").isFile());
        assertTrue(new File(pkg, "JuiceRive2.java").isFile());
    }

    @Test
    public void aClassFromAnotherTranscoderIsRegenerated() throws Exception {
        File basedir = tmp.newFolder("stamp");
        File riv = copyJuice(new File(basedir, "src/main/rive"), "juice.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        File pkg = new File(out, "com/codename1/generated/rive");
        assertTrue(pkg.mkdirs());
        File generated = new File(pkg, "JuiceRive.java");
        Files.write(generated.toPath(), "// Generated\n// rive-transcoder: old\n".getBytes("US-ASCII"));
        assertTrue(generated.setLastModified(riv.lastModified() + 60000L));
        new SvgTranscodeRunner(basedir, null, out, new File(basedir, "target/css-resources"), null,
                new SystemStreamLog()).run();
        String src = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        assertTrue(src.contains("// rive-transcoder: "
                + com.codename1.rive.transcoder.RiveTranscoder.stamp()));
        assertTrue(src.contains("class JuiceRive extends"));
    }

    private File copyFixture(File dir, String name, String fixture) throws Exception {
        assertTrue(dir.isDirectory() || dir.mkdirs());
        File riv = new File(dir, name);
        Files.copy(new File("../rive-transcoder/src/test/resources/rive/" + fixture).toPath(), riv.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        return riv;
    }

    private static void transcode(File basedir, File out) throws Exception {
        new SvgTranscodeRunner(basedir, null, out, new File(basedir, "target/css-resources"), null,
                new SystemStreamLog()).run();
    }

    @Test
    public void aSourceReplacedByAnOlderFileIsRegenerated() throws Exception {
        File basedir = tmp.newFolder("older");
        File riveDir = new File(basedir, "src/main/rive");
        copyFixture(riveDir, "anim.riv", "juice.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        transcode(basedir, out);
        File generated = new File(out, "com/codename1/generated/rive/AnimRive.java");
        String first = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        File riv = copyFixture(riveDir, "anim.riv", "tape.riv");
        assertTrue(riv.setLastModified(generated.lastModified() - 3600000L));
        transcode(basedir, out);
        String second = new String(Files.readAllBytes(generated.toPath()), "US-ASCII");
        byte[] tape = Files.readAllBytes(riv.toPath());
        assertFalse(first.equals(second));
        assertTrue(second.contains(SvgTranscodeRunner.riveSourceLine("/anim.riv", tape)));
    }

    @Test
    public void classesFollowTheirSourceWhenNamesShift() throws Exception {
        File basedir = tmp.newFolder("shift");
        File riveDir = new File(basedir, "src/main/rive");
        copyFixture(new File(riveDir, "b"), "juice.riv", "juice.riv");
        File out = new File(basedir, "target/generated-sources/svg");
        transcode(basedir, out);
        File pkg = new File(out, "com/codename1/generated/rive");
        File first = new File(pkg, "JuiceRive.java");
        // a/juice.riv now sorts first and takes the plain name, with an old timestamp
        File a = copyFixture(new File(riveDir, "a"), "juice.riv", "tape.riv");
        assertTrue(a.setLastModified(first.lastModified() - 3600000L));
        transcode(basedir, out);
        String plain = new String(Files.readAllBytes(first.toPath()), "US-ASCII");
        String second = new String(Files.readAllBytes(new File(pkg, "JuiceRive2.java").toPath()), "US-ASCII");
        assertTrue(plain.contains(SvgTranscodeRunner.riveSourceLine("/a/juice.riv", Files.readAllBytes(a.toPath()))));
        assertTrue(second.contains(SvgTranscodeRunner.riveSourceLine("/b/juice.riv",
                Files.readAllBytes(new File(riveDir, "b/juice.riv").toPath()))));
        assertTrue(second.contains("class JuiceRive2 extends"));
    }
}
