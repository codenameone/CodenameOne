package com.codename1.ui.animations;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.LayeredLayout;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FlipTransition}.
 */
public class FlipTransitionTest extends UITestBase {

    @Test
    public void testDefaultConfiguration() {
        FlipTransition transition = new FlipTransition();
        assertEquals(200, transition.getDuration());
        assertEquals(-1, transition.getBgColor());
    }

    @Test
    public void testCustomConfiguration() {
        FlipTransition transition = new FlipTransition(0x123456, 450);
        assertEquals(450, transition.getDuration());
        assertEquals(0x123456, transition.getBgColor());

        transition.setDuration(300);
        transition.setBgColor(0x654321);
        assertEquals(300, transition.getDuration());
        assertEquals(0x654321, transition.getBgColor());
    }

    @Test
    public void testCopyProducesNewInstance() {
        FlipTransition transition = new FlipTransition(0x111111, 350);
        FlipTransition copy = (FlipTransition) transition.copy(false);
        assertNotSame(transition, copy);
        assertEquals(transition.getDuration(), copy.getDuration());
        assertEquals(transition.getBgColor(), copy.getBgColor());
    }

    @Test
    public void testInitTransitionPreparesBuffers() throws Exception {
        FlipTransition transition = new FlipTransition(0, 10);
        Form source = createForm("source");
        Form destination = createForm("destination");
        transition.init(source, destination);
        transition.initTransition();

        Field sourceBufferField = FlipTransition.class.getDeclaredField("sourceBuffer");
        sourceBufferField.setAccessible(true);
        assertNotNull(sourceBufferField.get(transition));

        Field destBufferField = FlipTransition.class.getDeclaredField("destBuffer");
        destBufferField.setAccessible(true);
        assertNotNull(destBufferField.get(transition));

        Field motionField = FlipTransition.class.getDeclaredField("motion");
        motionField.setAccessible(true);
        Motion motion = (Motion) motionField.get(transition);
        assertNotNull(motion);

        Field stateField = FlipTransition.class.getDeclaredField("transitionState");
        stateField.setAccessible(true);
        int state = stateField.getInt(transition);
        assertEquals(getStaticIntField("STATE_MOVE_AWAY"), state);
    }

    @Test
    public void testAnimateProgressesStates() throws Exception {
        FlipTransition transition = new FlipTransition(0, 5);
        Form source = createForm("source");
        Form destination = createForm("destination");
        transition.init(source, destination);
        transition.initTransition();

        Field motionField = FlipTransition.class.getDeclaredField("motion");
        motionField.setAccessible(true);
        Field stateField = FlipTransition.class.getDeclaredField("transitionState");
        stateField.setAccessible(true);

        Motion motion = (Motion) motionField.get(transition);
        motion.setCurrentMotionTime(transition.getDuration() + 1);
        assertTrue(transition.animate());
        assertEquals(getStaticIntField("STATE_FLIP"), stateField.getInt(transition));

        motion = (Motion) motionField.get(transition);
        motion.setCurrentMotionTime(transition.getDuration() + 1);
        assertTrue(transition.animate());
        assertEquals(getStaticIntField("STATE_MOVE_CLOSER"), stateField.getInt(transition));

        motion = (Motion) motionField.get(transition);
        motion.setCurrentMotionTime(transition.getDuration() + 1);
        assertFalse(transition.animate());
    }

    @Test
    public void testCleanupReleasesBuffers() throws Exception {
        FlipTransition transition = new FlipTransition();
        Form source = createForm("source");
        Form destination = createForm("destination");
        transition.init(source, destination);
        transition.initTransition();

        transition.cleanup();

        Field sourceBufferField = FlipTransition.class.getDeclaredField("sourceBuffer");
        sourceBufferField.setAccessible(true);
        assertNull(sourceBufferField.get(transition));

        Field destBufferField = FlipTransition.class.getDeclaredField("destBuffer");
        destBufferField.setAccessible(true);
        assertNull(destBufferField.get(transition));
    }

    private Form createForm(String name) {
        Form form = new Form();
        form.setName(name);
        form.setWidth(240);
        form.setHeight(320);
        form.getContentPane().setWidth(240);
        form.getContentPane().setHeight(320);
        form.getContentPane().setLayout(new LayeredLayout());

        Label label = new Label(name);
        label.setName(name + "Label");
        label.setPreferredSize(new Dimension(80, 40));
        label.setWidth(80);
        label.setHeight(40);
        label.setX(20);
        label.setY(30);
        form.add(label);
        form.layoutContainer();
        form.getContentPane().layoutContainer();
        return form;
    }

    private int getStaticIntField(String name) throws Exception {
        Field field = FlipTransition.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }
}
