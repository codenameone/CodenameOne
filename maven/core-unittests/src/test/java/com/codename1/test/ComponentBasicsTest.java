package com.codename1.test;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentBasicsTest extends UITestBase {
    @Test
    void componentPropertyMutations() {
        Label label = new Label("Hello");
        label.setName("greetingLabel");
        label.setUIID("MyLabel");
        label.setVisible(false);
        label.setEnabled(false);
        label.setRTL(true);
        label.setDraggable(true);

        assertEquals("greetingLabel", label.getName());
        assertEquals("MyLabel", label.getUIID());
        assertFalse(label.isVisible());
        assertFalse(label.isEnabled());
        assertTrue(label.isRTL());
        assertTrue(label.isDraggable());
    }

    @Test
    void buttonToggleConfiguration() {
        Button button = new Button("Tap");
        button.setToggle(true);
        button.setText("Updated");
        button.setGap(5);
        button.setUIID("ActionButton");

        assertTrue(button.isToggle());
        assertEquals("Updated", button.getText());
        assertEquals(5, button.getGap());
        assertEquals("ActionButton", button.getUIID());
    }

    @Test
    void checkBoxAndRadioSelection() {
        CheckBox checkBox = new CheckBox("Accept");
        assertFalse(checkBox.isSelected());
        checkBox.setSelected(true);
        assertTrue(checkBox.isSelected());

        RadioButton radioButton = new RadioButton("Choice A");
        radioButton.setGroup("choices");
        radioButton.setSelected(true);

        assertTrue(radioButton.isSelected());
        assertEquals("choices", radioButton.getGroup());
    }

    @Test
    void textFieldEditingAndHints() {
        TextField textField = new TextField();
        textField.setText("123");
        textField.setConstraint(TextArea.NUMERIC);
        textField.setEditable(true);
        textField.setHint("Enter numbers");

        assertEquals("123", textField.getText());
        assertEquals(TextArea.NUMERIC, textField.getConstraint());
        assertTrue(textField.isEditable());
        assertEquals("Enter numbers", textField.getHint());
    }

    @Test
    void sliderRangeConfiguration() {
        Slider slider = new Slider();
        slider.setMinValue(0);
        slider.setMaxValue(100);
        slider.setProgress(40);
        slider.setEditable(true);
        slider.setIncrements(5);
        slider.setVertical(true);

        assertEquals(0, slider.getMinValue());
        assertEquals(100, slider.getMaxValue());
        assertEquals(40, slider.getProgress());
        assertTrue(slider.isEditable());
        assertEquals(5, slider.getIncrements());
        assertTrue(slider.isVertical());
    }
}
