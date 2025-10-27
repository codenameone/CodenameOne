package com.codenameone.developerguide.animations;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.AnimationManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.util.ImageIO;
import com.codenameone.developerguide.Demo;
import com.codenameone.developerguide.DemoBrowserForm;
import com.codenameone.developerguide.DemoRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures screenshots for all animation demos and persists them using Storage so
 * that external tooling can compare them against the developer guide imagery.
 */
public class AnimationDemosScreenshotTest extends AbstractTest {
    private static final String HOST_TITLE = "Developer Guide Demos";
    private static final long FORM_TIMEOUT_MS = 10000L;
    private static final String STORAGE_PREFIX = "developer-guide.animations.";
    private static final int FRAMES_PER_ANIMATION = 6;
    private static final long ANIMATION_CAPTURE_TIMEOUT_MS = 2000L;
    private static final long ANIMATION_SETTLE_TIMEOUT_MS = 1500L;
    private static final int ANIMATION_FRAME_DELAY_MS = 180;
    private static final String FRAME_MANIFEST_SUFFIX = "-frames.manifest";

    private static final Map<String, String> SCREENSHOT_NAME_OVERRIDES = createScreenshotNameOverrides();
    private static final Set<String> OVERRIDE_FILE_NAMES = new HashSet<>(SCREENSHOT_NAME_OVERRIDES.values());

    private final Storage storage = Storage.getInstance();

    @Override
    public boolean runTest() throws Exception {
        clearPreviousScreenshots();

        boolean previousSlowMotion = Motion.isSlowMotion();
        Motion.setSlowMotion(true);
        try {
            Form host = new DemoBrowserForm();
            host.show();
            TestUtils.waitForFormTitle(HOST_TITLE, FORM_TIMEOUT_MS);

            for (Demo demo : DemoRegistry.getDemos()) {
                Form previous = Display.getInstance().getCurrent();
                demo.show(host);
                Form demoForm = waitForFormChange(previous);
                waitForFormReady(demoForm);

                waitForAnimationsToFinish(demoForm);

                triggerAnimationIfNeeded(demo, demoForm);
                Form activeForm = ensureCurrentFormReady(demoForm);

                if (waitForAnimationStart(activeForm, ANIMATION_CAPTURE_TIMEOUT_MS)) {
                    captureAnimationFrames(demo, activeForm);
                    finalizeAnimations(activeForm);
                } else {
                    Image screenshot = capture(activeForm);
                    saveScreenshot(storageKeyFor(demo.getTitle()), screenshot);
                }

                returnToHost(activeForm, host);
            }

            return true;
        } finally {
            Motion.setSlowMotion(previousSlowMotion);
        }
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
        Image screenshot = Image.createImage(form.getWidth(), form.getHeight());
        form.paintComponent(screenshot.getGraphics(), true);
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
            animateCurrentForm();
            TestUtils.waitFor(50);
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting to return to host form.");
                break;
            }
        }
        TestUtils.waitFor(200);
    }

    private void returnToHost(Form demoForm, Form host) {
        if (host == null) {
            return;
        }

        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;

        if (demoForm != null && demoForm != currentForm()) {
            unwindForm(demoForm, host);
        }

        while (System.currentTimeMillis() <= deadline) {
            Dialog activeDialog = activeDialog();
            if (activeDialog != null) {
                activeDialog.dispose();
                animateCurrentForm();
                TestUtils.waitFor(120);
                continue;
            }

            Form active = currentForm();
            if (active == host) {
                break;
            }

            if (active == null) {
                host.show();
            } else {
                unwindForm(active, host);
            }

            animateCurrentForm();
            TestUtils.waitFor(120);
        }

        if (currentForm() != host) {
            host.show();
        }

        waitForHost(host);
    }

    private void unwindForm(Form form, Form host) {
        if (form == null) {
            return;
        }

        if (form instanceof Dialog) {
            ((Dialog) form).dispose();
            return;
        }

        if (form == host) {
            return;
        }

        form.showBack();
    }

    private Form currentForm() {
        Component current = Display.getInstance().getCurrent();
        return (current instanceof Form) ? (Form) current : null;
    }

    private Dialog activeDialog() {
        Component current = Display.getInstance().getCurrent();
        return (current instanceof Dialog) ? (Dialog) current : null;
    }

    private void animateCurrentForm() {
        Form current = currentForm();
        if (current != null) {
            current.animate();
            return;
        }

        Dialog dialog = activeDialog();
        if (dialog != null) {
            dialog.animate();
        }
    }

    private void triggerAnimationIfNeeded(Demo demo, Form form) {
        if (demo == null || form == null) {
            return;
        }

        if (demo instanceof LayoutAnimationsDemo) {
            clickButton(form, "Fall");
        } else if (demo instanceof UnlayoutAnimationsDemo) {
            clickButton(form, "Fall");
        } else if (demo instanceof HiddenComponentDemo) {
            clickButton(form, "Hide It");
        } else if (demo instanceof AnimationSynchronicityDemo) {
            clickButton(form, "Run Sequence");
        } else if (demo instanceof ReplaceTransitionDemo) {
            clickButton(form, "Replace Pending");
        } else if (demo instanceof SlideTransitionsDemo) {
            clickButton(form, "Show");
        } else if (demo instanceof BubbleTransitionDemo) {
            clickButton(form, "+");
        } else if (demo instanceof SwipeBackSupportDemo) {
            clickButton(form, "Open Destination");
        }
    }

    private void clickButton(Component root, String text) {
        Button button = findButton(root, text);
        if (button != null) {
            button.pressed();
            button.released();
            TestUtils.waitFor(200);
        }
    }

    private Button findButton(Component component, String text) {
        if (component instanceof Button) {
            Button button = (Button) component;
            if (text.equals(button.getText())) {
                return button;
            }
        }

        if (component instanceof Form) {
            return findButton(((Form) component).getContentPane(), text);
        }

        if (component instanceof Container) {
            Container container = (Container) component;
            int childCount = container.getComponentCount();
            for (int i = 0; i < childCount; i++) {
                Button match = findButton(container.getComponentAt(i), text);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    private Form ensureCurrentFormReady(Form fallback) {
        Component current = Display.getInstance().getCurrent();
        if (current instanceof Form) {
            Form form = (Form) current;
            waitForFormReady(form);
            return form;
        }
        return fallback;
    }

    private void waitForAnimationsToFinish(Form form) {
        if (form == null) {
            return;
        }
        long deadline = System.currentTimeMillis() + ANIMATION_SETTLE_TIMEOUT_MS;
        while (System.currentTimeMillis() <= deadline) {
            AnimationManager manager = form.getAnimationManager();
            if (manager == null || !manager.isAnimating()) {
                break;
            }
            form.animate();
            TestUtils.waitFor(50);
        }
    }

    private void captureAnimationFrames(Demo demo, Form form) throws IOException {
        String sanitized = sanitizeFileName(demo.getTitle());
        String baseKey = storageKeyFor(demo.getTitle());
        boolean baseSaved = false;
        List<String> frameKeys = new ArrayList<>(FRAMES_PER_ANIMATION);
        Image finalFrameImage = null;

        for (int frameIndex = 0; frameIndex < FRAMES_PER_ANIMATION; frameIndex++) {
            if (!isAnimating(form) && frameIndex == 0) {
                break;
            }

            Image frameImage = capture(form);
            if (!baseSaved) {
                saveScreenshot(baseKey, frameImage);
                baseSaved = true;
            }

            String frameKey = stageStorageKeyFor(sanitized, frameIndex);
            saveScreenshot(frameKey, frameImage);
            frameKeys.add(frameKey);
            finalFrameImage = frameImage;

            if (frameIndex >= FRAMES_PER_ANIMATION - 1) {
                break;
            }

            if (!advanceAnimation(form)) {
                finalFrameImage = capture(form);
                break;
            }
        }

        if (!baseSaved) {
            Image screenshot = capture(form);
            saveScreenshot(baseKey, screenshot);
            finalFrameImage = screenshot;
        }

        if (finalFrameImage == null) {
            finalFrameImage = capture(form);
        }

        while (frameKeys.size() < FRAMES_PER_ANIMATION) {
            String frameKey = stageStorageKeyFor(sanitized, frameKeys.size());
            saveScreenshot(frameKey, finalFrameImage);
            frameKeys.add(frameKey);
        }

        if (!frameKeys.isEmpty()) {
            recordFrameManifest(sanitized, frameKeys);
        }
    }

    private String stageStorageKeyFor(String sanitizedTitle, int frame) {
        return STORAGE_PREFIX + sanitizedTitle + "-frame-" + (frame + 1) + ".png";
    }

    private void recordFrameManifest(String sanitizedTitle, List<String> frameKeys) {
        if (sanitizedTitle == null || frameKeys == null || frameKeys.isEmpty()) {
            return;
        }

        String manifestKey = STORAGE_PREFIX + sanitizedTitle + FRAME_MANIFEST_SUFFIX;
        storage.deleteStorageFile(manifestKey);

        Map<String, Object> manifest = new HashMap<>();
        manifest.put("frames", new ArrayList<>(frameKeys));

        List<Integer> comparableFrames = new ArrayList<>(2);
        comparableFrames.add(Integer.valueOf(0));
        if (frameKeys.size() > 1) {
            comparableFrames.add(Integer.valueOf(frameKeys.size() - 1));
        }
        manifest.put("compareFrames", comparableFrames);

        storage.writeObject(manifestKey, manifest);
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

    private void finalizeAnimations(Form form) {
        if (form == null) {
            return;
        }
        form.animate();
        waitForAnimationsToFinish(form);
    }

    private boolean waitForAnimationStart(Form form, long timeoutMs) {
        if (form == null) {
            return false;
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() <= deadline) {
            if (isAnimating(form)) {
                return true;
            }
            form.animate();
            TestUtils.waitFor(50);
        }
        return isAnimating(form);
    }

    private boolean isAnimating(Form form) {
        if (form == null) {
            return false;
        }
        AnimationManager manager = form.getAnimationManager();
        return manager != null && manager.isAnimating();
    }

    private boolean advanceAnimation(Form form) {
        if (form == null) {
            return false;
        }
        long deadline = System.currentTimeMillis() + ANIMATION_FRAME_DELAY_MS;
        boolean animating = isAnimating(form);
        while (System.currentTimeMillis() <= deadline && animating) {
            form.animate();
            TestUtils.waitFor(20);
            animating = isAnimating(form);
        }
        return animating;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
}
