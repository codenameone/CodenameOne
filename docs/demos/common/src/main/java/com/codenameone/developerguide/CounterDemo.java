package com.codenameone.developerguide;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Simple interactive demo that lets the user update a counter using buttons and a slider.
 */
public class CounterDemo implements Demo {
    @Override
    public String getTitle() {
        return "Counter";
    }

    @Override
    public String getDescription() {
        return "Interactive counter with increment/decrement controls and a slider.";
    }

    @Override
    public void show(Form parent) {
        Form form = new Form("Counter", BoxLayout.y());
        form.setName("counterForm");
        form.getToolbar().setBackCommand("Back", e -> parent.showBack());

        SpanLabel valueLabel = new SpanLabel("Current value: 0");
        valueLabel.setName("counterValueLabel");

        Slider slider = new Slider();
        slider.setName("counterSlider");
        slider.setEditable(true);
        slider.setMinValue(0);
        slider.setMaxValue(20);
        slider.setProgress(0);

        Button increment = new Button("Increment");
        increment.setName("incrementButton");
        Button decrement = new Button("Decrement");
        decrement.setName("decrementButton");

        increment.addActionListener(e -> adjustValue(slider, valueLabel, Math.min(slider.getProgress() + 1, slider.getMaxValue())));
        decrement.addActionListener(e -> adjustValue(slider, valueLabel, Math.max(slider.getProgress() - 1, slider.getMinValue())));
        slider.addDataChangedListener((type, index) -> updateLabel(valueLabel, slider.getProgress()));

        form.addAll(valueLabel, slider, increment, decrement);
        form.show();
    }

    private void adjustValue(Slider slider, SpanLabel valueLabel, int newValue) {
        slider.setProgress(newValue);
        updateLabel(valueLabel, newValue);
    }

    private void updateLabel(SpanLabel valueLabel, int value) {
        valueLabel.setText("Current value: " + value);
        valueLabel.repaint();
    }
}
