package com.codenameone.examples.javase.tests;

import com.codename1.impl.javase.Executor;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs the JavaSE simulator in-process, validates window mode, and captures a Robot screenshot.
 */
public class SimulatorWindowModeVerifier {
    private static final String APP_CLASS = "com.codenameone.examples.javase.tests.SimulatorModeTestApp";

    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Graphics environment is headless. Run under xvfb-run.");
        }

        final boolean expectSingleWindow = "single".equals(parsed.mode);
        System.setProperty("cn1.simulator.useAppFrame", String.valueOf(expectSingleWindow));
        System.setProperty("cn1.test.window.mode", parsed.mode);
        System.setProperty("cn1.javase.noExit", "true");

        System.setProperty("cn1.javase.implementation", "jmf");
        prepareCodenameOneSettings();

        Thread simulatorThread = new Thread(() -> {
            try {
                Executor.main(new String[]{APP_CLASS});
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }, "cn1-simulator-launcher");
        simulatorThread.setDaemon(true);
        simulatorThread.start();

        waitForVisibleFrames(parsed.timeoutMs);
        sleep(1800);

        boolean useAppFrameDetected = isUseAppFrameEnabled();
        if (expectSingleWindow && !useAppFrameDetected) {
            closeAllWindows();
            throw new AssertionError("Expected single-window mode (useAppFrame=true) but it was false.");
        }
        if (!expectSingleWindow && useAppFrameDetected) {
            closeAllWindows();
            throw new AssertionError("Expected multi-window mode (useAppFrame=false) but it was true.");
        }

        BufferedImage image = captureDesktopScreenshot();
        validateScreenshotContent(image);

        Path screenshotPath = Path.of(parsed.screenshotPath);
        Files.createDirectories(screenshotPath.getParent());
        ImageIO.write(image, "png", screenshotPath.toFile());
        System.out.println("[javase-verifier] screenshot=" + screenshotPath + " mode=" + parsed.mode + " useAppFrame=" + useAppFrameDetected);

        closeAllWindows();
        sleep(500);
        System.exit(0);
    }

    private static void waitForVisibleFrames(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Frame[] frames = Frame.getFrames();
            for (Frame frame : frames) {
                if (frame != null && frame.isShowing()) {
                    return;
                }
            }
            sleep(120);
        }
        throw new AssertionError("Timed out waiting for simulator windows.");
    }

    private static boolean isUseAppFrameEnabled() {
        try {
            Class<?> portClass = Class.forName("com.codename1.impl.javase.JavaSEPort");
            java.lang.reflect.Field field = portClass.getDeclaredField("useAppFrame");
            field.setAccessible(true);
            return field.getBoolean(null);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect JavaSEPort.useAppFrame", ex);
        }
    }

    private static BufferedImage captureDesktopScreenshot() throws Exception {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle bounds = new Rectangle(0, 0, Math.max(1, size.width), Math.max(1, size.height));
        Robot robot = new Robot();
        return robot.createScreenCapture(bounds);
    }

    private static void validateScreenshotContent(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 100 || height < 100) {
            throw new AssertionError("Screenshot is unexpectedly small: " + width + "x" + height);
        }
        Set<Integer> samples = new HashSet<>();
        int stepX = Math.max(1, width / 24);
        int stepY = Math.max(1, height / 24);
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                samples.add(image.getRGB(x, y));
            }
        }
        if (samples.size() < 12) {
            throw new AssertionError("Screenshot appears blank/flat (insufficient color variation): " + samples.size());
        }
    }

    private static void closeAllWindows() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (window == null) {
                        continue;
                    }
                    if (window.isDisplayable()) {
                        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                        window.dispose();
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static void prepareCodenameOneSettings() throws Exception {
        Path tempProject = Files.createTempDirectory("cn1-javase-sim-project");
        Path settings = tempProject.resolve("codenameone_settings.properties");
        String content = "codename1.displayName=JavaSESimulatorTest\n"
                + "codename1.mainName=SimulatorModeTestApp\n"
                + "codename1.packageName=com.codenameone.examples.javase.tests\n"
                + "codename1.version=1.0\n"
                + "codename1.vendor=CodenameOne\n";
        Files.write(settings, content.getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.dir", tempProject.toAbsolutePath().toString());
    }

    private static final class Args {
        final String mode;
        final String screenshotPath;
        final long timeoutMs;

        private Args(String mode, String screenshotPath, long timeoutMs) {
            this.mode = mode;
            this.screenshotPath = screenshotPath;
            this.timeoutMs = timeoutMs;
        }

        static Args parse(String[] args) {
            String mode = null;
            String screenshot = null;
            long timeout = 60000L;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--mode".equals(arg) && i + 1 < args.length) {
                    mode = args[++i];
                } else if ("--screenshot".equals(arg) && i + 1 < args.length) {
                    screenshot = args[++i];
                } else if ("--timeout-ms".equals(arg) && i + 1 < args.length) {
                    timeout = Long.parseLong(args[++i]);
                }
            }
            if (!"single".equals(mode) && !"multi".equals(mode)) {
                throw new IllegalArgumentException("--mode must be 'single' or 'multi'");
            }
            if (screenshot == null || screenshot.trim().isEmpty()) {
                throw new IllegalArgumentException("--screenshot path is required");
            }
            return new Args(mode, screenshot, timeout);
        }
    }
}
