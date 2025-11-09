package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class InterFormContainerTest extends UITestBase {

    @FormTest
    void testConstructorWithForm() {
        Form childForm = new Form("Child");
        InterFormContainer container = new InterFormContainer(childForm);
        assertNotNull(container);
    }

    @FormTest
    void testAddToParentForm() {
        Form parent = Display.getInstance().getCurrent();
        parent.setLayout(new BorderLayout());

        Form child = new Form("Child");
        InterFormContainer container = new InterFormContainer(child);

        parent.add(BorderLayout.CENTER, container);
        parent.revalidate();

        // Verify container was added
        assertTrue(parent.contains(container));
    }

    @FormTest
    void testRevalidate() {
        Form childForm = new Form("Child");
        childForm.setLayout(new BorderLayout());
        childForm.add(BorderLayout.CENTER, new Label("Content"));

        InterFormContainer container = new InterFormContainer(childForm);

        Form parent = Display.getInstance().getCurrent();
        parent.setLayout(new BorderLayout());
        parent.add(BorderLayout.CENTER, container);

        parent.revalidate();

        // Just verify revalidate doesn't crash
        assertNotNull(container);
    }

    @FormTest
    void testChildFormWithComponents() {
        Form childForm = new Form("Child");
        childForm.setLayout(new BorderLayout());

        Label label = new Label("Test Label");
        Button button = new Button("Test Button");

        childForm.add(BorderLayout.NORTH, label);
        childForm.add(BorderLayout.SOUTH, button);

        InterFormContainer container = new InterFormContainer(childForm);

        Form parent = Display.getInstance().getCurrent();
        parent.setLayout(new BorderLayout());
        parent.add(BorderLayout.CENTER, container);
        parent.revalidate();

        // Verify child form components are present
        assertTrue(childForm.contains(label));
        assertTrue(childForm.contains(button));
    }

    @FormTest
    void testMultipleInterFormContainers() {
        Form parent = Display.getInstance().getCurrent();
        parent.removeAll();
        parent.setLayout(new BorderLayout());

        Form child1 = new Form("Child1");
        child1.add(new Label("First"));
        InterFormContainer container1 = new InterFormContainer(child1);

        Form child2 = new Form("Child2");
        child2.add(new Label("Second"));
        InterFormContainer container2 = new InterFormContainer(child2);

        Container wrapper = new Container(new BorderLayout());
        wrapper.add(BorderLayout.NORTH, container1);
        wrapper.add(BorderLayout.SOUTH, container2);

        parent.add(BorderLayout.CENTER, wrapper);
        parent.revalidate();

        assertNotNull(container1);
        assertNotNull(container2);
    }

    @FormTest
    void testInterFormContainerSize() {
        Form childForm = new Form("Child");
        childForm.setLayout(new BorderLayout());
        childForm.add(new Label("Content"));

        InterFormContainer container = new InterFormContainer(childForm);

        Form parent = Display.getInstance().getCurrent();
        parent.setLayout(new BorderLayout());
        parent.add(BorderLayout.CENTER, container);
        parent.revalidate();

        int width = container.getWidth();
        int height = container.getHeight();

        assertTrue(width >= 0);
        assertTrue(height >= 0);
    }

    @FormTest
    void testUIID() {
        Form childForm = new Form("Child");
        InterFormContainer container = new InterFormContainer(childForm);

        container.setUIID("CustomInterForm");
        assertEquals("CustomInterForm", container.getUIID());
    }
}
