package com.codename1.ui.animations;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.LayeredLayout;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MorphTransition}.
 */
public class MorphTransitionTest extends UITestBase {

    @Test
    public void testMorphMappingAndCopy() throws Exception {
        MorphTransition transition = MorphTransition.create(300)
                .morph("a")
                .morph("b", "c");

        Field fromToField = MorphTransition.class.getDeclaredField("fromTo");
        fromToField.setAccessible(true);
        Map<?, ?> mapping = (Map<?, ?>) fromToField.get(transition);
        assertEquals("a", mapping.get("a"));
        assertEquals("c", mapping.get("b"));

        MorphTransition reversed = (MorphTransition) transition.copy(true);
        Map<?, ?> reversedMap = (Map<?, ?>) fromToField.get(reversed);
        assertEquals("b", reversedMap.get("c"));
        assertEquals("a", reversedMap.get("a"));
    }

    @Test
    public void testInitTransitionSetsUpComponents() throws Exception {
        Form source = createFormWithComponent("sourceForm", "shared", 10, 20, 40, 30);
        Form destination = createFormWithComponent("destinationForm", "shared", 120, 150, 80, 60);

        MorphTransition transition = MorphTransition.create(200).morph("shared");
        transition.init(source, destination);
        transition.initTransition();

        Field componentsField = MorphTransition.class.getDeclaredField("fromToComponents");
        componentsField.setAccessible(true);
        Object[] ccArray = (Object[]) componentsField.get(transition);
        assertNotNull(ccArray);
        assertTrue(ccArray.length > 0);
        assertNotNull(ccArray[0]);

        Field motionField = MorphTransition.class.getDeclaredField("animationMotion");
        motionField.setAccessible(true);
        Motion animationMotion = (Motion) motionField.get(transition);
        assertNotNull(animationMotion);

        Field finishedField = MorphTransition.class.getDeclaredField("finished");
        finishedField.setAccessible(true);
        assertFalse(finishedField.getBoolean(transition));

        Object cc = ccArray[0];
        Class<?> ccClass = cc.getClass();
        Field xMotionField = ccClass.getDeclaredField("xMotion");
        xMotionField.setAccessible(true);
        Motion xMotion = (Motion) xMotionField.get(cc);
        xMotion.setCurrentMotionTime(transitionTime(animationMotion, 100));

        Field yMotionField = ccClass.getDeclaredField("yMotion");
        yMotionField.setAccessible(true);
        Motion yMotion = (Motion) yMotionField.get(cc);
        yMotion.setCurrentMotionTime(transitionTime(animationMotion, 100));

        Field wMotionField = ccClass.getDeclaredField("wMotion");
        wMotionField.setAccessible(true);
        Motion wMotion = (Motion) wMotionField.get(cc);
        wMotion.setCurrentMotionTime(transitionTime(animationMotion, 100));

        Field hMotionField = ccClass.getDeclaredField("hMotion");
        hMotionField.setAccessible(true);
        Motion hMotion = (Motion) hMotionField.get(cc);
        hMotion.setCurrentMotionTime(transitionTime(animationMotion, 100));

        assertTrue(transition.animate());

        Label sourceLabel = findLabel(source, "shared");
        Label destLabel = findLabel(destination, "shared");
        assertEquals(xMotion.getValue(), sourceLabel.getX());
        assertEquals(yMotion.getValue(), sourceLabel.getY());
        assertEquals(wMotion.getValue(), sourceLabel.getWidth());
        assertEquals(hMotion.getValue(), sourceLabel.getHeight());
        assertEquals(sourceLabel.getX(), destLabel.getX());
        assertEquals(sourceLabel.getY(), destLabel.getY());
    }

    @Test
    public void testAnimateCompletionRestoresComponents() throws Exception {
        Form source = createFormWithComponent("sourceForm", "alpha", 5, 15, 50, 35);
        Form destination = createFormWithComponent("destinationForm", "alpha", 100, 120, 90, 70);

        Container sourceParent = source.getContentPane();
        Container destinationParent = destination.getContentPane();
        Label sourceLabel = findLabel(source, "alpha");
        Label destinationLabel = findLabel(destination, "alpha");

        MorphTransition transition = MorphTransition.create(150).morph("alpha");
        transition.init(source, destination);
        transition.initTransition();

        Field motionField = MorphTransition.class.getDeclaredField("animationMotion");
        motionField.setAccessible(true);
        Motion animationMotion = (Motion) motionField.get(transition);
        animationMotion.setCurrentMotionTime(transitionTime(animationMotion, 1000));
        assertTrue(transition.animate());

        Field componentsField = MorphTransition.class.getDeclaredField("fromToComponents");
        componentsField.setAccessible(true);
        assertNull(componentsField.get(transition));

        Field finishedField = MorphTransition.class.getDeclaredField("finished");
        finishedField.setAccessible(true);
        assertTrue(finishedField.getBoolean(transition));

        assertSame(sourceParent, sourceLabel.getParent());
        assertSame(destinationParent, destinationLabel.getParent());
        assertFalse(transition.animate());
    }

    private int transitionTime(Motion motion, int offset) {
        return motion.getDuration() + offset;
    }

    private Form createFormWithComponent(String formName, String componentName, int x, int y, int w, int h) {
        Form form = new Form(new LayeredLayout());
        form.setName(formName);
        form.setWidth(240);
        form.setHeight(320);
        form.getContentPane().setWidth(240);
        form.getContentPane().setHeight(320);
        form.getContentPane().setLayout(new LayeredLayout());

        Label label = new Label(componentName);
        label.setName(componentName);
        label.setWidth(w);
        label.setHeight(h);
        label.setPreferredSize(new com.codename1.ui.geom.Dimension(w, h));
        label.setX(x);
        label.setY(y);
        form.add(label);
        form.layoutContainer();
        form.getContentPane().layoutContainer();
        return form;
    }

    private Label findLabel(Container root, String name) {
        Label label = findLabelRecursive(root, name);
        if (label != null) {
            return label;
        }
        if (root instanceof Form) {
            Form form = (Form) root;
            label = findLabelRecursive(form.getLayeredPane(), name);
            if (label != null) {
                return label;
            }
        }
        throw new IllegalStateException("Label not found: " + name);
    }

    private Label findLabelRecursive(Container container, String name) {
        if (container == null) {
            return null;
        }
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component cmp = container.getComponentAt(i);
            if (cmp instanceof Label && name.equals(cmp.getName())) {
                return (Label) cmp;
            }
            if (cmp instanceof Container) {
                Label nested = findLabelRecursive((Container) cmp, name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
