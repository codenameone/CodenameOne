package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material slider over a double range with CONTROLLED semantics: drags
 * fire {@code onChanged(newValue)} and the thumb snaps back to the widget's
 * configured {@code value} until a rebuild moves it. Backed by a CN1
 * {@link com.codename1.ui.Slider} (UIID "FlutterSlider") whose int progress
 * model the double range is scaled onto ({@code divisions} steps when given,
 * a fine-grained default otherwise).
 */
public class Slider extends Widget {

    private double value;
    private Double min;
    private Double max;
    private Long divisions;
    private Funcs.VoidFunc1<Double> onChanged;

    public void value(double v) {
        this.value = v;
    }

    public void min(double v) {
        this.min = v;
    }

    public void max(double v) {
        this.max = v;
    }

    public void divisions(long v) {
        this.divisions = v;
    }

    public void onChanged(Funcs.VoidFunc1<Double> v) {
        this.onChanged = v;
    }

    public double getValue() {
        return value;
    }

    /** Flutter default: 0.0. */
    public double getMin() {
        return min == null ? 0.0 : min;
    }

    /** Flutter default: 1.0. */
    public double getMax() {
        return max == null ? 1.0 : max;
    }

    public Long getDivisions() {
        return divisions;
    }

    public Funcs.VoidFunc1<Double> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new SliderRenderElement(this);
    }
}
