package com.codenameone.developerguide.animations;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codenameone.developerguide.Demo;
import com.codenameone.developerguide.DemoRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Captures screenshots for all animation demos and persists them using Storage so
 * that external tooling can compare them against the developer guide imagery.
 */
public class AnimationDemosScreenshotTest extends AbstractTest {
    private static final String HOST_TITLE = "Demo Test Host";
    private static final long FORM_TIMEOUT_MS = 10000L;
    private static final String STORAGE_PREFIX = "developer-guide.animations.";

    private static final Map<String, String> SCREENSHOT_NAME_OVERRIDES = createScreenshotNameOverrides();
    private static final Set<String> OVERRIDE_FILE_NAMES = new HashSet<>(SCREENSHOT_NAME_OVERRIDES.values());

    private final Storage storage = Storage.getInstance();

    @Override
    public boolean runTest() throws Exception {
        clearPreviousScreenshots();

        Form host = new Form(HOST_TITLE);
        host.show();
        TestUtils.waitForFormTitle(HOST_TITLE, FORM_TIMEOUT_MS);

        for (Demo demo : DemoRegistry.getDemos()) {
            Form previous = Display.getInstance().getCurrent();
            demo.show(host);
            Form demoForm = waitForFormChange(previous);
            waitForFormReady(demoForm);

            Image screenshot = capture(demoForm);
            saveScreenshot(storageKeyFor(demo.getTitle()), screenshot);

            host.show();
            waitForHost(host);
        }

        return true;
    }

    private void clearPreviousScreenshots() {
        String[] entries = storage.listEntries();
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            if (entry.startsWith(STORAGE_PREFIX) || OVERRIDE_FILE_NAMES.contains(entry)) {
                storage.deleteStorageFile(entry);
            }
        }
    }

    private String storageKeyFor(String title) {
        String override = SCREENSHOT_NAME_OVERRIDES.get(title);
        if (override != null && override.length() > 0) {
            return override;
        }
        return STORAGE_PREFIX + sanitizeFileName(title) + ".png";
    }

    private void saveScreenshot(String storageKey, Image screenshot) throws IOException {
        if (storage.exists(storageKey)) {
            storage.deleteStorageFile(storageKey);
        }
        ImageIO io = ImageIO.getImageIO();
        assertNotNull(io, "PNG image support is required to save screenshots.");
        OutputStream out = null;
        try {
            out = storage.createOutputStream(storageKey);
            io.save(screenshot, out, ImageIO.FORMAT_PNG, 1);
        } finally {
            Util.cleanup(out);
        }
    }

    private Image capture(Form form) {
        final Display display = Display.getInstance();
        final Image[] holder = new Image[1];
        display.screenshot(screen -> holder[0] = screen);

        display.invokeAndBlock(() -> {
            long deadline = System.currentTimeMillis() + 2000L;
            while (holder[0] == null && System.currentTimeMillis() < deadline) {
                Util.sleep(20);
            }
        });

        Image screenshot = holder[0];
        if (screenshot == null) {
            fail("Timed out waiting for native screenshot result.");
            throw new IllegalStateException("Timed out waiting for native screenshot result.");
        }

        if (screenshot.getGraphics() != null && display.shouldPaintNativeScreenshot(screenshot)) {
            form.paintComponent(screenshot.getGraphics(), true);
        }

        return screenshot;
    }

    private Form waitForFormChange(Form previous) {
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        while (Display.getInstance().getCurrent() == previous) {
            TestUtils.waitFor(50);
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for demo form to appear.");
                break;
            }
        }
        return Display.getInstance().getCurrent();
    }

    private void waitForFormReady(Form form) {
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        while ((form.getWidth() <= 0 || form.getHeight() <= 0) && System.currentTimeMillis() <= deadline) {
            TestUtils.waitFor(50);
        }
        assertTrue(form.getWidth() > 0, "Demo form width should be > 0 for screenshot capture.");
        assertTrue(form.getHeight() > 0, "Demo form height should be > 0 for screenshot capture.");
        TestUtils.waitFor(200);
    }

    private void waitForHost(Form host) {
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        while (Display.getInstance().getCurrent() != host) {
            TestUtils.waitFor(50);
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting to return to host form.");
                break;
            }
        }
        TestUtils.waitFor(200);
    }

    private static Map<String, String> createScreenshotNameOverrides() {
        Map<String, String> map = new HashMap<>();
        map.put("Layout Animations", "layout-animation-1.png");
        map.put("Slide Transitions", "transition-slide.png");
        map.put("Bubble Transition", "transition-bubble.png");
        map.put("Morph Transition", "mighty-morphing-components-1.png");
        return map;
    }

    private String sanitizeFileName(String value) {
        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return sanitized.isEmpty() ? "demo-screenshot" : sanitized;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
}
