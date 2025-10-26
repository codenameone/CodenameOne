package com.codenameone.developerguide.animations;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.AnimationManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.util.ImageIO;
import com.codenameone.developerguide.Demo;
import com.codenameone.developerguide.DemoRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
    private static final int FRAMES_PER_ANIMATION = 6;

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

            triggerAnimationIfNeeded(demo, demoForm);
            Form activeForm = ensureCurrentFormReady(demoForm);
            AnimationContext context = waitForAnimationContext(activeForm);

            if (context != null && context.hasMotions()) {
                captureAnimationFrames(demo, activeForm, context);
            } else {
                Image screenshot = capture(activeForm);
                saveScreenshot(storageKeyFor(demo.getTitle()), screenshot);
            }

            flushAnimations(activeForm);

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
            TestUtils.waitFor(50);
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting to return to host form.");
                break;
            }
        }
        TestUtils.waitFor(200);
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

    private AnimationContext waitForAnimationContext(Form form) {
        if (form == null) {
            return AnimationContext.empty();
        }
        long deadline = System.currentTimeMillis() + FORM_TIMEOUT_MS;
        AnimationContext context = collectAnimationContext(form);
        while (!context.hasMotions() && System.currentTimeMillis() <= deadline) {
            TestUtils.waitFor(100);
            context = collectAnimationContext(form);
        }
        return context;
    }

    private AnimationContext collectAnimationContext(Form form) {
        if (form == null) {
            return AnimationContext.empty();
        }
        try {
            AnimationManager manager = form.getAnimationManager();
            if (manager == null) {
                return AnimationContext.empty();
            }
            List<ComponentAnimation> animations = getComponentAnimations(manager);
            if (animations.isEmpty()) {
                return new AnimationContext(animations, Collections.emptyList());
            }
            Set<Motion> motions = new LinkedHashSet<>();
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ComponentAnimation animation : animations) {
                collectMotions(animation, motions, visited, 0);
            }
            return new AnimationContext(animations, new ArrayList<>(motions));
        } catch (Exception err) {
            return AnimationContext.empty();
        }
    }

    private List<ComponentAnimation> getComponentAnimations(AnimationManager manager) throws NoSuchFieldException, IllegalAccessException {
        Field field = AnimationManager.class.getDeclaredField("anims");
        field.setAccessible(true);
        Object value = field.get(manager);
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<ComponentAnimation> animations = new ArrayList<>((List<ComponentAnimation>) value);
            animations.removeIf(item -> item == null);
            return animations;
        }
        return Collections.emptyList();
    }

    private void collectMotions(Object candidate, Set<Motion> motions, Set<Object> visited, int depth) throws IllegalAccessException {
        if (candidate == null || visited.contains(candidate)) {
            return;
        }
        visited.add(candidate);

        if (candidate instanceof Motion) {
            motions.add((Motion) candidate);
            return;
        }

        if (depth > 6) {
            return;
        }

        Class<?> type = candidate.getClass();
        if (type.isArray()) {
            int length = Array.getLength(candidate);
            for (int i = 0; i < length; i++) {
                collectMotions(Array.get(candidate, i), motions, visited, depth + 1);
            }
            return;
        }

        if (candidate instanceof Iterable) {
            for (Object element : (Iterable<?>) candidate) {
                collectMotions(element, motions, visited, depth + 1);
            }
            return;
        }

        if (!type.getName().startsWith("com.codename1")) {
            return;
        }

        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                collectMotions(field.get(candidate), motions, visited, depth + 1);
            }
            type = type.getSuperclass();
        }
    }

    private void captureAnimationFrames(Demo demo, Form form, AnimationContext context) throws IOException {
        String sanitized = sanitizeFileName(demo.getTitle());
        String baseKey = storageKeyFor(demo.getTitle());
        boolean baseSaved = false;

        for (int frame = 0; frame < FRAMES_PER_ANIMATION; frame++) {
            double progress = FRAMES_PER_ANIMATION == 1 ? 1.0 : (double) frame / (FRAMES_PER_ANIMATION - 1);
            advanceMotions(context.motions, progress);
            refreshAnimations(context.componentAnimations);
            Image frameImage = capture(form);
            if (!baseSaved) {
                saveScreenshot(baseKey, frameImage);
                baseSaved = true;
            }
            saveScreenshot(stageStorageKeyFor(sanitized, frame), frameImage);
        }
    }

    private void advanceMotions(List<Motion> motions, double progress) {
        for (Motion motion : motions) {
            int duration = Math.max(motion.getDuration(), 0);
            long targetTime = progress >= 1.0 ? duration : Math.round(duration * progress);
            motion.setCurrentMotionTime(targetTime);
        }
    }

    private void refreshAnimations(List<ComponentAnimation> animations) {
        for (ComponentAnimation animation : animations) {
            animation.updateAnimationState();
        }
    }

    private String stageStorageKeyFor(String sanitizedTitle, int frame) {
        return STORAGE_PREFIX + sanitizedTitle + "-frame-" + (frame + 1) + ".png";
    }

    private void flushAnimations(Form form) {
        if (form == null) {
            return;
        }
        AnimationManager manager = form.getAnimationManager();
        if (manager != null) {
            manager.flush();
        }
    }

    private static final class AnimationContext {
        private final List<ComponentAnimation> componentAnimations;
        private final List<Motion> motions;

        private AnimationContext(List<ComponentAnimation> componentAnimations, List<Motion> motions) {
            this.componentAnimations = componentAnimations;
            this.motions = motions;
        }

        static AnimationContext empty() {
            return new AnimationContext(Collections.emptyList(), Collections.emptyList());
        }

        boolean hasMotions() {
            return motions != null && !motions.isEmpty();
        }
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
