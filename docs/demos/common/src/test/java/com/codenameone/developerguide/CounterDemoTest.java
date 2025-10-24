package com.codenameone.developerguide;

import com.codename1.components.SpanLabel;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.events.ActionEvent;

/**
 * Validates the Counter demo's interactions update the UI consistently.
 */
public class CounterDemoTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        Form parent = new Form("Parent");
        parent.show();
        TestUtils.waitForFormTitle("Parent", 5000);

        Demo demo = new CounterDemo();
        demo.show(parent);
        TestUtils.waitForFormTitle("Counter", 5000);

        Form current = Display.getInstance().getCurrent();
        assertEqual("Counter", current.getTitle());
        assertEqual("counterForm", current.getName());

        SpanLabel valueLabel = (SpanLabel) TestUtils.findByName("counterValueLabel");
        assertNotNull(valueLabel, "Counter value label should exist.");
        assertEqual("Current value: 0", valueLabel.getText());

        Slider slider = (Slider) TestUtils.findByName("counterSlider");
        assertNotNull(slider, "Counter slider should exist.");
        assertEqual(0, slider.getProgress());

        Button increment = (Button) TestUtils.findByName("incrementButton");
        Button decrement = (Button) TestUtils.findByName("decrementButton");
        assertNotNull(increment);
        assertNotNull(decrement);

        increment.pressed();
        increment.released();
        TestUtils.waitFor(200);
        assertEqual(1, slider.getProgress());
        assertEqual("Current value: 1", valueLabel.getText());

        slider.setProgress(slider.getMaxValue());
        TestUtils.waitFor(200);
        assertEqual("Current value: " + slider.getMaxValue(), valueLabel.getText());

        increment.pressed();
        increment.released();
        TestUtils.waitFor(200);
        assertEqual(slider.getMaxValue(), slider.getProgress());
        assertEqual("Current value: " + slider.getMaxValue(), valueLabel.getText());

        decrement.pressed();
        decrement.released();
        TestUtils.waitFor(200);
        assertEqual(slider.getMaxValue() - 1, slider.getProgress());
        assertEqual("Current value: " + (slider.getMaxValue() - 1), valueLabel.getText());

        slider.setProgress(slider.getMinValue());
        TestUtils.waitFor(200);
        decrement.pressed();
        decrement.released();
        TestUtils.waitFor(200);
        assertEqual(slider.getMinValue(), slider.getProgress());
        assertEqual("Current value: " + slider.getMinValue(), valueLabel.getText());

        Command back = current.getBackCommand();
        assertNotNull(back, "Back command should be available.");
        back.actionPerformed(new ActionEvent(back));
        TestUtils.waitForFormTitle("Parent", 5000);
        assertEqual(parent, Display.getInstance().getCurrent());

        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
}
