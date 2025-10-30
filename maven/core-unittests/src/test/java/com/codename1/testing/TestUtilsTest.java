package com.codename1.testing;

import com.codename1.junit.EdtTest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;

import static org.junit.jupiter.api.Assertions.*;

class TestUtilsTest extends UITestBase {

    @FormTest
    void findersAndClicksLocateComponents() {
        SpyForm form = new SpyForm();
        form.setLayout(BoxLayout.y());
        RecordingButton button = new RecordingButton("Tap me");
        button.setName("actionButton");
        Label label = new Label("Hello");
        label.setName("helloLabel");
        form.add(button);
        form.add(label);
        form.show();

        Component located = TestUtils.findByName("actionButton");
        assertSame(button, located);
        assertSame(button, TestUtils.findLabelText("Tap me"));

        TestUtils.clickButtonByName("actionButton");
        TestUtils.clickButtonByLabel("Tap me");

        assertEquals(2, button.getPressedCount());
        assertEquals(2, button.getReleasedCount());
    }

    @FormTest
    void selectionAndVisibilityHelpersWork() {
        SpyForm form = new SpyForm();
        form.setLayout(BoxLayout.y());
        List<String> list = new List<String>(new DefaultListModel<String>("A", "B", "C"));
        list.setName("options");
        Label label = new Label("Target");
        label.setName("targetLabel");
        form.add(list);
        form.add(label);
        form.show();

        TestUtils.selectInList("options", 2);
        assertEquals(2, list.getSelectedIndex());

        TestUtils.selectInList(new int[]{0}, 1);
        assertEquals(1, list.getSelectedIndex());

        TestUtils.ensureVisible("targetLabel");
        assertSame(label, form.lastScrolled);

        TestUtils.ensureVisible(new int[]{1});
        assertSame(label, form.lastScrolled);
    }

    @FormTest
    void setTextUpdatesLabelsAndTextAreas() {
        SpyForm form = new SpyForm();
        form.setLayout(BoxLayout.y());
        Label label = new Label("Initial");
        label.setName("label");
        TextArea area = new TextArea();
        area.setName("field");
        form.add(label);
        form.add(area);
        form.show();
        TestUtils.setText("label", "Updated");
        assertEquals("Updated", label.getText());

        TestUtils.setText("field", "Typed");
        assertEquals("Typed", area.getText());

        TestUtils.setText(new int[]{0}, "Changed");
        assertEquals("Changed", label.getText());

        TestUtils.setText(new int[]{1}, "Replaced");
        assertEquals("Replaced", area.getText());
    }

    @FormTest
    void assertionHelpersValidateExpectations() {
        TestUtils.assertEqual(5, 5);
        RuntimeException mismatch = assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(5, 4));
        assertTrue(mismatch.getMessage().contains("Expected [5], Actual [4]"));

        TestUtils.assertNotEqual("a", "b");
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotEqual("a", "a"));

        TestUtils.assertTrue(true);
        assertThrows(RuntimeException.class, () -> TestUtils.assertTrue(false));

        TestUtils.assertFalse(false);
        assertThrows(RuntimeException.class, () -> TestUtils.assertFalse(true));

        TestUtils.assertNull(null);
        assertThrows(RuntimeException.class, () -> TestUtils.assertNull("value"));

        TestUtils.assertNotNull("value");
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotNull(null));
    }

    private static class RecordingButton extends Button {
        private int pressedCount;
        private int releasedCount;

        RecordingButton(String text) {
            super(text);
        }

        public void pressed() {
            pressedCount++;
            super.pressed();
        }

        public void released() {
            releasedCount++;
            super.released();
        }

        int getPressedCount() {
            return pressedCount;
        }

        int getReleasedCount() {
            return releasedCount;
        }
    }

    private static class SpyForm extends Form {
        private Component lastScrolled;

        public void scrollComponentToVisible(Component c) {
            lastScrolled = c;
            super.scrollComponentToVisible(c);
        }
    }
}
