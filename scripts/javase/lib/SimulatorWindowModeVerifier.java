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
            if (parsed.nativeTheme != null) {
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

            BufferedImage image = captureDesktop();
            validateScreenshotContent(image);

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

    private static void validateScreenshotContent(BufferedImage image) {
        if (image.getWidth() < 120 || image.getHeight() < 120) {
            throw new AssertionError("Screenshot is unexpectedly small: " + image.getWidth() + "x" + image.getHeight());
        }
        Set<Integer> samples = new HashSet<Integer>();
        int stepX = Math.max(1, image.getWidth() / 24);
        int stepY = Math.max(1, image.getHeight() / 24);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                samples.add(image.getRGB(x, y));
            }
        }
        if (samples.size() < 3) {
            throw new AssertionError("Screenshot appears blank/flat (insufficient color variation): " + samples.size());
        }
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
