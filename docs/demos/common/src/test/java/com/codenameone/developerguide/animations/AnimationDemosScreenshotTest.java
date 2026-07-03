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
import com.codename1.ui.Graphics;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.TextField;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.FlipTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BoxLayout;
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
    private static final int LAYOUT_FRAME_DELAY_MS = 3300;

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

        return true;
    }

    private void captureLayoutAnimationFrames(Form host) throws IOException {
        GuideScreenshot firstFrame = screenshot("layout-animation-1.png");
        Form demoForm = showDemo(firstFrame, host);
        runOnEdtAsync(() -> buttonByText(demoForm, "Fall").released());
        TestUtils.waitFor(100);

        for (int frame = 1; frame <= LAYOUT_FRAME_COUNT; frame++) {
            saveScreenshot("layout-animation-" + frame + ".png", captureDisplay());
            if (frame < LAYOUT_FRAME_COUNT) {
                TestUtils.waitFor(LAYOUT_FRAME_DELAY_MS);
            }
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
        saveScreenshot(fileName, renderSlideTransitionStrip(transitionName, horizontal));
        returnToHost(host);
    }

    private void captureBubbleTransitionStrip(Form host) throws IOException {
        GuideScreenshot screenshot = screenshot("transition-bubble.png");
        Form demoForm = showDemo(screenshot, host);
        runOnEdtAsync(() -> buttonByText(demoForm, "+").released());
        TestUtils.waitFor(50);
        saveScreenshot(screenshot.getFileName(), captureFrameStrip(TRANSITION_FRAME_COUNT, TRANSITION_FRAME_DELAY_MS));
        returnToHost(host);
    }

    private void captureMorphTransitionStrip(Form host) throws IOException {
        GuideScreenshot screenshot = screenshot("mighty-morphing-components-1.png");
        showDemo(screenshot, host);
        TestUtils.waitFor(50);
        saveScreenshot(screenshot.getFileName(), captureFrameStrip(TRANSITION_FRAME_COUNT, TRANSITION_FRAME_DELAY_MS));
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

    private Image renderSlideTransitionStrip(String transitionName, boolean horizontal) {
        Transition transition = createSlideTransition(transitionName, horizontal);
        TransitionRenderContext context = runOnEdt(() -> {
            Form source = createTransitionSourceForm(transitionName, horizontal);
            Form destination = createTransitionDestinationForm();
            source.show();
            source.setWidth(Display.getInstance().getDisplayWidth());
            source.setHeight(Display.getInstance().getDisplayHeight());
            destination.setWidth(source.getWidth());
            destination.setHeight(source.getHeight());
            source.layoutContainer();
            destination.layoutContainer();
            transition.init(source, destination);
            transition.initTransition();
            return new TransitionRenderContext(transition, source.getWidth(), source.getHeight());
        });

        List<Image> frames = new ArrayList<>();
        for (int frame = 0; frame < TRANSITION_FRAME_COUNT; frame++) {
            frames.add(renderTransitionFrame(context));
            TestUtils.waitFor(TRANSITION_FRAME_DELAY_MS);
            runOnEdt(context.transition::animate);
        }
        runOnEdt(context.transition::cleanup);
        return createFrameStrip(frames);
    }

    private Transition createSlideTransition(String transitionName, boolean horizontal) {
        int direction = horizontal ? CommonTransitions.SLIDE_HORIZONTAL : CommonTransitions.SLIDE_VERTICAL;
        switch (transitionName) {
            case "Slide":
                return CommonTransitions.createSlide(direction, true, TRANSITION_DURATION_MS);
            case "SlideFade":
                return CommonTransitions.createSlideFadeTitle(true, TRANSITION_DURATION_MS);
            case "Cover":
                return CommonTransitions.createCover(direction, true, TRANSITION_DURATION_MS);
            case "Uncover":
                return CommonTransitions.createUncover(direction, true, TRANSITION_DURATION_MS);
            case "Fade":
                return CommonTransitions.createFade(TRANSITION_DURATION_MS);
            case "Flip":
                return new FlipTransition(-1, TRANSITION_DURATION_MS);
            default:
                throw new IllegalArgumentException("Unsupported transition: " + transitionName);
        }
    }

    private Form createTransitionSourceForm(String transitionName, boolean horizontal) {
        Form source = new Form("Transitions", new BoxLayout(BoxLayout.Y_AXIS));
        Style bg = source.getContentPane().getUnselectedStyle();
        bg.setBgTransparency(255);
        bg.setBgColor(0xff0000);
        source.add(new Button("Show"));
        Picker picker = new Picker();
        picker.setStrings("Slide", "SlideFade", "Cover", "Uncover", "Fade", "Flip");
        picker.setSelectedString(transitionName);
        source.add(picker);
        source.add(new TextField(String.valueOf(TRANSITION_DURATION_MS), "Duration", 6, TextField.NUMERIC));
        CheckBox horizontalToggle = CheckBox.createToggle("Horizontal");
        horizontalToggle.setSelected(horizontal);
        source.add(horizontalToggle);
        return source;
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

    private Image renderTransitionFrame(TransitionRenderContext context) {
        return runOnEdt(() -> {
            Image frame = Image.createImage(context.width, context.height, 0xffffffff);
            context.transition.paint(frame.getGraphics());
            return frame;
        });
    }

    private Form showDemo(GuideScreenshot screenshotDemo, Form host) {
        Form previous = currentForm();
        runOnEdt(() -> screenshotDemo.getDemo().show(host));
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
            Form form = Display.getInstance().getCurrent();
            Image fallback = Image.createImage(form.getWidth(), form.getHeight());
            form.paintComponent(fallback.getGraphics(), true);
            return fallback;
        });
    }

    private Image captureFrameStrip(int frameCount, int frameDelayMs) {
        List<Image> frames = new ArrayList<>();
        for (int frame = 0; frame < frameCount; frame++) {
            frames.add(captureDisplay());
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

    private interface UiSupplier<T> {
        T get();
    }

    private interface ComponentMatcher {
        boolean matches(Component component);
    }

    private static final class TransitionRenderContext {
        private final Transition transition;
        private final int width;
        private final int height;

        private TransitionRenderContext(Transition transition, int width, int height) {
            this.transition = transition;
            this.width = width;
            this.height = height;
        }
    }
}
