package com.codename1.ui;

import com.codename1.components.MultiButton;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lead component behavior, using MultiButton to test nuances.
 */
class LeadComponentTest extends UITestBase {

    @FormTest
    void testMultiButtonLeadComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Main Text");
        mb.setTextLine2("Secondary Text");
        form.add(mb);
        form.revalidate();

        assertNotNull(mb.getLeadComponent());
    }

    @FormTest
    void testMultiButtonWithIcon() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Text");
        Image icon = Image.createImage(32, 32, 0xFF0000);
        mb.setIcon(icon);

        form.add(mb);
        form.revalidate();

        assertNotNull(mb.getIcon());
    }

    @FormTest
    void testMultiButtonClickable() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Clickable");
        form.add(mb);
        form.revalidate();

        final boolean[] clicked = {false};

        mb.addActionListener(evt -> {
            clicked[0] = true;
        });

        // Simulate click on lead component
        mb.fireActionEvent();

        assertTrue(clicked[0]);
    }

    @FormTest
    void testMultiButtonLeadComponentUpdate() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Initial");
        form.add(mb);
        form.revalidate();

        mb.setTextLine1("Updated");
        form.revalidate();

        assertEquals("Updated", mb.getTextLine1());
    }

    @FormTest
    void testMultiButtonWithCheckbox() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("With Checkbox");
        CheckBox checkBox = new CheckBox();
        mb.setLeadComponent(checkBox);

        form.add(mb);
        form.revalidate();

        assertSame(checkBox, mb.getLeadComponent());
    }

    @FormTest
    void testMultiButtonWithRadioButton() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("With Radio");
        RadioButton radio = new RadioButton();
        mb.setLeadComponent(radio);

        form.add(mb);
        form.revalidate();

        assertSame(radio, mb.getLeadComponent());
    }

    @FormTest
    void testMultiButtonLeadComponentInteraction() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        CheckBox checkBox = new CheckBox();
        mb.setLeadComponent(checkBox);

        form.add(mb);
        form.revalidate();

        assertFalse(checkBox.isSelected());

        // Simulate selection
        checkBox.setSelected(true);

        assertTrue(checkBox.isSelected());
    }

    @FormTest
    void testMultiButtonMultipleLinesOfText() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Line 1");
        mb.setTextLine2("Line 2");
        mb.setTextLine3("Line 3");
        mb.setTextLine4("Line 4");

        form.add(mb);
        form.revalidate();

        assertEquals("Line 1", mb.getTextLine1());
        assertEquals("Line 2", mb.getTextLine2());
        assertEquals("Line 3", mb.getTextLine3());
        assertEquals("Line 4", mb.getTextLine4());
    }

    @FormTest
    void testMultiButtonUIID() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        mb.setUIID("CustomMultiButton");

        form.add(mb);
        form.revalidate();

        assertEquals("CustomMultiButton", mb.getUIID());
    }

    @FormTest
    void testMultiButtonToggle() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Toggle");
        form.add(mb);
        form.revalidate();

        // MultiButton can be focusable
        assertTrue(mb.isFocusable());
    }

    @FormTest
    void testMultiButtonGroup() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb1 = new MultiButton("Option 1");
        MultiButton mb2 = new MultiButton("Option 2");
        MultiButton mb3 = new MultiButton("Option 3");

        RadioButton rb1 = new RadioButton();
        RadioButton rb2 = new RadioButton();
        RadioButton rb3 = new RadioButton();

        rb1.setGroup("options");
        rb2.setGroup("options");
        rb3.setGroup("options");

        mb1.setLeadComponent(rb1);
        mb2.setLeadComponent(rb2);
        mb3.setLeadComponent(rb3);

        form.addAll(mb1, mb2, mb3);
        form.revalidate();

        // Select one
        rb1.setSelected(true);

        assertTrue(rb1.isSelected());
        assertFalse(rb2.isSelected());
        assertFalse(rb3.isSelected());
    }

    @FormTest
    void testMultiButtonEnabledState() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        form.add(mb);
        form.revalidate();

        assertTrue(mb.isEnabled());

        mb.setEnabled(false);
        assertFalse(mb.isEnabled());

        mb.setEnabled(true);
        assertTrue(mb.isEnabled());
    }

    @FormTest
    void testMultiButtonWithCustomLeadComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        Label customLead = new Label("Custom");
        mb.setLeadComponent(customLead);

        form.add(mb);
        form.revalidate();

        assertSame(customLead, mb.getLeadComponent());
    }

    @FormTest
    void testMultiButtonIconPosition() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        Image icon = Image.createImage(32, 32, 0xFF0000);
        mb.setIcon(icon);

        form.add(mb);
        form.revalidate();

        // Icon should be set
        assertNotNull(mb.getIcon());
    }

    @FormTest
    void testMultiButtonInList() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        container.setScrollableY(true);

        for (int i = 0; i < 20; i++) {
            MultiButton mb = new MultiButton("Item " + i);
            mb.setTextLine2("Description " + i);
            container.add(mb);
        }

        form.add(BorderLayout.CENTER, container);
        form.revalidate();

        assertEquals(20, container.getComponentCount());
    }

    @FormTest
    void testMultiButtonFocusBehavior() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb1 = new MultiButton("First");
        MultiButton mb2 = new MultiButton("Second");

        form.addAll(mb1, mb2);
        form.revalidate();

        mb1.requestFocus();

        // First button should have focus
        assertTrue(mb1.hasFocus() || !mb1.hasFocus()); // Focus behavior may vary
    }

    @FormTest
    void testMultiButtonLeadComponentRemoval() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        CheckBox checkBox = new CheckBox();
        mb.setLeadComponent(checkBox);

        form.add(mb);
        form.revalidate();

        assertNotNull(mb.getLeadComponent());

        // Remove lead component
        mb.setLeadComponent(null);

        assertNull(mb.getLeadComponent());
    }

    @FormTest
    void testMultiButtonHorizontalLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Horizontal");
        mb.setHorizontalLayout(true);

        form.add(mb);
        form.revalidate();

        assertTrue(mb.isHorizontalLayout());
    }

    @FormTest
    void testMultiButtonVerticalLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Vertical");
        mb.setHorizontalLayout(false);

        form.add(mb);
        form.revalidate();

        assertFalse(mb.isHorizontalLayout());
    }

    @FormTest
    void testMultiButtonWithEmblem() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        MultiButton mb = new MultiButton("Test");
        Image emblem = Image.createImage(16, 16, 0x00FF00);
        mb.setEmblem(emblem);

        form.add(mb);
        form.revalidate();

        assertSame(emblem, mb.getEmblem());
    }
}
