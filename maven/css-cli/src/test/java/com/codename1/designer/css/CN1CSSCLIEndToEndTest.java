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
package com.codename1.designer.css;

import com.codename1.ui.util.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the compiler the way a build does -- through [CN1CSSCLI#run] with a
/// command line -- and reads the `.res` it wrote.
///
/// The whole class runs with `java.awt.headless=true` (the module's surefire
/// configuration), so a compile that reached for a window or a dialog would
/// fail here on any machine, not only on a server without a display.
class CN1CSSCLIEndToEndTest {

    private PrintStream realOut;
    private PrintStream realErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void captureOutput() throws Exception {
        realOut = System.out;
        realErr = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, "UTF-8"));
        System.setErr(new PrintStream(err, true, "UTF-8"));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(realOut);
        System.setErr(realErr);
        System.clearProperty("parent.port");
    }

    private String stdout() throws Exception {
        System.out.flush();
        synchronized (out) {
            return out.toString("UTF-8");
        }
    }

    private String stderr() throws Exception {
        System.err.flush();
        return err.toString("UTF-8");
    }

    private static File write(File f, String content) throws Exception {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static File png(File f, int w, int h, int argb) throws Exception {
        f.getParentFile().mkdirs();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        assertTrue(ImageIO.write(img, "png", f));
        return f;
    }

    private static Resources open(File res) throws Exception {
        InputStream in = new FileInputStream(res);
        try {
            return Resources.open(in);
        } finally {
            in.close();
        }
    }

    /// A colour read back through the runtime loader is lower-case hex with
    /// no leading zeros, which is why the expectations below look as they do.
    private static Hashtable theme(File res) throws Exception {
        Resources r = open(res);
        Hashtable theme = r.getTheme("Theme");
        assertNotNull(theme, "the compiled file holds a theme named Theme");
        return theme;
    }

    /// A Maven project: the only layout in which the compiler keeps state
    /// (the lock, the selector cache) and skips an up-to-date compile.
    private static File mavenProject(File root) throws Exception {
        write(new File(root, "pom.xml"), "<project/>");
        File common = new File(root, "common");
        write(new File(common, "pom.xml"), "<project/>");
        write(new File(common, "codenameone_settings.properties"), "codename1.cssTheme=true\n");
        return common;
    }

    @Test
    void theTestJvmIsHeadless() {
        assertTrue(GraphicsEnvironment.isHeadless(),
                "these tests prove nothing about a display unless they run with java.awt.headless=true");
    }

    @Test
    void compilesAStylesheetIntoAThemeWithNoSideFiles(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File css = write(new File(dir, "theme.css"), "Label { color: #ff0000; }\n");
        File res = new File(dir, "out/theme.res");

        assertEquals(0, CN1CSSCLI.run(new String[]{"-input", css.getPath(), "-output", res.getPath()}), stderr());

        assertEquals("ff0000", theme(res).get("Label.fgColor"));
        // Outside a project there is nowhere to keep state, so there must be none:
        // the framework's own themes are compiled this way and committed.
        assertEquals(new TreeSet<String>(Arrays.asList("out", "theme.css")),
                new TreeSet<String>(Arrays.asList(dir.list())), "nothing but the input and the output directory");
        assertArrayEquals(new String[]{"theme.res"}, new File(dir, "out").list());

        byte[] first = Files.readAllBytes(res.toPath());
        assertEquals(0, CN1CSSCLI.run(new String[]{"-input", css.getPath(), "-output", res.getPath()}), stderr());
        assertArrayEquals(first, Files.readAllBytes(res.toPath()), "the same input compiles to the same bytes");
    }

    @Test
    void theLegacyArgumentsOlderCallersPassAreIgnored(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File css = write(new File(dir, "theme.css"), "Label { color: #00ff00; }\n");
        File res = new File(dir, "theme.res");

        // An older simulator or plugin still leads with -css, and older scripts
        // pass -stateless and -no-cef. None of them means anything any more, but
        // rejecting them would break a mixed-version build.
        assertEquals(0, CN1CSSCLI.run(new String[]{"-css", "-stateless", "-no-cef",
            "-input", css.getPath(), "-output", res.getPath()}), stderr());
        assertEquals("ff00", theme(res).get("Label.fgColor"));
    }

    @Test
    void mergesLibraryCssAheadOfTheApplicationAndCarriesItsImages(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File lib = write(new File(dir, "lib/theme.css"),
                "LibBadge { background-image: url(img/a.png); cn1-source-dpi: 160; }\nShared { color: #111111; }\n");
        png(new File(dir, "lib/img/a.png"), 8, 6, 0xff336699);
        File app = write(new File(dir, "app/theme.css"), "Shared { color: #222222; }\n");
        File merged = new File(dir, "work/theme.css.merged");
        File res = new File(dir, "work/theme.res");

        assertEquals(0, CN1CSSCLI.run(new String[]{"-input", lib.getPath() + "," + app.getPath(),
            "-output", res.getPath(), "-merge", merged.getPath()}), stderr());

        String mergedText = new String(Files.readAllBytes(merged.toPath()), StandardCharsets.UTF_8);
        File[] mirrors = new File(dir, "work/cn1-merged-files").listFiles();
        assertNotNull(mirrors);
        File libMirror = null;
        for (File mirror : mirrors) {
            if (new File(mirror, "img/a.png").isFile()) {
                libMirror = mirror;
            }
        }
        assertNotNull(libMirror, "the library's directory is mirrored beside the merged file");
        assertTrue(mergedText.contains("url(\"cn1-merged-files/" + libMirror.getName() + "/img/a.png\")"),
                "the library's url() points at the mirror, was: " + mergedText);

        Resources r = open(res);
        assertTrue(Arrays.asList(r.getImageResourceNames()).contains("a.png"),
                "the library's image is in the theme: " + Arrays.toString(r.getImageResourceNames()));
        assertEquals(8, r.getImage("a.png").getWidth(), "and it decodes to its real size");
        // The application's rule comes last, so it wins.
        assertEquals("222222", r.getTheme("Theme").get("Shared.fgColor"));
    }

    @Test
    void aMergedBuildCarriesTheImagesOfAnImportFromOutsideTheInputDirectory(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        write(new File(dir, "shared/base.css"),
                "SharedBadge { background-image: url(img/b.png); cn1-source-dpi: 160; }\n");
        png(new File(dir, "shared/img/b.png"), 10, 4, 0xff993366);
        File app = write(new File(dir, "app/css/theme.css"),
                "@import \"../../shared/base.css\";\nLabel { color: #222222; }\n");
        File merged = new File(dir, "work/theme.css.merged");
        File res = new File(dir, "work/theme.res");

        assertEquals(0, CN1CSSCLI.run(new String[]{"-input", app.getPath(),
            "-output", res.getPath(), "-merge", merged.getPath()}), stderr());

        String mergedText = new String(Files.readAllBytes(merged.toPath()), StandardCharsets.UTF_8);
        assertFalse(mergedText.contains("../"), "no url() climbs out of the mirror, was: " + mergedText);
        Resources r = open(res);
        assertTrue(Arrays.asList(r.getImageResourceNames()).contains("b.png"),
                "the imported stylesheet's image is in the theme: " + Arrays.toString(r.getImageResourceNames()));
        assertEquals(10, r.getImage("b.png").getWidth(), "and it decodes to its real size");
    }

    @Test
    void anEditToAnImageAloneRecompilesAMergedBuild(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File app = write(new File(dir, "app/theme.css"),
                "Badge { background-image: url(img/a.png); cn1-source-dpi: 160; }\n");
        File image = png(new File(dir, "app/img/a.png"), 8, 6, 0xff336699);
        File merged = new File(dir, "work/theme.css.merged");
        File res = new File(dir, "work/theme.res");
        String[] args = {"-input", app.getPath(), "-output", res.getPath(), "-merge", merged.getPath()};
        assertEquals(0, CN1CSSCLI.run(args), stderr());
        assertEquals(8, open(res).getImage("a.png").getWidth());

        png(image, 20, 6, 0xff336699);
        assertTrue(image.setLastModified(res.lastModified() + 5000));
        assertEquals(0, CN1CSSCLI.run(args), stderr());

        assertEquals(20, open(res).getImage("a.png").getWidth(), "the new image is in the theme");
    }

    @Test
    void anImportIsCompiledAndAnEditToItRecompiles(@TempDir Path tmp) throws Exception {
        File common = mavenProject(tmp.toFile());
        File css = write(new File(common, "src/main/css/theme.css"),
                "@import \"parts/colors.css\";\nLabel { color: #000001; }\n");
        File part = write(new File(common, "src/main/css/parts/colors.css"), "Imported { color: #abcdef; }\n");
        File res = new File(common, "target/classes/theme.res");
        String[] args = {"-input", css.getPath(), "-output", res.getPath()};

        assertEquals(0, CN1CSSCLI.run(args), stderr());
        assertEquals("abcdef", theme(res).get("Imported.fgColor"), "the imported rule is in the theme");
        assertEquals("1", theme(res).get("Label.fgColor"));

        long built = res.lastModified();
        assertEquals(0, CN1CSSCLI.run(args), stderr());
        assertTrue(stdout().contains("File has not changed since last compile."), stdout());
        assertEquals(built, res.lastModified(), "an up-to-date theme is left alone");

        write(part, "Imported { color: #fedcba; }\n");
        assertTrue(part.setLastModified(built + 5000));
        assertEquals(0, CN1CSSCLI.run(args), stderr());
        assertEquals("fedcba", theme(res).get("Imported.fgColor"),
                "an edit to an imported file is an edit to the theme");
    }

    @Test
    void anEditedImageIsRepaintedIntoARuleWhoseCssDidNotChange(@TempDir Path tmp) throws Exception {
        File common = mavenProject(tmp.toFile());
        File css = write(new File(common, "src/main/css/theme.css"),
                "Banner { width: 50%; height: 10%; background-image: url(img/a.png);"
                + " box-shadow: 0 0 4px black; }\n");
        File image = png(new File(common, "src/main/css/img/a.png"), 8, 8, 0xffff0000);
        File res = new File(common, "target/classes/theme.res");
        String[] args = {"-input", css.getPath(), "-output", res.getPath()};

        assertEquals(0, CN1CSSCLI.run(args), stderr());
        int[] before = open(res).getImage("Banner_1.png").getRGB();
        assertEquals(0xffff0000, before[before.length / 2], "the image fills the box");

        png(image, 8, 8, 0xff0000ff);
        assertTrue(image.setLastModified(System.currentTimeMillis() + 5000));
        assertEquals(0, CN1CSSCLI.run(args), stderr());

        int[] after = open(res).getImage("Banner_1.png").getRGB();
        assertEquals(0xff0000ff, after[after.length / 2], "the rule was painted again from the new image");
    }

    @Test
    void aMissingImportIsStillSomethingTheStylesheetDependsOn(@TempDir Path tmp) throws Exception {
        File css = write(new File(tmp.toFile(), "theme.css"),
                "@import \"first.css\";\n@import \"absent.css\";\n");
        File first = write(new File(tmp.toFile(), "first.css"), "A { color: red; }\n");

        java.util.Set<File> found = CssImports.collectReachable(css);

        assertTrue(found.contains(first.getCanonicalFile()), found.toString());
        assertTrue(found.contains(new File(tmp.toFile(), "absent.css").getCanonicalFile())
                || found.contains(new File(tmp.toFile(), "absent.css")), found.toString());
    }

    @Test
    void bundlesLocalizationAndRecompilesWhenOnlyABundleChanges(@TempDir Path tmp) throws Exception {
        File common = mavenProject(tmp.toFile());
        File css = write(new File(common, "src/main/css/theme.css"), "Label { color: #000001; }\n");
        File l10n = new File(common, "src/main/l10n");
        File bundle = write(new File(l10n, "Messages.properties"), "greeting=Hello\n");
        write(new File(l10n, "Messages_fr.properties"), "greeting=Bonjour\n");
        File res = new File(common, "target/classes/theme.res");
        String[] args = {"-input", css.getPath(), "-output", res.getPath(), "-l", l10n.getPath()};

        assertEquals(0, CN1CSSCLI.run(args), stderr());
        Resources r = open(res);
        assertTrue(Arrays.asList(r.getL10NResourceNames()).contains("Messages"),
                Arrays.toString(r.getL10NResourceNames()));
        assertEquals("Bonjour", r.getL10N("Messages", "fr").get("greeting"));
        assertEquals("Hello", r.getL10N("Messages", "").get("greeting"));

        // The stylesheet has not changed. The theme still has to be rebuilt: the
        // bundle is compiled into the same file.
        long built = res.lastModified();
        write(bundle, "greeting=Hi\n");
        assertTrue(bundle.setLastModified(built + 5000));
        assertEquals(0, CN1CSSCLI.run(args), stderr());
        assertFalse(stdout().contains("File has not changed since last compile."), stdout());
        assertEquals("Hi", open(res).getL10N("Messages", "").get("greeting"));
    }

    @Test
    void aBadCommandLineIsAUsageErrorThatSaysWhatIsMissing(@TempDir Path tmp) throws Exception {
        File css = write(new File(tmp.toFile(), "theme.css"), "Label { color: red; }\n");

        assertEquals(CN1CSSCLI.EXIT_USAGE, CN1CSSCLI.run(new String[0]));
        assertTrue(stderr().contains("-input <file.css>"), stderr());

        err.reset();
        // The form Ant builds used. It is refused by name rather than
        // misread as an option-less compile.
        assertEquals(CN1CSSCLI.EXIT_USAGE, CN1CSSCLI.run(new String[]{css.getPath(), "theme.res"}));
        assertTrue(stderr().contains("positional form"), stderr());

        err.reset();
        assertEquals(CN1CSSCLI.EXIT_USAGE, CN1CSSCLI.run(new String[]{"-input", css.getPath()}));
        assertTrue(stderr().contains("-output <file.res>"), stderr());

        err.reset();
        File missing = new File(tmp.toFile(), "nope.css");
        assertEquals(CN1CSSCLI.EXIT_USAGE,
                CN1CSSCLI.run(new String[]{"-input", missing.getPath(), "-output", "x.res"}));
        assertTrue(stderr().contains("does not exist") && stderr().contains("nope.css"), stderr());
    }

    @Test
    void aStylesheetThatCannotBeCompiledFailsWithTheReason(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File css = write(new File(dir, "theme.css"), "@import \"gone.css\";\nLabel { color: red; }\n");
        File res = new File(dir, "theme.res");

        assertEquals(CN1CSSCLI.EXIT_COMPILE_FAILED,
                CN1CSSCLI.run(new String[]{"-input", css.getPath(), "-output", res.getPath()}));
        String message = stderr();
        assertTrue(message.contains("CSS compile failed: "), message);
        assertTrue(message.contains("names a file that does not exist") && message.contains("gone.css"), message);
        assertFalse(res.exists(), "a failed compile writes no theme");
    }

    @Test
    void aRuleWithNoNativeEquivalentBecomesANinePieceBorder(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File css = write(new File(dir, "theme.css"),
                "Card { background-color: #ffffff; border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.5); }\n");
        File res = new File(dir, "theme.res");

        assertEquals(0, CN1CSSCLI.run(new String[]{"-input", css.getPath(), "-output", res.getPath()}), stderr());

        Resources r = open(res);
        Object border = r.getTheme("Theme").get("Card.border");
        assertTrue(border instanceof com.codename1.ui.plaf.Border, "Card.border is " + border);
        java.util.List<String> images = Arrays.asList(r.getImageResourceNames());
        for (String piece : new String[]{"TopL", "Top", "TopR", "Left", "Center", "Right", "BottomL", "Bottom", "BottomR"}) {
            assertTrue(images.contains("Card" + piece + "_1.png"), "missing the " + piece + " slice in " + images);
        }

        // The same rule under -no-raster is a build failure that names it.
        err.reset();
        File strict = new File(dir, "strict.res");
        assertEquals(CN1CSSCLI.EXIT_COMPILE_FAILED, CN1CSSCLI.run(new String[]{"-input", css.getPath(),
            "-output", strict.getPath(), "-no-raster"}));
        assertTrue(stderr().contains("Card.unselected (image border)"), stderr());
        assertFalse(strict.exists());
    }

    @Test
    void watchModeRecompilesOnAnEditAndStopsWhenTheParentGoes(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        final File css = write(new File(dir, "theme.css"), "Label { color: #000001; }\n");
        final File res = new File(dir, "theme.res");

        ServerSocket parent = new ServerSocket(0);
        try {
            System.setProperty("parent.port", String.valueOf(parent.getLocalPort()));
            final int[] status = {-1};
            Thread watcher = new Thread(new Runnable() {
                @Override
                public void run() {
                    status[0] = CN1CSSCLI.run(new String[]{"-input", css.getPath(), "-output", res.getPath(), "-watch"});
                }
            }, "css-watch-under-test");
            watcher.start();
            Socket child = parent.accept();

            awaitStdout("CSS file successfully compiled.");
            assertEquals("1", theme(res).get("Label.fgColor"));
            assertFalse(stdout().contains(CN1CSSCLI.REFRESH_SIGNAL), "the first compile is not a reload");

            write(css, "Label { color: #000002; }\n");
            assertTrue(css.setLastModified(System.currentTimeMillis() + 5000));
            awaitStdout(CN1CSSCLI.REFRESH_SIGNAL);
            assertEquals("2", theme(res).get("Label.fgColor"), "the reload signal follows the new theme");

            // The parent going away is the only thing that ends watch mode.
            child.close();
            watcher.join(20000);
            assertFalse(watcher.isAlive(), "watch mode ends when the parent's socket closes");
            assertEquals(0, status[0]);
        } finally {
            parent.close();
        }
    }

    /// Forks the compiler as a build does and lists the classes it loaded.
    ///
    /// `java.awt.headless=true` already turns showing a window into an error.
    /// This is stricter: the window, frame and Swing component classes must
    /// not even load, on a stylesheet that exercises every part of the
    /// compiler that touches AWT -- a decoded image, a generated 9-piece
    /// border, an embedded font's metadata. A model interface such as
    /// `javax.swing.tree.TreeModel` is allowed: the resource file's document
    /// model implements it, and an interface draws nothing.
    @Test
    void aForkedCompileLoadsNoWindowOrSwingComponentClass(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        png(new File(dir, "img/a.png"), 16, 16, 0xff112233);
        File css = write(new File(dir, "theme.css"),
                "Pic { background-image: url(img/a.png); cn1-source-dpi: 160; }\n"
                + "Card { background-color: #ffffff; border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.5); }\n");
        File res = new File(dir, "theme.res");

        String javaBin = new File(new File(System.getProperty("java.home"), "bin"), "java").getPath();
        ProcessBuilder pb = new ProcessBuilder(javaBin, "-Djava.awt.headless=true", "-verbose:class",
                "-cp", System.getProperty("java.class.path"), CN1CSSCLI.class.getName(),
                "-input", css.getPath(), "-output", res.getPath());
        pb.redirectErrorStream(true);
        // No display to fall back on, whatever the machine running the test has.
        pb.environment().remove("DISPLAY");
        pb.environment().remove("WAYLAND_DISPLAY");
        Process process = pb.start();
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        InputStream in = process.getInputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            log.write(buffer, 0, read);
        }
        int status = process.waitFor();
        String output = log.toString("UTF-8");
        assertEquals(0, status, output.length() > 4000 ? output.substring(output.length() - 4000) : output);
        assertTrue(res.isFile(), "the forked compile wrote the theme");

        java.util.List<String> offenders = new java.util.ArrayList<String>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(javax\\.swing\\.J[A-Za-z]+|java\\.awt\\.(?:Frame|Window|Dialog)\\b)").matcher(output);
        while (m.find()) {
            if (!offenders.contains(m.group(1))) {
                offenders.add(m.group(1));
            }
        }
        assertTrue(offenders.isEmpty(), "a headless compile loaded window or Swing component classes: " + offenders);
        assertTrue(output.contains("com.codename1.designer.css.raster.CssBoxRasterizer"),
                "the stylesheet did reach the rasterizer, so the list above means something");
    }

    private void awaitStdout(String text) throws Exception {
        long deadline = System.nanoTime() + 60L * 1000 * 1000 * 1000;
        final Object tick = new Object();
        while (!stdout().contains(text)) {
            assertTrue(System.nanoTime() < deadline, "timed out waiting for \"" + text + "\" in:\n" + stdout()
                    + "\nstderr:\n" + stderr());
            synchronized (tick) {
                tick.wait(100);
            }
        }
    }
}
