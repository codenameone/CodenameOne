package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class ComponentSelectorTest extends UITestBase {

    @FormTest
    void testSelectByUIID() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(BoxLayout.y());

        Button button1 = new Button("Button1");
        button1.setUIID("MyButton");

        Button button2 = new Button("Button2");
        button2.setUIID("MyButton");

        Label label = new Label("Label");
        label.setUIID("MyLabel");

        form.addAll(button1, button2, label);
        form.revalidate();

        ComponentSelector selector = ComponentSelector.$("MyButton", form);
        assertEquals(2, selector.size());
    }

    @FormTest
    void testSelectSingleComponent() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(BorderLayout.center());

        Label label = new Label("Test");
        label.setUIID("UniqueLabel");
        form.add(label);
        form.revalidate();

        ComponentSelector selector = ComponentSelector.$("UniqueLabel", form);
        assertEquals(1, selector.size());
    }

    @FormTest
    void testSelectNoMatch() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.add(new Label("Label"));
        form.revalidate();

        ComponentSelector selector = ComponentSelector.$("NonExistentUIID", form);
        assertEquals(0, selector.size());
    }

    @FormTest
    void testSelectNested() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        Button button = new Button("Nested");
        button.setUIID("NestedButton");
        container.add(button);

        form.add(BorderLayout.CENTER, container);
        form.revalidate();

        ComponentSelector selector = ComponentSelector.$("NestedButton", form);
        assertEquals(1, selector.size());
    }

    @FormTest
    void testSetText() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label1 = new Label("Original1");
        label1.setUIID("TestLabel");
        Label label2 = new Label("Original2");
        label2.setUIID("TestLabel");

        form.addAll(label1, label2);
        form.revalidate();

        ComponentSelector.$("TestLabel", form).setText("Updated");

        assertEquals("Updated", label1.getText());
        assertEquals("Updated", label2.getText());
    }

    @FormTest
    void testSetUIID() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Button button = new Button("Test");
        button.setUIID("OldUIID");
        form.add(button);
        form.revalidate();

        ComponentSelector.$("OldUIID", form).setUIID("NewUIID");

        assertEquals("NewUIID", button.getUIID());
    }

    @FormTest
    void testSetEnabled() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Button button1 = new Button("Button1");
        button1.setUIID("MyButton");
        Button button2 = new Button("Button2");
        button2.setUIID("MyButton");

        form.addAll(button1, button2);
        form.revalidate();

        ComponentSelector.$("MyButton", form).setEnabled(false);

        assertFalse(button1.isEnabled());
        assertFalse(button2.isEnabled());
    }

    @FormTest
    void testSetVisible() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label("Test");
        label.setUIID("TestLabel");
        form.add(label);
        form.revalidate();

        ComponentSelector.$("TestLabel", form).setVisible(false);

        assertFalse(label.isVisible());
    }

    @FormTest
    void testGetParent() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Container container = new Container(BoxLayout.y());
        Button button = new Button("Test");
        button.setUIID("TestButton");
        container.add(button);
        form.add(container);
        form.revalidate();

        ComponentSelector parent = ComponentSelector.$("TestButton", form).getParent();
        assertNotNull(parent);
    }

    @FormTest
    void testMultipleSelectors() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Button button = new Button("Button");
        button.setUIID("Button");
        Label label = new Label("Label");
        label.setUIID("Label");

        form.addAll(button, label);
        form.revalidate();

        ComponentSelector buttonSelector = ComponentSelector.$("Button", form);
        ComponentSelector labelSelector = ComponentSelector.$("Label", form);

        assertEquals(1, buttonSelector.size());
        assertEquals(1, labelSelector.size());
    }

    @FormTest
    void testChaining() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Button button = new Button("Original");
        button.setUIID("ChainTest");
        form.add(button);
        form.revalidate();

        ComponentSelector.$("ChainTest", form)
            .setText("Updated")
            .setEnabled(false);

        assertEquals("Updated", button.getText());
        assertFalse(button.isEnabled());
    }

    @FormTest
    void testSelectFromCurrentForm() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label("Test");
        label.setUIID("CurrentFormLabel");
        form.add(label);
        form.revalidate();

        ComponentSelector selector = ComponentSelector.$("CurrentFormLabel");
        assertEquals(1, selector.size());
    }
}
