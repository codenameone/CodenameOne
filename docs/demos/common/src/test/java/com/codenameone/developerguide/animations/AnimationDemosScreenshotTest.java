package com.codenameone.developerguide.animations;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.TextField;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.ImageIO;
import com.codenameone.developerguide.DemoRegistry;
import com.codenameone.developerguide.GuideScreenshot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final int TRANSITION_FRAME_COUNT = 6;
    private static final int TRANSITION_FRAME_DELAY_MS = 250;
    private static final int TRANSITION_DURATION_MS = 1500;
    private static final int LAYOUT_FRAME_COUNT = 7;
    private static final int LAYOUT_LABEL_COUNT = 10;
    private static final int MAX_STATIC_SCREENSHOT_WIDTH = 720;
    private static final int MAX_STRIP_FRAME_WIDTH = 360;

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

        captureLayoutAnimationFrames(host);
        captureSlideTransitionStrips(host);
        captureBubbleTransitionStrip(host);
        captureMorphTransitionStrip(host);
        captureStaticGuideScreenshots(host);

        return true;
    }

    private void captureStaticGuideScreenshots(Form host) throws IOException {
        for (GuideScreenshot screenshot : DemoRegistry.getScreenshots()) {
            if (isAnimationScreenshot(screenshot.getFileName())) {
                continue;
            }
            showDemoWithoutParent(screenshot);
            TestUtils.waitFor(100);
            saveScreenshot(screenshot.getFileName(),
                    prepareStaticScreenshot(screenshot.getFileName(), captureStaticScreenshot(screenshot.getFileName())));
            returnToHost(host);
        }
    }

    private boolean isAnimationScreenshot(String fileName) {
        return fileName.startsWith("layout-animation-")
                || fileName.startsWith("transition-")
                || "mighty-morphing-components-1.png".equals(fileName);
    }

    private void captureLayoutAnimationFrames(Form host) throws IOException {
        GuideScreenshot firstFrame = screenshot("layout-animation-1.png");
        showDemo(firstFrame, host);
        Image baseFrame = cropLayoutAnimationFrame(captureDisplay());

        for (int frame = 1; frame <= LAYOUT_FRAME_COUNT; frame++) {
            saveScreenshot("layout-animation-" + frame + ".png", renderLayoutAnimationFrame(frame - 1, baseFrame));
        }

        returnToHost(host);
    }

    private void captureSlideTransitionStrips(Form host) throws IOException {
        captureSlideTransitionStrip(host, "transition-slide.png", "Slide", true);
        captureSlideTransitionStrip(host, "transition-slide-vertical.png", "Slide", false);
        captureSlideTransitionStrip(host, "transition-slide-fade.png", "SlideFade", true);
        captureSlideTransitionStrip(host, "transition-cover.png", "Cover", true);
        captureSlideTransitionStrip(host, "transition-uncover.png", "Uncover", true);
        captureSlideTransitionStrip(host, "transition-fade.png", "Fade", true);
        captureSlideTransitionStrip(host, "transition-flip.png", "Flip", true);
    }

    private void captureSlideTransitionStrip(Form host, String fileName, String transitionName, boolean horizontal)
            throws IOException {
        GuideScreenshot screenshot = screenshot(fileName);
        Form demoForm = showDemo(screenshot, host);
        configureSlideDemo(demoForm, transitionName, horizontal);
        TestUtils.waitFor(100);
        Image sourceFrame = cropTransitionFrame(fileName, captureDisplay());
        Image destinationFrame = cropTransitionFrame(fileName, captureTransitionDestinationFrame());
        saveScreenshot(fileName,
                renderDeterministicTransitionStrip(transitionName, horizontal, sourceFrame, destinationFrame));
        returnToHost(host);
    }

    private void captureBubbleTransitionStrip(Form host) throws IOException {
        GuideScreenshot screenshot = screenshot("transition-bubble.png");
        Form demoForm = showDemo(screenshot, host);
        runOnEdtAsync(() -> buttonByText(demoForm, "+").released());
        TestUtils.waitFor(50);
        saveScreenshot(screenshot.getFileName(), captureFrameStrip(TRANSITION_FRAME_COUNT, TRANSITION_FRAME_DELAY_MS,
                this::cropBubbleFrame));
        returnToHost(host);
    }

    private void captureMorphTransitionStrip(Form host) throws IOException {
        GuideScreenshot screenshot = screenshot("mighty-morphing-components-1.png");
        showDemo(screenshot, host);
        TestUtils.waitFor(50);
        saveScreenshot(screenshot.getFileName(), captureFrameStrip(TRANSITION_FRAME_COUNT, TRANSITION_FRAME_DELAY_MS,
                this::cropMorphFrame));
        returnToHost(host);
    }

    private void configureSlideDemo(Form demoForm, String transitionName, boolean horizontal) {
        runOnEdt(() -> {
            Picker picker = componentByType(demoForm, Picker.class);
            TextField duration = componentByType(demoForm, TextField.class);
            CheckBox horizontalToggle = componentByType(demoForm, CheckBox.class);
            picker.setSelectedString(transitionName);
            duration.setText(String.valueOf(TRANSITION_DURATION_MS));
            horizontalToggle.setSelected(horizontal);
        });
    }

    private Image renderLayoutAnimationFrame(int frameIndex, Image baseFrame) {
        Image frame = Image.createImage(baseFrame.getWidth(), baseFrame.getHeight(), 0xffffffff);
        Graphics graphics = frame.getGraphics();
        graphics.drawImage(baseFrame, 0, 0);
        graphics.setColor(0x202020);

        double progress = frameIndex / (double) (LAYOUT_FRAME_COUNT - 1);
        int lineHeight = Math.max(42, baseFrame.getHeight() / 36);
        int startY = -lineHeight;
        int firstEndY = Math.max(lineHeight * 3, baseFrame.getHeight() / 7);
        int x = Math.max(20, baseFrame.getWidth() / 28);
        for (int iter = 0; iter < LAYOUT_LABEL_COUNT; iter++) {
            int endY = firstEndY + (iter * lineHeight);
            int y = startY + (int) Math.round((endY - startY) * progress);
            graphics.drawString("Label " + iter, x, y);
        }
        return frame;
    }

    private Image renderDeterministicTransitionStrip(
            String transitionName,
            boolean horizontal,
            Image source,
            Image destination) {
        List<Image> frames = new ArrayList<>();
        for (int frame = 0; frame < TRANSITION_FRAME_COUNT; frame++) {
            double progress = frame / (double) (TRANSITION_FRAME_COUNT - 1);
            frames.add(renderTransitionFrame(transitionName, horizontal, source, destination, progress));
        }
        return createFrameStrip(frames);
    }

    private Form createTransitionDestinationForm() {
        Form destination = new Form("Destination");
        Style bg = destination.getContentPane().getUnselectedStyle();
        bg.setBgTransparency(255);
        bg.setBgColor(0xff);
        destination.getToolbar().addCommandToLeftBar("Back", null, ignored -> {
        });
        return destination;
    }

    private Image captureTransitionDestinationFrame() {
        Form previous = currentForm();
        runOnEdt(() -> createTransitionDestinationForm().show());
        Form destination = waitForFormChange(previous);
        waitForFormReady(destination);
        TestUtils.waitFor(300);
        return captureDisplay();
    }

    private Image renderTransitionFrame(
            String transitionName,
            boolean horizontal,
            Image source,
            Image destination,
            double progress) {
        int width = source.getWidth();
        int height = source.getHeight();
        Image frame = Image.createImage(width, height, 0xffffffff);
        Graphics graphics = frame.getGraphics();
        int travel = horizontal ? width : height;
        int offset = (int) Math.round(progress * travel);

        switch (transitionName) {
            case "Slide":
                drawOffset(graphics, source, horizontal, -offset);
                drawOffset(graphics, destination, horizontal, travel - offset);
                break;
            case "SlideFade":
                drawOffset(graphics, source, horizontal, -offset);
                drawImageWithAlpha(graphics, destination, horizontal ? travel - offset : 0,
                        horizontal ? 0 : travel - offset, (int) Math.round(progress * 255));
                break;
            case "Cover":
                graphics.drawImage(source, 0, 0);
                drawOffset(graphics, destination, horizontal, travel - offset);
                break;
            case "Uncover":
                graphics.drawImage(destination, 0, 0);
                drawOffset(graphics, source, horizontal, -offset);
                break;
            case "Fade":
                drawImageWithAlpha(graphics, source, 0, 0, (int) Math.round((1.0 - progress) * 255));
                drawImageWithAlpha(graphics, destination, 0, 0, (int) Math.round(progress * 255));
                break;
            case "Flip":
                renderFlipFrame(graphics, source, destination, progress);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transition: " + transitionName);
        }
        return frame;
    }

    private Image prepareStaticScreenshot(String fileName, Image screenshot) {
        Image prepared = screenshot;
        if ("badge-floating-button.png".equals(fileName)
                || "components-slider.png".equals(fileName)
                || "components-floatinghint.png".equals(fileName)
                || "graphics-glasspane.png".equals(fileName)
                || "graphics-fontimage-fixed.png".equals(fileName)
                || "graphics-fontimage-style.png".equals(fileName)
                || "graphics-fontimage-material.png".equals(fileName)) {
            prepared = cropByRatio(screenshot, 0, 0, 1, 0.42);
        } else if ("floating-action.png".equals(fileName)) {
            prepared = cropByRatio(screenshot, 0, 0, 1, 0.55);
        } else if ("graphics-hiworld.png".equals(fileName)) {
            prepared = cropByRatio(screenshot, 0, 0, 1, 0.5);
        } else if ("shaped-clipping.png".equals(fileName)) {
            prepared = cropByRatio(screenshot, 0.18, 0.17, 0.64, 0.54);
        } else if ("components-dialog-modal-south.png".equals(fileName)
                || "components-dialog-modal-bottom-half.png".equals(fileName)) {
            prepared = cropByRatio(screenshot, 0, 0.35, 1, 0.65);
        } else if ("components-interaction-dialog.png".equals(fileName)) {
            prepared = cropByRatio(screenshot, 0, 0, 1, 0.6);
        }
        return scaleToMaxWidth(prepared, MAX_STATIC_SCREENSHOT_WIDTH);
    }

    private Image cropLayoutAnimationFrame(Image frame) {
        return scaleToMaxWidth(cropByRatio(frame, 0, 0, 1, 0.5), MAX_STATIC_SCREENSHOT_WIDTH);
    }

    private Image cropTransitionFrame(String fileName, Image frame) {
        if ("transition-bubble.png".equals(fileName)) {
            return cropBubbleFrame(frame);
        }
        return scaleToMaxWidth(cropByRatio(frame, 0, 0, 1, 0.58), MAX_STRIP_FRAME_WIDTH);
    }

    private Image cropBubbleFrame(Image frame) {
        return scaleToMaxWidth(cropByRatio(frame, 0, 0, 1, 0.58), MAX_STRIP_FRAME_WIDTH);
    }

    private Image cropMorphFrame(Image frame) {
        return scaleToMaxWidth(cropByRatio(frame, 0, 0, 1, 0.48), MAX_STRIP_FRAME_WIDTH);
    }

    private Image cropByRatio(Image image, double xRatio, double yRatio, double widthRatio, double heightRatio) {
        int x = Math.max(0, Math.min(image.getWidth() - 1, (int) Math.round(image.getWidth() * xRatio)));
        int y = Math.max(0, Math.min(image.getHeight() - 1, (int) Math.round(image.getHeight() * yRatio)));
        int width = Math.max(1, (int) Math.round(image.getWidth() * widthRatio));
        int height = Math.max(1, (int) Math.round(image.getHeight() * heightRatio));
        width = Math.min(width, image.getWidth() - x);
        height = Math.min(height, image.getHeight() - y);
        return image.subImage(x, y, width, height, true);
    }

    private Image scaleToMaxWidth(Image image, int maxWidth) {
        if (image.getWidth() <= maxWidth) {
            return image;
        }
        int height = Math.max(1, (int) Math.round(image.getHeight() * (maxWidth / (double) image.getWidth())));
        return image.scaled(maxWidth, height);
    }

    private void renderFlipFrame(Graphics graphics, Image source, Image destination, double progress) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean firstHalf = progress < 0.5;
        double localProgress = firstHalf ? progress * 2.0 : (progress - 0.5) * 2.0;
        int visibleWidth = Math.max(1, (int) Math.round(width * (firstHalf ? 1.0 - localProgress : localProgress)));
        int x = (width - visibleWidth) / 2;
        graphics.drawImage(firstHalf ? source : destination, x, 0, visibleWidth, height);
    }

    private void drawOffset(Graphics graphics, Image image, boolean horizontal, int offset) {
        graphics.drawImage(image, horizontal ? offset : 0, horizontal ? 0 : offset);
    }

    private void drawImageWithAlpha(Graphics graphics, Image image, int x, int y, int alpha) {
        int previousAlpha = graphics.getAlpha();
        graphics.setAlpha(Math.max(0, Math.min(255, alpha)));
        graphics.drawImage(image, x, y);
        graphics.setAlpha(previousAlpha);
    }

    private Form showDemo(GuideScreenshot screenshotDemo, Form host) {
        Form previous = currentForm();
        runOnEdt(() -> screenshotDemo.getDemo().show(host));
        Form demoForm = waitForFormChange(previous);
        waitForFormReady(demoForm);
        return demoForm;
    }

    private Form showDemoWithoutParent(GuideScreenshot screenshotDemo) {
        Form previous = currentForm();
        runOnEdt(() -> screenshotDemo.getDemo().show(null));
        Form demoForm = waitForFormChange(previous);
        waitForFormReady(demoForm);
        return demoForm;
    }

    private void returnToHost(Form host) {
        runOnEdt(host::show);
        waitForHost(host);
    }

    private GuideScreenshot screenshot(String fileName) {
        for (GuideScreenshot screenshot : DemoRegistry.getScreenshots()) {
            if (fileName.equals(screenshot.getFileName())) {
                return screenshot;
            }
        }
        throw new IllegalStateException("No guide screenshot registered for: " + fileName);
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
            io.save(screenshot, out, imageFormat(storageKey), 1);
        } finally {
            Util.cleanup(out);
        }
    }

    private String imageFormat(String storageKey) {
        if (storageKey.endsWith(".jpg") || storageKey.endsWith(".jpeg")) {
            return ImageIO.FORMAT_JPEG;
        }
        return ImageIO.FORMAT_PNG;
    }

    private Image captureDisplay() {
        return runOnEdt(() -> {
            Image screenshot = Display.getInstance().captureScreen();
            if (screenshot != null) {
                return screenshot;
            }
            return captureCurrentFormPaint();
        });
    }

    private Image captureStaticScreenshot(String fileName) {
        if ("graphics-glasspane.png".equals(fileName)) {
            return runOnEdt(this::captureCurrentFormPaint);
        }
        return captureDisplay();
    }

    private Image captureCurrentFormPaint() {
        Form form = Display.getInstance().getCurrent();
        Image fallback = Image.createImage(form.getWidth(), form.getHeight());
        form.paintComponent(fallback.getGraphics(), true);
        return fallback;
    }

    private Image captureFrameStrip(int frameCount, int frameDelayMs, ImageProcessor frameProcessor) {
        List<Image> frames = new ArrayList<>();
        for (int frame = 0; frame < frameCount; frame++) {
            frames.add(frameProcessor.process(captureDisplay()));
            if (frame < frameCount - 1) {
                TestUtils.waitFor(frameDelayMs);
            }
        }
        return createFrameStrip(frames);
    }

    private Image createFrameStrip(List<Image> frames) {
        int width = 0;
        int height = 0;
        for (Image frame : frames) {
            width += frame.getWidth();
            height = Math.max(height, frame.getHeight());
        }
        Image strip = Image.createImage(width, height, 0xffffffff);
        Graphics graphics = strip.getGraphics();
        int x = 0;
        for (Image frame : frames) {
            graphics.drawImage(frame, x, 0);
            x += frame.getWidth();
        }
        return strip;
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
        TestUtils.waitFor(100);
    }

    private void waitForHost(Form host) {
        waitForCurrentForm(host);
        TestUtils.waitFor(50);
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

    private Button buttonByText(Form form, String text) {
        Button button = findComponent(form, Button.class, component -> text.equals(((Button) component).getText()));
        assertNotNull(button, "Expected button not found: " + text);
        return button;
    }

    private <T extends Component> T componentByType(Form form, Class<T> type) {
        T component = findComponent(form, type, ignored -> true);
        assertNotNull(component, "Expected component not found: " + type.getName());
        return component;
    }

    private <T extends Component> T findComponent(Component root, Class<T> type, ComponentMatcher matcher) {
        if (type.isInstance(root) && matcher.matches(root)) {
            return type.cast(root);
        }
        if (root instanceof Container) {
            Container container = (Container) root;
            for (int i = 0; i < container.getComponentCount(); i++) {
                T found = findComponent(container.getComponentAt(i), type, matcher);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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

    private void runOnEdtAsync(Runnable runnable) {
        Display.getInstance().callSerially(runnable);
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

    @Override
    public int getTimeoutMillis() {
        return 600000;
    }

    private interface UiSupplier<T> {
        T get();
    }

    private interface ComponentMatcher {
        boolean matches(Component component);
    }

    private interface ImageProcessor {
        Image process(Image image);
    }

}
