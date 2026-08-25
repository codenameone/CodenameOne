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
package com.codenameone.examples.javase.tests;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Launches the Codename One simulator as an external process (same as end users),
 * captures Robot screenshots, and validates output.
 */
public class SimulatorWindowModeVerifier {
    private static final String APP_CLASS = "com.codenameone.examples.javase.tests.SimulatorModeTestApp";

    public static void main(String[] args) {
        int exitCode = 1;
        Process child = null;
        try {
            Args parsed = Args.parse(args);
            Path projectDir = prepareCodenameOneSettings();
            Path prefsRoot = configureSimulatorPreferences(parsed, projectDir);

            // Native-theme scenarios write the result line to this
            // sentinel so the verifier can read it after capturing the
            // screenshot. Path lives in the temp project so different
            // scenario runs don't trample each other.
            Path nativeThemeSentinel = parsed.nativeTheme != null
                    ? projectDir.resolve("native-theme-result.txt")
                    : null;
            boolean arDemo = "ar-demo".equals(parsed.scenario);
            Path arSentinel = arDemo ? projectDir.resolve("ar-result.txt") : null;

            List<String> cmd = new ArrayList<String>();
            String javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            cmd.add(javaExec);
            cmd.add("-Djava.awt.headless=false");
            cmd.add("-Djava.util.prefs.userRoot=" + prefsRoot.toAbsolutePath());
            cmd.add("-Dcn1.simulator.useAppFrame=" + ("single".equals(parsed.mode)));
            cmd.add("-Dcn1.javase.implementation=jmf");
            cmd.add("-Dcn1.test.window.mode=" + parsed.mode);
            if ("landscape".equals(parsed.scenario)) {
                cmd.add("-Dcn1.test.landscape=true");
            }
            if ("component-inspector".equals(parsed.scenario)) {
                cmd.add("-Dcn1.simulator.autoComponentInspector=true");
            }
            if ("network-monitor".equals(parsed.scenario)) {
                cmd.add("-Dcn1.simulator.autoNetworkMonitor=true");
                cmd.add("-Dcn1.test.doNetwork=true");
            }
            if ("test-recorder".equals(parsed.scenario)) {
                cmd.add("-Dcn1.simulator.autoTestRecorder=true");
                cmd.add("-Dcn1.simulator.autoTestRecorderRecord=true");
            }
            if (parsed.nativeTheme != null) {
                cmd.add("-Dcn1.test.expectedNativeTheme=" + parsed.nativeTheme);
                cmd.add("-Dcn1.test.nativeThemeResultFile=" + nativeThemeSentinel.toAbsolutePath());
            }
            if (arDemo) {
                cmd.add("-Dcn1.test.arDemo=true");
                cmd.add("-Dcn1.test.arResultFile=" + arSentinel.toAbsolutePath());
            }
            if (parsed.skinPath != null && parsed.skinPath.length() > 0) {
                cmd.add("-Dskin=" + parsed.skinPath);
                cmd.add("-Ddskin=" + parsed.skinPath);
            }
            cmd.add("-cp");
            cmd.add(parsed.simClasspath);
            cmd.add("com.codename1.impl.javase.Simulator");
            cmd.add(APP_CLASS);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(projectDir.toFile());
            pb.redirectErrorStream(true);
            if (parsed.nativeTheme != null || arDemo) {
                // Capture output to a log so we can also confirm the
                // result line on stdout if the sentinel file isn't
                // written for some reason. Without the redirect the
                // inherited stdout would be swallowed by the JVM and
                // unavailable to the assertion below.
                Path logPath = projectDir.resolve("simulator-output.log");
                pb.redirectOutput(logPath.toFile());
            } else {
                pb.inheritIO();
            }
            child = pb.start();

            waitForSimulatorWarmup(Duration.ofSeconds("network-monitor".equals(parsed.scenario) ? 12 : 8));

            if (arDemo) {
                // The AR scenario reaches its steady state (model anchored on
                // the detected floor plane) at its own pace; the sentinel file
                // is written exactly then, so wait for it instead of guessing
                // with a longer sleep. The post-capture assertion below still
                // reports the real failure if the deadline passes.
                Instant arDeadline = Instant.now().plusSeconds(30);
                while (!Files.exists(arSentinel) && Instant.now().isBefore(arDeadline)) {
                    Thread.sleep(250);
                }
                // One extra second so the EDT repaint that follows the anchor
                // attachment is on screen before the Robot capture.
                Thread.sleep(1000);
            }

            // The warmup above is a floor for the scenario to reach its
            // steady state, but on slow runners the simulator window may not
            // have painted yet, which used to fail the run with a blank/flat
            // capture. Keep polling until the desktop shows actual content
            // (or a generous deadline passes and validation reports the
            // real failure).
            BufferedImage image = captureDesktop();
            Instant renderDeadline = Instant.now().plusSeconds(30);
            while ((isBlankOrFlat(image) || isSingleWindowDeviceMissing(parsed, image)
                        || isSingleWindowDeviceUnpainted(parsed, image)
                        || isComponentInspectorDetailsUnsettled(parsed, image)
                        || isComponentInspectorPropertiesUnpopulated(parsed, image))
                    && Instant.now().isBefore(renderDeadline)) {
                Thread.sleep(500);
                image = captureDesktop();
            }
            validateScreenshotContent(parsed, image);

            Path screenshotPath = Path.of(parsed.screenshotPath);
            Files.createDirectories(screenshotPath.getParent());
            if (!ImageIO.write(image, "png", screenshotPath.toFile())) {
                throw new AssertionError("No PNG writer available; screenshot was not written");
            }
            System.out.println("[javase-verifier] screenshot=" + screenshotPath
                    + " mode=" + parsed.mode + " scenario=" + parsed.scenario
                    + (parsed.nativeTheme != null ? " nativeTheme=" + parsed.nativeTheme : ""));

            if (parsed.nativeTheme != null) {
                assertNativeThemeApplied(parsed, nativeThemeSentinel, projectDir);
            }
            if (arDemo) {
                assertResultLinePass("[ar-test]", arSentinel, projectDir,
                        "AR simulator demo (open session, detect plane, hit test, anchor model)");
            }
            exitCode = 0;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        } finally {
            if (child != null && child.isAlive()) {
                child.destroy();
                try {
                    if (!child.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        child.destroyForcibly();
                    }
                } catch (Exception ignored) {
                    child.destroyForcibly();
                }
            }
            System.exit(exitCode);
        }
    }

    private static void waitForSimulatorWarmup(Duration duration) throws Exception {
        Instant until = Instant.now().plus(duration);
        while (Instant.now().isBefore(until)) {
            Thread.sleep(200);
        }
    }

    private static BufferedImage captureDesktop() throws Exception {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle bounds = new Rectangle(0, 0, Math.max(1, size.width), Math.max(1, size.height));
        return new Robot().createScreenCapture(bounds);
    }

    private static void validateScreenshotContent(Args args, BufferedImage image) {
        if (image.getWidth() < 120 || image.getHeight() < 120) {
            throw new AssertionError("Screenshot is unexpectedly small: " + image.getWidth() + "x" + image.getHeight());
        }
        int samples = sampleColorCount(image);
        if (samples < 3) {
            throw new AssertionError("Screenshot appears blank/flat (insufficient color variation): " + samples);
        }
        int darkPixels = countSingleWindowDevicePixels(args, image);
        int minDarkPixels = minimumSingleWindowDevicePixels(args);
        if (darkPixels >= 0 && darkPixels < minDarkPixels) {
            throw new AssertionError("Single-window simulator device content did not appear before capture; darkPixels="
                    + darkPixels);
        }
        if (isComponentInspectorDetailsUnsettled(args, image)) {
            throw new AssertionError("Component inspector details panel had not settled before capture; textPixels="
                    + countComponentDetailsPixels(image));
        }
        if (isComponentInspectorPropertiesUnpopulated(args, image)) {
            throw new AssertionError("Component inspector properties had not been populated before capture; valuePixels="
                    + countComponentPropertyValuePixels(image));
        }
    }

    private static boolean isBlankOrFlat(BufferedImage image) {
        return sampleColorCount(image) < 3;
    }

    /**
     * Whether the device's screen -- the area inside the skin's bezel -- is still
     * entirely dark, which is a capture taken before the first paint rather than a
     * rendering difference. The javase-single-native-theme-ios-modern scenario is the
     * one that shows it: it is the first capture after the CSS native themes are
     * built, so it is the one that races the first paint, and it has come back with a
     * solid black screen more than once while the scenarios after it passed.
     *
     * This is a WAIT condition and deliberately not an assertion. An earlier attempt
     * asserted on a dark-pixel ratio measured over a region that included the skin's
     * bezel, and the Nexus5X skin used by the Windows tooling run is mostly bezel:
     * the threshold was never valid there and it turned a passing job red on its
     * first run. Used only to wait, the worst a bad measurement can do is spend the
     * 30 second deadline and then capture anyway, which costs time rather than a
     * build.
     *
     * The bezel is measured rather than assumed, for the same reason: the dark body
     * is found first and its interior is what gets tested, so nothing here depends on
     * which skin is loaded or where the window sits.
     */
    private static boolean isSingleWindowDeviceUnpainted(Args args, BufferedImage image) {
        if (!"single".equals(args.mode)) {
            return false;
        }
        Rectangle body = darkBodyBounds(image);
        if (body == null) {
            // No device body found, which isSingleWindowDeviceMissing already covers.
            return false;
        }
        // Inset well inside the bezel. The screen is a large fraction of the body on
        // every skin here, so a fifth in from each edge is inside the screen on all of
        // them without needing to know which one this is.
        int insetX = Math.max(1, body.width / 5);
        int insetY = Math.max(1, body.height / 5);
        int x0 = body.x + insetX;
        int y0 = body.y + insetY;
        int x1 = body.x + body.width - insetX;
        int y1 = body.y + body.height - insetY;
        if (x1 - x0 < 20 || y1 - y0 < 20) {
            return false;
        }
        int dark = 0;
        int total = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xff) < 45 && ((rgb >> 8) & 0xff) < 45 && (rgb & 0xff) < 45) {
                    dark++;
                }
                total++;
            }
        }
        // Nearly all of it. A painted screen in any of these themes leaves plenty of
        // light pixels; an unpainted one measured 99% here against 0% for the stored
        // reference of the same scenario.
        return total > 0 && dark * 100L / total >= 95;
    }

    /**
     * The bounding box of the near-black device body within the region the
     * single-window scenarios place it, or null when there is not enough of one to
     * be a device.
     */
    private static Rectangle darkBodyBounds(BufferedImage image) {
        int xMax = Math.min(image.getWidth(), 560);
        int yMin = Math.min(image.getHeight(), 70);
        int yMax = Math.min(image.getHeight(), 560);
        if (xMax <= 0 || yMax <= yMin) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = yMin; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xff) < 45 && ((rgb >> 8) & 0xff) < 45 && (rgb & 0xff) < 45) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }
        if (maxX < 0 || maxX - minX < 60 || maxY - minY < 60) {
            return null;
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isSingleWindowDeviceMissing(Args args, BufferedImage image) {
        int darkPixels = countSingleWindowDevicePixels(args, image);
        return darkPixels >= 0 && darkPixels < minimumSingleWindowDevicePixels(args);
    }

    private static int countSingleWindowDevicePixels(Args args, BufferedImage image) {
        if (!"single".equals(args.mode)) {
            return -1;
        }
        int xMax = Math.min(image.getWidth(), 560);
        int yMin = Math.min(image.getHeight(), 70);
        int yMax = Math.min(image.getHeight(), 560);
        if (xMax <= 0 || yMax <= yMin) {
            return 0;
        }
        int darkPixels = 0;
        for (int y = yMin; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (r < 45 && g < 45 && b < 45) {
                    darkPixels++;
                }
            }
        }
        return darkPixels;
    }

    /**
     * Whether the docked Component Details panel is still showing a form that is on its way out.
     *
     * <p>The inspector is created and then moved into its own window by showInFrame(), and the
     * docked panel it leaves behind settles empty -- which is what the stored reference holds and
     * what every run captures once it has settled. A capture taken mid-move catches the form
     * laid out with no values in it, and fails the comparison over a state neither the simulator
     * nor the reference is supposed to be in.</p>
     *
     * <p>So the capture waits for the panel to settle rather than screenshotting on a fixed
     * timer. Read from the pixels, like the device check above, because this verifier drives the
     * simulator from another process and has no handle on its Swing tree.</p>
     */
    private static boolean isComponentInspectorDetailsUnsettled(Args args, BufferedImage image) {
        if (!"component-inspector".equals(args.scenario)) {
            return false;
        }
        return countComponentDetailsPixels(image) >= MIN_COMPONENT_DETAILS_PIXELS;
    }

    /** The label text of the form, which the settled panel does not draw at all. */
    private static int countComponentDetailsPixels(BufferedImage image) {
        int xMin = Math.min(image.getWidth(), 20);
        int xMax = Math.min(image.getWidth(), 680);
        int yMin = Math.min(image.getHeight(), 690);
        int yMax = Math.min(image.getHeight(), 800);
        if (xMax <= xMin || yMax <= yMin) {
            return 0;
        }
        int textPixels = 0;
        for (int y = yMin; y < yMax; y++) {
            for (int x = xMin; x < xMax; x++) {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xff) < 100 && ((rgb >> 8) & 0xff) < 100 && (rgb & 0xff) < 100) {
                    textPixels++;
                }
            }
        }
        return textPixels;
    }

    /**
     * Measured on both sides of the race this fixes: the form on its way out draws about 740 dark
     * pixels in that band and the settled panel draws none, so anything above a small margin is
     * the form still being there.
     */
    private static final int MIN_COMPONENT_DETAILS_PIXELS = 200;

    /**
     * Whether the inspector's property VALUES have not been filled in yet.
     *
     * <p>The details panel below settles EMPTY, so the check above waits for it to go away. The
     * properties above it settle the other way round: the inspector selects a component and fills
     * the Class, UUID, Coordinates, Padding and Margin rows in, and the reference holds them
     * populated. A capture taken before the selection propagates shows the same layout with every
     * value blank -- which is not a state the simulator settles in, and comparing it against the
     * reference fails over timing rather than over anything the run did.</p>
     *
     * <p>This is the race that produced four differing screenshots in one run on a slow runner
     * while the two commits either side of it passed. Read from the pixels for the reason the
     * other two checks are: this verifier drives the simulator from another process.</p>
     */
    private static boolean isComponentInspectorPropertiesUnpopulated(Args args, BufferedImage image) {
        if (!"component-inspector".equals(args.scenario)) {
            return false;
        }
        return countComponentPropertyValuePixels(image) < MIN_COMPONENT_PROPERTY_PIXELS;
    }

    /** The text drawn in the property VALUE column, beside the Class..Margin labels. */
    private static int countComponentPropertyValuePixels(BufferedImage image) {
        int xMin = Math.min(image.getWidth(), 127);
        int xMax = Math.min(image.getWidth(), 583);
        int yMin = Math.min(image.getHeight(), 4);
        int yMax = Math.min(image.getHeight(), 248);
        if (xMax <= xMin || yMax <= yMin) {
            return 0;
        }
        int textPixels = 0;
        for (int y = yMin; y < yMax; y++) {
            for (int x = xMin; x < xMax; x++) {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xff) < 100 && ((rgb >> 8) & 0xff) < 100 && (rgb & 0xff) < 100) {
                    textPixels++;
                }
            }
        }
        return textPixels;
    }

    /**
     * Measured on both sides of the race this fixes: the stored reference draws about 2625 dark
     * pixels in that band and the unpopulated capture draws 84, so the threshold sits an order of
     * magnitude clear of the failure and well under the settled state.
     */
    private static final int MIN_COMPONENT_PROPERTY_PIXELS = 800;

    private static int minimumSingleWindowDevicePixels(Args args) {
        if ("test-recorder".equals(args.scenario)) {
            // The recorder window intentionally covers most of the simulator
            // device; its stored baseline has about 1900 dark device pixels.
            // Keep this above the blank/partial failure (~229) without
            // rejecting the expected recorder layout.
            return 1000;
        }
        return 5000;
    }

    private static int sampleColorCount(BufferedImage image) {
        Set<Integer> samples = new HashSet<Integer>();
        int stepX = Math.max(1, image.getWidth() / 24);
        int stepY = Math.max(1, image.getHeight() / 24);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                samples.add(image.getRGB(x, y));
            }
        }
        return samples.size();
    }

    /**
     * Reads the result line written by {@code SimulatorModeTestApp}
     * during simulator startup and verifies it reports a PASS. The
     * sentinel file is preferred since it lands the line atomically;
     * we fall back to the captured stdout log if the sentinel is
     * missing (e.g. the app's init threw before the report ran).
     */
    private static void assertNativeThemeApplied(Args args, Path sentinel, Path projectDir) throws Exception {
        String line = null;
        if (sentinel != null && Files.exists(sentinel)) {
            line = new String(Files.readAllBytes(sentinel), StandardCharsets.UTF_8).trim();
        }
        if (line == null || line.isEmpty()) {
            Path log = projectDir.resolve("simulator-output.log");
            if (Files.exists(log)) {
                for (String l : Files.readAllLines(log, StandardCharsets.UTF_8)) {
                    if (l.startsWith("[native-theme-test]")) {
                        line = l.trim();
                        break;
                    }
                }
            }
        }
        if (line == null || line.isEmpty()) {
            throw new AssertionError("Native theme test produced no result line for "
                    + args.nativeTheme + " (sentinel=" + sentinel + ")");
        }
        System.out.println("[javase-verifier] native-theme assertion: " + line);
        if (!line.contains("result=PASS")) {
            throw new AssertionError("Native theme " + args.nativeTheme
                    + " was not loaded by the simulator: " + line);
        }
    }

    /**
     * Asserts a scenario result line (sentinel file preferred, captured
     * stdout log as fallback) starts with {@code linePrefix} and reports
     * {@code result=PASS}. Mirrors the native-theme assertion for
     * scenarios that verify behavior in addition to the screenshot.
     */
    private static void assertResultLinePass(String linePrefix, Path sentinel, Path projectDir,
            String description) throws Exception {
        String line = null;
        if (sentinel != null && Files.exists(sentinel)) {
            line = new String(Files.readAllBytes(sentinel), StandardCharsets.UTF_8).trim();
        }
        if (line == null || line.isEmpty()) {
            Path log = projectDir.resolve("simulator-output.log");
            if (Files.exists(log)) {
                for (String l : Files.readAllLines(log, StandardCharsets.UTF_8)) {
                    if (l.startsWith(linePrefix)) {
                        line = l.trim();
                        break;
                    }
                }
            }
        }
        if (line == null || line.isEmpty()) {
            throw new AssertionError(description + " produced no result line (sentinel=" + sentinel + ")");
        }
        System.out.println("[javase-verifier] assertion: " + line);
        if (!line.contains("result=PASS")) {
            throw new AssertionError(description + " did not pass: " + line);
        }
    }

    private static Path prepareCodenameOneSettings() throws Exception {
        Path tempProject = Files.createTempDirectory("cn1-javase-sim-project");
        Path settings = tempProject.resolve("codenameone_settings.properties");
        String content = "codename1.displayName=JavaSESimulatorTest\n"
                + "codename1.mainName=SimulatorModeTestApp\n"
                + "codename1.packageName=com.codenameone.examples.javase.tests\n"
                + "codename1.version=1.0\n"
                + "codename1.vendor=CodenameOne\n";
        Files.write(settings, content.getBytes(StandardCharsets.UTF_8));
        return tempProject;
    }

    private static Path configureSimulatorPreferences(Args args, Path projectDir) throws Exception {
        Path prefsRoot = projectDir.resolve("prefs");
        Files.createDirectories(prefsRoot);
        System.setProperty("java.util.prefs.userRoot", prefsRoot.toAbsolutePath().toString());
        Preferences prefs = Preferences.userNodeForPackage(com.codename1.impl.javase.JavaSEPort.class);
        prefs.putBoolean("Portrait", !"landscape".equals(args.scenario));
        if (args.nativeTheme != null) {
            // Mirrors exactly what the "Native Theme" menu writes when
            // the user picks an explicit theme - this is the lever the
            // simulator menu acts on, so testing the lever directly
            // covers the menu's reload path without driving the menu
            // via AWT events.
            prefs.put("simulatorNativeTheme", args.nativeTheme);
        }
        prefs.flush();
        return prefsRoot;
    }

    private static final class Args {
        final String mode;
        final String screenshotPath;
        final String simClasspath;
        final String skinPath;
        final String scenario;
        final String nativeTheme;

        private Args(String mode, String screenshotPath, String simClasspath, String skinPath, String scenario,
                String nativeTheme) {
            this.mode = mode;
            this.screenshotPath = screenshotPath;
            this.simClasspath = simClasspath;
            this.skinPath = skinPath;
            this.scenario = scenario;
            this.nativeTheme = nativeTheme;
        }

        static Args parse(String[] args) {
            String mode = null;
            String screenshot = null;
            String simClasspath = null;
            String skinPath = null;
            String scenario = "default";
            String nativeTheme = null;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--mode".equals(arg) && i + 1 < args.length) {
                    mode = args[++i];
                } else if ("--screenshot".equals(arg) && i + 1 < args.length) {
                    screenshot = args[++i];
                } else if ("--sim-classpath".equals(arg) && i + 1 < args.length) {
                    simClasspath = args[++i];
                } else if ("--skin".equals(arg) && i + 1 < args.length) {
                    skinPath = args[++i];
                } else if ("--scenario".equals(arg) && i + 1 < args.length) {
                    scenario = args[++i];
                } else if ("--native-theme".equals(arg) && i + 1 < args.length) {
                    nativeTheme = args[++i];
                }
            }
            if (!"single".equals(mode) && !"multi".equals(mode)) {
                throw new IllegalArgumentException("--mode must be 'single' or 'multi'");
            }
            if (screenshot == null || screenshot.trim().isEmpty()) {
                throw new IllegalArgumentException("--screenshot path is required");
            }
            if (simClasspath == null || simClasspath.trim().isEmpty()) {
                throw new IllegalArgumentException("--sim-classpath is required");
            }
            return new Args(mode, screenshot, simClasspath, skinPath, scenario, nativeTheme);
        }
    }
}
