package com.codenameone.tools.skindesigner.screenshots;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches the Skin Designer Codename One app inside the JavaSE simulator
 * with the demo-override system properties wired up, waits for the UI to
 * settle, and captures the desktop with {@link Robot}. The CI workflow
 * runs this once per scenario to refresh the developer-guide screenshots.
 *
 * <p>Designed to be invoked from
 * {@code scripts/skindesigner/screenshots/take-screenshots.sh} which
 * provides the simulator classpath and a per-scenario set of system
 * properties (cn1.skindesigner.demoStep / demoDevice / demoSource etc.).
 */
public class SkinDesignerScreenshotter {
    private static final String APP_CLASS =
            "com.codename1.tools.skindesigner.SkinDesigner";

    public static void main(String[] args) {
        int exitCode = 1;
        Process child = null;
        try {
            Args parsed = Args.parse(args);
            // The simulator persists its preferences under
            // ~/.codenameone/<group>/...; override java.util.prefs.userRoot
            // to a sandboxed location so each scenario starts fresh.
            Path prefsRoot = Files.createTempDirectory("skin-designer-prefs-");
            Path projectDir = Files.createTempDirectory("skin-designer-app-");

            List<String> cmd = new ArrayList<String>();
            String javaExec = System.getProperty("java.home")
                    + File.separator + "bin" + File.separator + "java";
            cmd.add(javaExec);
            cmd.add("-Djava.awt.headless=false");
            cmd.add("-Djava.util.prefs.userRoot=" + prefsRoot.toAbsolutePath());
            cmd.add("-Dcn1.simulator.useAppFrame=true");
            cmd.add("-Dcn1.javase.implementation=jmf");
            // Demo-mode overrides forwarded to SkinDesigner's
            // applyDemoOverrides() — drive the wizard to a specific step
            // with a specific device / source / preset / sidebar tab.
            for (String override : parsed.demoOverrides) {
                cmd.add("-D" + override);
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
            pb.inheritIO();
            child = pb.start();

            waitForSimulatorWarmup(Duration.ofSeconds(parsed.warmupSeconds));

            BufferedImage image = captureDesktop();
            Path screenshotPath = Paths.get(parsed.screenshotPath);
            Files.createDirectories(screenshotPath.getParent());
            if (!ImageIO.write(image, "png", screenshotPath.toFile())) {
                throw new AssertionError(
                        "No PNG writer available; screenshot was not written");
            }
            System.out.println("[skin-designer-screenshotter] wrote "
                    + screenshotPath + " for scenario=" + parsed.scenario);
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
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
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
        Rectangle bounds = new Rectangle(0, 0,
                Math.max(1, size.width), Math.max(1, size.height));
        return new Robot().createScreenCapture(bounds);
    }

    /** Minimal argv parser. */
    private static final class Args {
        String scenario = "default";
        String simClasspath = "";
        String skinPath = "";
        String screenshotPath = "";
        int warmupSeconds = 8;
        List<String> demoOverrides = new ArrayList<String>();

        static Args parse(String[] argv) {
            Args a = new Args();
            for (int i = 0; i < argv.length; i++) {
                String key = argv[i];
                if ("--scenario".equals(key)) {
                    a.scenario = argv[++i];
                } else if ("--sim-classpath".equals(key)) {
                    a.simClasspath = argv[++i];
                } else if ("--skin".equals(key)) {
                    a.skinPath = argv[++i];
                } else if ("--screenshot".equals(key)) {
                    a.screenshotPath = argv[++i];
                } else if ("--warmup".equals(key)) {
                    a.warmupSeconds = Integer.parseInt(argv[++i]);
                } else if ("--demo".equals(key)) {
                    // Repeatable: --demo cn1.skindesigner.demoStep=2
                    a.demoOverrides.add(argv[++i]);
                } else {
                    throw new IllegalArgumentException("unknown arg: " + key);
                }
            }
            if (a.simClasspath.isEmpty()) {
                throw new IllegalArgumentException("--sim-classpath is required");
            }
            if (a.screenshotPath.isEmpty()) {
                throw new IllegalArgumentException("--screenshot is required");
            }
            return a;
        }
    }
}
