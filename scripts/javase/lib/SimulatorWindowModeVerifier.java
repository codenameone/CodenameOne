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
            while ((isBlankOrFlat(image) || isSingleWindowDeviceMissing(parsed, image))
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
    }

    private static boolean isBlankOrFlat(BufferedImage image) {
        return sampleColorCount(image) < 3;
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
