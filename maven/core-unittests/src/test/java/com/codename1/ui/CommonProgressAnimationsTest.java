package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.CommonProgressAnimations.CircleProgress;
import com.codename1.ui.CommonProgressAnimations.EmptyAnimation;
import com.codename1.ui.CommonProgressAnimations.LoadingTextAnimation;
import com.codename1.ui.CommonProgressAnimations.ProgressAnimation;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CommonProgressAnimationsTest extends UITestBase {

    @Test
    void testMarkComponentLoadingReplacesComponentAndStoresProgress() throws Exception {
        Container parent = new Container();
        Label content = new Label("Data");
        parent.add(content);

        ProgressAnimation progress = ProgressAnimation.markComponentLoading(content, CircleProgress.class);
        assertNotNull(progress);
        assertSame(progress, ProgressAnimation.getProgressAnimation(content));
        assertEquals(1, parent.getComponentCount());
        assertSame(progress, parent.getComponentAt(0));

        Field cmpField = ProgressAnimation.class.getDeclaredField("cmp");
        cmpField.setAccessible(true);
        assertSame(content, cmpField.get(progress));

        ProgressAnimation again = ProgressAnimation.markComponentLoading(content, CircleProgress.class);
        assertSame(progress, again);

        ProgressAnimation.markComponentReady(content);
    }

    @Test
    void testMarkComponentReadyRestoresComponentAndClearsState() {
        Container parent = new Container();
        Label content = new Label("Loaded");
        parent.add(content);

        ProgressAnimation progress = ProgressAnimation.markComponentLoading(content, EmptyAnimation.class);
        assertSame(progress, parent.getComponentAt(0));

        ProgressAnimation.markComponentReady(content);
        assertSame(content, parent.getComponentAt(0));
        assertNull(ProgressAnimation.getProgressAnimation(content));
    }

    @Test
    void testMarkComponentReadyHandlesComponentsWithoutProgress() {
        Label orphan = new Label("Standalone");
        ProgressAnimation.markComponentReady(orphan);
        assertNull(ProgressAnimation.getProgressAnimation(orphan));
    }

    @Test
    void testMarkComponentLoadingWithoutParentThrowsException() {
        Label orphan = new Label("Missing Parent");
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> ProgressAnimation.markComponentLoading(orphan, CircleProgress.class));
        assertNotNull(failure.getMessage());
        assertTrue(failure.getMessage().contains("Component has no parent"));
    }

    @Test
    void testMarkComponentReadyWithTransitionRestoresComponent() {
        Container parent = new Container();
        Label content = new Label("Transitions");
        parent.add(content);

        ProgressAnimation progress = ProgressAnimation.markComponentLoading(content, EmptyAnimation.class);
        assertSame(progress, parent.getComponentAt(0));

        Transition transition = mockTransition();
        ProgressAnimation.markComponentReady(content, transition);
        assertSame(content, parent.getComponentAt(0));
        assertNull(ProgressAnimation.getProgressAnimation(content));
    }

    @Test
    void testCircleProgressAnimateWrapsStepValue() throws Exception {
        CircleProgress progress = new CircleProgress();
        Field stepField = CircleProgress.class.getDeclaredField("step");
        stepField.setAccessible(true);
        Field stepSizeField = CircleProgress.class.getDeclaredField("stepSize");
        stepSizeField.setAccessible(true);

        int stepSize = stepSizeField.getInt(progress);
        progress.animate();
        assertEquals(stepSize % 720, stepField.getInt(progress));

        stepField.setInt(progress, 719);
        progress.animate();
        assertEquals((719 + stepSize) % 720, stepField.getInt(progress));
    }

    @Test
    void testEmptyAnimationPreferredSizeMatchesContent() {
        EmptyAnimation animation = new EmptyAnimation();
        Dimension preferred = animation.getPreferredSize();
        assertTrue(preferred.getWidth() > 0);
        assertTrue(preferred.getHeight() > 0);
    }

    @Test
    void testLoadingTextAnimationEntersPauseCycle() throws Exception {
        LoadingTextAnimation animation = new LoadingTextAnimation();
        Field cyclesPerChunk = LoadingTextAnimation.class.getDeclaredField("cyclesPerChunk");
        cyclesPerChunk.setAccessible(true);
        cyclesPerChunk.setInt(animation, 2);
        Field lettersPerChunk = LoadingTextAnimation.class.getDeclaredField("lettersPerChunk");
        lettersPerChunk.setAccessible(true);
        lettersPerChunk.setInt(animation, 1);
        Field strlenField = LoadingTextAnimation.class.getDeclaredField("strlen");
        strlenField.setAccessible(true);
        strlenField.setInt(animation, 1);
        Field pauseCounterField = LoadingTextAnimation.class.getDeclaredField("pauseCounter");
        pauseCounterField.setAccessible(true);
        Field pauseLengthField = LoadingTextAnimation.class.getDeclaredField("pauseLength");
        pauseLengthField.setAccessible(true);
        pauseLengthField.setInt(animation, 3);

        assertTrue(animation.animate());
        assertTrue(pauseCounterField.getInt(animation) > 0);

        int initialPause = pauseCounterField.getInt(animation);
        animation.animate();
        assertEquals(initialPause - 1, pauseCounterField.getInt(animation));
    }

    private Transition mockTransition() {
        return new Transition() {
            @Override
            public boolean animate() {
                return false;
            }

            @Override
            public void paint(Graphics g) {
            }
        };
    }

    @AfterEach
    void drainSerialCalls() {
        flushSerialCalls();
    }
}
