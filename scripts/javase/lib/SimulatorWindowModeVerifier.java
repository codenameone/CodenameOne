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

            List<String> cmd = new ArrayList<String>();
            String javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            cmd.add(javaExec);
            cmd.add("-Djava.awt.headless=false");
            cmd.add("-Dcn1.simulator.useAppFrame=" + ("single".equals(parsed.mode)));
            cmd.add("-Dcn1.javase.implementation=jmf");
            cmd.add("-Dcn1.test.window.mode=" + parsed.mode);
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
            pb.inheritIO();
            child = pb.start();

            waitForSimulatorWarmup(Duration.ofSeconds(8));

            BufferedImage raw = captureDesktop();
            BufferedImage image = cropToNonBlack(raw);
            validateScreenshotContent(image);

            Path screenshotPath = Path.of(parsed.screenshotPath);
            Files.createDirectories(screenshotPath.getParent());
            if (!ImageIO.write(image, "png", screenshotPath.toFile())) {
                throw new AssertionError("No PNG writer available; screenshot was not written");
            }
            System.out.println("[javase-verifier] screenshot=" + screenshotPath + " mode=" + parsed.mode);
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

    private static BufferedImage cropToNonBlack(BufferedImage src) {
        int minX = src.getWidth();
        int minY = src.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y) & 0x00FFFFFF;
                if (rgb != 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX <= minX || maxY <= minY) {
            return src;
        }
        int pad = 8;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(src.getWidth() - 1, maxX + pad);
        maxY = Math.min(src.getHeight() - 1, maxY + pad);
        return src.getSubimage(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
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

    private static final class Args {
        final String mode;
        final String screenshotPath;
        final String simClasspath;
        final String skinPath;

        private Args(String mode, String screenshotPath, String simClasspath, String skinPath) {
            this.mode = mode;
            this.screenshotPath = screenshotPath;
            this.simClasspath = simClasspath;
            this.skinPath = skinPath;
        }

        static Args parse(String[] args) {
            String mode = null;
            String screenshot = null;
            String simClasspath = null;
            String skinPath = null;
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
            return new Args(mode, screenshot, simClasspath, skinPath);
        }
    }
}
