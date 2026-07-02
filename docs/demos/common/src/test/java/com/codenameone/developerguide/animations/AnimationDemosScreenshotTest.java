package com.codenameone.developerguide.animations;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codenameone.developerguide.DemoRegistry;
import com.codenameone.developerguide.GuideScreenshot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Captures screenshots for all registered guide demos and persists them using Storage so
 * that external tooling can compare them against the developer guide imagery.
 */
public class AnimationDemosScreenshotTest extends AbstractTest {
    private static final String HOST_TITLE = "Demo Test Host";
    private static final long FORM_TIMEOUT_MS = 10000L;
    private static final String STORAGE_PREFIX = "developer-guide.screenshots.";

    private static final Set<String> SCREENSHOT_FILE_NAMES = createScreenshotFileNames();

    private final Storage storage = Storage.getInstance();

    @Override
    public boolean runTest() throws Exception {
        clearPreviousScreenshots();

        Form host = runOnEdt(() -> {
            Form form = new Form(HOST_TITLE);
            form.show();
            return form;
        });
        waitForCurrentForm(host);

        for (GuideScreenshot screenshotDemo : DemoRegistry.getScreenshots()) {
            Form previous = currentForm();
            runOnEdt(() -> screenshotDemo.getDemo().show(host));
            Form demoForm = waitForFormChange(previous);
            waitForFormReady(demoForm);

            Image screenshot = capture(demoForm);
            saveScreenshot(storageKeyFor(screenshotDemo), screenshot);

            runOnEdt(host::show);
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
            if (entry.startsWith(STORAGE_PREFIX) || SCREENSHOT_FILE_NAMES.contains(entry)) {
                storage.deleteStorageFile(entry);
            }
        }
    }

    private String storageKeyFor(GuideScreenshot screenshotDemo) {
        String fileName = screenshotDemo.getFileName();
        if (fileName != null && fileName.length() > 0) {
            return fileName;
        }
        return STORAGE_PREFIX + screenshotDemo.getId() + ".png";
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
        return runOnEdt(() -> {
            Image screenshot = Image.createImage(form.getWidth(), form.getHeight());
            form.paintComponent(screenshot.getGraphics(), true);
            return screenshot;
        });
    }

    private Form waitForFormChange(Form previous) {
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        while (currentForm() == previous) {
            TestUtils.waitFor(50);
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for demo form to appear.");
                break;
            }
        }
        return currentForm();
    }

    private void waitForFormReady(Form form) {
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        while ((!hasSize(form)) && System.currentTimeMillis() <= deadline) {
            TestUtils.waitFor(50);
        }
        assertTrue(form.getWidth() > 0, "Demo form width should be > 0 for screenshot capture.");
        assertTrue(form.getHeight() > 0, "Demo form height should be > 0 for screenshot capture.");
        TestUtils.waitFor(200);
    }

    private void waitForHost(Form host) {
        waitForCurrentForm(host);
        TestUtils.waitFor(200);
    }

    private void waitForCurrentForm(Form expected) {
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        while (currentForm() != expected) {
            TestUtils.waitFor(50);
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for expected form: " + expected.getTitle());
                break;
            }
        }
    }

    private Form currentForm() {
        return runOnEdt(() -> Display.getInstance().getCurrent());
    }

    private boolean hasSize(Form form) {
        Boolean hasSize = runOnEdt(() -> form.getWidth() > 0 && form.getHeight() > 0);
        return hasSize.booleanValue();
    }

    private <T> T runOnEdt(UiSupplier<T> supplier) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                result.set(supplier.get());
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });
        if (failure.get() != null) {
            Throwable thrown = failure.get();
            if (thrown instanceof RuntimeException) {
                throw (RuntimeException) thrown;
            }
            if (thrown instanceof Error) {
                throw (Error) thrown;
            }
            throw new RuntimeException(thrown);
        }
        return result.get();
    }

    private void runOnEdt(Runnable runnable) {
        runOnEdt(() -> {
            runnable.run();
            return null;
        });
    }

    private static Set<String> createScreenshotFileNames() {
        Set<String> names = new HashSet<>();
        for (GuideScreenshot screenshot : DemoRegistry.getScreenshots()) {
            names.add(screenshot.getFileName());
        }
        return names;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return false;
    }

    private interface UiSupplier<T> {
        T get();
    }
}
