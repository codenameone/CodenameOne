package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.SelectionListener;

import static org.junit.jupiter.api.Assertions.*;

class SpinnerPackageTest extends UITestBase {

    @FormTest
    void numericSpinnerReflectsPropertyChanges() {
        NumericSpinner spinner = new NumericSpinner();
        spinner.setMin(5);
        spinner.setMax(15);
        spinner.setStep(2);
        spinner.setValue(9);
        spinner.initSpinner();

        assertEquals(5.0, spinner.getMin());
        assertEquals(15.0, spinner.getMax());
        assertEquals(2.0, spinner.getStep());
        assertEquals(9.0, spinner.getValue());

        assertEquals(new Double(5.0), spinner.getPropertyValue("min"));
        assertEquals(new Double(15.0), spinner.getPropertyValue("max"));
        assertEquals(new Double(9.0), spinner.getPropertyValue("value"));
        assertEquals(new Double(2.0), spinner.getPropertyValue("step"));

        spinner.setPropertyValue("min", 7);
        spinner.setPropertyValue("max", 12);
        spinner.setPropertyValue("value", 10);
        spinner.setPropertyValue("step", 1.5);

        assertEquals(7.0, spinner.getMin());
        assertEquals(12.0, spinner.getMax());
        assertEquals(10.0, spinner.getValue());
        assertEquals(1.5, spinner.getStep());
    }

    @FormTest
    void spinnerNumberModelNotifiesSelectionListeners() {
        SpinnerNumberModel model = new SpinnerNumberModel(0, 10, 0, 1);
        RecordingSelection selection = new RecordingSelection();
        model.addSelectionListener(selection);
        model.setSelectedIndex(3);
        assertEquals(3, model.getSelectedIndex());
        assertTrue(selection.wasNotified);
        assertEquals(3, model.getItemAt(model.getSelectedIndex()));

        selection.wasNotified = false;
        model.setSelectedIndex(model.getSelectedIndex());
        assertFalse(selection.wasNotified);
    }

    private static class RecordingSelection implements SelectionListener {
        private boolean wasNotified;

        public void selectionChanged(int oldSelection, int newSelection) {
            wasNotified = true;
        }
    }
}
