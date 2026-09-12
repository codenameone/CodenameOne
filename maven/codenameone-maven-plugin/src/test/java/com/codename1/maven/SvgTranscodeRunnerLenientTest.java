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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The automatic repair must never turn a passing build into a failing one.
 *
 * <p>A {@code .lottie} is a ZIP that the JSON-only parser cannot read, and any
 * unrelated {@code .json} sitting in a vector directory looks like a Lottie by
 * extension alone. Aborting on those is right for a goal the developer bound
 * themselves and wrong for a repair that ran on its own initiative in a project
 * that never asked for a transcoder.</p>
 */
public class SvgTranscodeRunnerLenientTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static final String STAR_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 16 16\">"
            + "<path d=\"M8 1 L10 6 L15 6 L11 9 L13 14 L8 11 L3 14 L5 9 L1 6 L6 6 Z\" fill=\"#ff0000\"/>"
            + "</svg>";

    private File newProject() throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, "star.svg"), STAR_SVG.getBytes("UTF-8"));
        // A dotLottie archive: a ZIP, not JSON.
        write(new File(css, "anim.lottie"), new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0});
        return basedir;
    }

    private SvgTranscodeRunner runner(File basedir, boolean lenient) {
        return new SvgTranscodeRunner(basedir, null,
                new File(basedir, "target/generated-sources/svg"),
                new File(basedir, "target/css-resources"),
                null, new SystemStreamLog(), lenient);
    }

    @Test
    public void strictRunStillFailsOnAnUnreadableSource() throws Exception {
        File basedir = newProject();
        try {
            runner(basedir, false).run();
            fail("a goal the developer bound must report an unreadable source");
        } catch (MojoExecutionException expected) {
            assertTrue("names the offending file: " + expected.getMessage(),
                    expected.getMessage().contains("anim.lottie"));
        }
    }

    @Test
    public void lenientRunSkipsItAndTranscodesTheRest() throws Exception {
        File basedir = newProject();
        SvgTranscodeRunner runner = runner(basedir, true);
        runner.run();

        assertTrue("nothing is thrown, and nothing failed mid-transcode",
                runner.getFailures().isEmpty());
        assertEquals("the archive is declined before it is ever parsed",
                Arrays.asList("anim.lottie"), runner.getNonVectorInputs());

        File pkg = new File(basedir, "target/generated-sources/svg/com/codename1/generated/svg");
        assertTrue("the readable source is still transcoded",
                new File(pkg, "Star.java").isFile());
        assertTrue("a registry is still produced",
                new File(pkg, "SVGRegistry.java").isFile());
        assertFalse("no half-written class is left behind for the failure",
                new File(pkg, "Anim.java").isFile());
    }

    @Test
    public void failuresAreClearedBetweenRuns() throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, "star.svg"), STAR_SVG.getBytes("UTF-8"));
        SvgTranscodeRunner runner = runner(basedir, true);
        runner.run();
        assertTrue("a clean project reports nothing", runner.getFailures().isEmpty());
    }

    /**
     * LottieParser reads "layers" and returns an empty document when there is
     * none, so it accepts any JSON object -- an unrelated config file under
     * src/main/css transcodes "successfully". Binding the strict goal to that
     * file means the first edit turning it into an array, or saving it
     * half-written, fails every build over something that was never an
     * animation. The lenient path declines it instead.
     */
    @Test
    public void declinesAJsonThatIsNotAnAnimation() throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, "star.svg"), STAR_SVG.getBytes("UTF-8"));
        write(new File(css, "config.json"),
                "{\"apiBase\":\"https://example.com\"}".getBytes("UTF-8"));

        SvgTranscodeRunner runner = runner(basedir, true);
        runner.run();

        assertTrue("it is not a transcode failure", runner.getFailures().isEmpty());
        assertEquals("it is declined as not an animation",
                Arrays.asList("config.json"), runner.getNonVectorInputs());
        File pkg = new File(basedir, "target/generated-sources/svg/com/codename1/generated/svg");
        assertFalse("no class is emitted for it", new File(pkg, "Config.java").isFile());
        assertTrue("the real asset is unaffected", new File(pkg, "Star.java").isFile());
    }

    /** A genuine Lottie is still accepted. */
    @Test
    public void acceptsARealLottie() throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, "spin.json"),
                ("{\"v\":\"5.7.4\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":64,\"h\":64,"
                        + "\"layers\":[]}").getBytes("UTF-8"));

        SvgTranscodeRunner runner = runner(basedir, true);
        runner.run();

        assertTrue("not declined: " + runner.getNonVectorInputs(),
                runner.getNonVectorInputs().isEmpty());
        assertTrue(runner.getFailures().isEmpty());
        assertTrue(new File(basedir,
                "target/generated-sources/svg/com/codename1/generated/svg/Spin.java").isFile());
    }

    /**
     * A substring search for the word "layers" matched it nested inside another
     * property, or inside an unrelated string value. The parser only ever looks
     * at a top-level array, so that is the question to ask.
     */
    @Test
    public void declinesJsonWhoseLayersIsNotATopLevelArray() throws Exception {
        String[] impostors = {
                "{\"config\":{\"layers\":[1,2]}}",
                "{\"note\":\"we render layers here\"}",
                "{\"layers\":\"not-an-array\"}",
                "{\"layers\":{\"a\":1}}",
        };
        for (String json : impostors) {
            File basedir = temp.newFolder();
            File css = new File(basedir, "src/main/css");
            assertTrue(css.mkdirs());
            write(new File(css, "thing.json"), json.getBytes("UTF-8"));

            SvgTranscodeRunner runner = runner(basedir, true);
            runner.run();
            assertEquals("declined: " + json,
                    Arrays.asList("thing.json"), runner.getNonVectorInputs());
        }
    }

    /**
     * When every candidate is rejected there is nothing to register, and an
     * empty registry is not harmless: it carries the one fixed name the
     * per-platform builders look for, so it would be wired into the stub and
     * run, shadowing a dependency's registry and leaving that library's images
     * as placeholders.
     */
    @Test
    public void emitsNoRegistryWhenEverythingWasRejected() throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, "config.json"), "{\"apiBase\":\"x\"}".getBytes("UTF-8"));
        write(new File(css, "anim.lottie"), new byte[]{'P', 'K', 3, 4});

        SvgTranscodeRunner runner = runner(basedir, true);
        runner.run();

        assertEquals(2, runner.getNonVectorInputs().size());
        assertFalse("no registry is written when it would register nothing",
                new File(basedir,
                        "target/generated-sources/svg/com/codename1/generated/svg/SVGRegistry.java")
                        .isFile());
    }

    /** A stale registry from an earlier build is swept rather than left behind. */
    @Test
    public void sweepsAStaleRegistryWhenEverythingWasRejected() throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, "star.svg"), STAR_SVG.getBytes("UTF-8"));
        SvgTranscodeRunner runner = runner(basedir, true);
        runner.run();
        File registry = new File(basedir,
                "target/generated-sources/svg/com/codename1/generated/svg/SVGRegistry.java");
        assertTrue("precondition: a registry exists", registry.isFile());

        assertTrue(new File(css, "star.svg").delete());
        write(new File(css, "config.json"), "{\"apiBase\":\"x\"}".getBytes("UTF-8"));
        runner(basedir, true).run();
        assertFalse("the stale registry is removed", registry.isFile());
    }

    private static void write(File f, byte[] bytes) throws Exception {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }
}
