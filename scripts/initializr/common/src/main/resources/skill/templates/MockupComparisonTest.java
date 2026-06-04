// ===========================================================================
// COPY-PASTE TEMPLATE - not compiled where it ships.
//
// To use it:
//   1. Copy this file into  common/src/test/java/<your-package>/
//   2. Replace the package below with your app's package.
//   3. Replace `YourMainClass` with your Lifecycle main class and adjust the
//      navigation in each capture method to reach the screen you want.
//
// What it does:
//   Renders a screen in the simulator and writes a PNG to
//   `target/mockup-compare/<name>.png`. It does NOT score anything - scoring
//   lives in tools/CompareToMockup.java so there is a single definition of the
//   similarity metric. After running this test, compare the capture to your
//   designer mockup:
//
//     java .claude/skills/codename-one/tools/CompareToMockup.java \
//          target/mockup-compare/home.png \
//          common/src/test/resources/mockups/home.png \
//          --ignore-top 8% --diff target/mockup-compare/home-diff.png
//
// This is a JUnit 5 @CodenameOneTest, so it runs only in the simulator JVM
// (which is exactly where you compare against a mockup during development).
// See references/mockup-comparison.md for the full loop.
// ===========================================================================
package com.example.myapp; // <-- CHANGE to your app package

import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.testing.junit.RunOnEdt;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

@CodenameOneTest
class MockupComparisonTest {

    @Test
    @RunOnEdt
    void captureHome() throws Exception {
        // Build and show the screen you want to compare against the mockup.
        new YourMainClass().runApp(); // <-- CHANGE to your Lifecycle main class
        // Pump the EDT so the form is laid out and painted before capture.
        Display.getInstance().getCurrent().revalidate();

        captureCurrentScreen("home");
    }

    /// Captures the current form to target/mockup-compare/<name>.png.
    /// Mirrors the capture idiom used by TestUtils.screenshotTest
    /// (captureScreen + paintComponent), but writes to a predictable project
    /// path instead of CN1 Storage so the comparison tool can find it.
    private static void captureCurrentScreen(String name) throws Exception {
        ImageIO io = ImageIO.getImageIO();
        if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
            throw new IllegalStateException("PNG ImageIO not available in this environment");
        }

        Image shot = Display.getInstance().captureScreen();
        Display.getInstance().getCurrent().paintComponent(shot.getGraphics(), true);

        File outDir = new File("target/mockup-compare");
        outDir.mkdirs();
        File outFile = new File(outDir, name + ".png");
        try (OutputStream out = new FileOutputStream(outFile)) {
            io.save(shot, out, ImageIO.FORMAT_PNG, 1);
        }
        // Surfaced in the test log so the agent can locate the artifact.
        System.out.println("[MockupComparisonTest] wrote " + outFile.getAbsolutePath()
                + " (" + shot.getWidth() + "x" + shot.getHeight() + ")");
    }
}
