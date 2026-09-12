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
 * The automatic repair must never turn a passing build into a failing one, and
 * it decides that before it starts rather than by behaving differently once it
 * has.
 *
 * <p>A {@code .lottie} is a ZIP that the JSON-only parser cannot read, and any
 * unrelated {@code .json} sitting in a vector directory is claimed by extension
 * alone. The transcoder reports both and stops -- one behaviour, the same one
 * the bound goal has. {@link SvgTranscodeRunner#unreadableSources()} is how a
 * caller that must not break a build finds that out in advance.</p>
 */
public class SvgTranscodeRunnerScreeningTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static final String STAR_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 16 16\">"
            + "<path d=\"M8 1 L10 6 L15 6 L11 9 L13 14 L8 11 L3 14 L5 9 L1 6 L6 6 Z\" fill=\"#ff0000\"/>"
            + "</svg>";

    @Test
    public void runFailsOnAnUnreadableSource() throws Exception {
        File basedir = projectWithAnArchive();
        try {
            runner(basedir).run();
            fail("the transcoder must report a source it cannot read");
        } catch (MojoExecutionException expected) {
            assertTrue("names the offending file: " + expected.getMessage(),
                    expected.getMessage().contains("anim.lottie"));
        }
    }

    /** Which is why the caller asks first, and so never reaches that failure. */
    @Test
    public void screeningNamesTheUnreadableSourceUpFront() throws Exception {
        assertEquals(Arrays.asList("anim.lottie"),
                runner(projectWithAnArchive()).unreadableSources());
    }

    @Test
    public void aCleanProjectScreensCleanAndTranscodes() throws Exception {
        File basedir = projectWith("star.svg", STAR_SVG.getBytes("UTF-8"));
        SvgTranscodeRunner runner = runner(basedir);
        assertTrue(runner.unreadableSources().isEmpty());
        runner.run();
        assertTrue(generated(basedir, "Star.java").isFile());
    }

    /**
     * LottieParser reads "layers" and returns an empty document when there is
     * none, so it accepts any JSON object -- an unrelated config file under
     * src/main/css would transcode "successfully" and then be bound to the
     * strict goal forever. Screening asks what the parser does not.
     */
    @Test
    public void screensOutAJsonThatIsNotAnAnimation() throws Exception {
        File basedir = projectWith("star.svg", STAR_SVG.getBytes("UTF-8"));
        write(new File(basedir, "src/main/css/config.json"),
                "{\"apiBase\":\"https://example.com\"}".getBytes("UTF-8"));
        assertEquals(Arrays.asList("config.json"), runner(basedir).unreadableSources());
    }

    /**
     * A substring search for "layers" matched it nested inside another property
     * or sitting in an unrelated string value. Only a top-level array counts.
     */
    @Test
    public void screensOutJsonWhoseLayersIsNotATopLevelArray() throws Exception {
        String[] impostors = {
                "{\"config\":{\"layers\":[1,2]}}",
                "{\"note\":\"we render layers here\"}",
                "{\"layers\":\"not-an-array\"}",
                "{\"layers\":{\"a\":1}}",
        };
        for (String json : impostors) {
            File basedir = projectWith("thing.json", json.getBytes("UTF-8"));
            assertEquals("screened out: " + json,
                    Arrays.asList("thing.json"), runner(basedir).unreadableSources());
        }
    }

    /** A genuine Lottie passes screening and transcodes. */
    @Test
    public void acceptsARealLottie() throws Exception {
        File basedir = projectWith("spin.json",
                ("{\"v\":\"5.7.4\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":64,\"h\":64,"
                        + "\"layers\":[]}").getBytes("UTF-8"));
        SvgTranscodeRunner runner = runner(basedir);
        assertTrue("not screened out: " + runner.unreadableSources(),
                runner.unreadableSources().isEmpty());
        runner.run();
        assertTrue(generated(basedir, "Spin.java").isFile());
    }

    /**
     * A registry left by an earlier build is swept once the sources are gone.
     * It carries the one fixed name the per-platform builders look for, so a
     * stale one would be wired into the stub and run, shadowing a dependency's
     * registry and leaving that library's images as placeholders.
     */
    @Test
    public void sweepsAStaleRegistryWhenTheSourcesAreGone() throws Exception {
        File basedir = projectWith("star.svg", STAR_SVG.getBytes("UTF-8"));
        runner(basedir).run();
        File registry = generated(basedir, "SVGRegistry.java");
        assertTrue("precondition: a registry exists", registry.isFile());

        assertTrue(new File(basedir, "src/main/css/star.svg").delete());
        runner(basedir).run();
        assertFalse("the stale registry is removed", registry.isFile());
    }

    // ---- helpers -----------------------------------------------------

    private File projectWith(String name, byte[] content) throws Exception {
        File basedir = temp.newFolder();
        File css = new File(basedir, "src/main/css");
        assertTrue(css.mkdirs());
        write(new File(css, name), content);
        return basedir;
    }

    private File projectWithAnArchive() throws Exception {
        File basedir = projectWith("star.svg", STAR_SVG.getBytes("UTF-8"));
        // A dotLottie archive: a ZIP, not JSON.
        write(new File(basedir, "src/main/css/anim.lottie"),
                new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0});
        return basedir;
    }

    private static File generated(File basedir, String name) {
        return new File(basedir,
                "target/generated-sources/svg/com/codename1/generated/svg/" + name);
    }

    private SvgTranscodeRunner runner(File basedir) {
        return new SvgTranscodeRunner(basedir, null,
                new File(basedir, "target/generated-sources/svg"),
                new File(basedir, "target/css-resources"),
                null, new SystemStreamLog());
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
