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

        assertEquals("the unreadable source is reported, not thrown",
                Arrays.asList("anim.lottie"), runner.getFailures());

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

    private static void write(File f, byte[] bytes) throws Exception {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }
}
