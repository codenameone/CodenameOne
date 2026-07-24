package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A slider selecting a range between two thumbs — Flutter's {@code RangeSlider}.
 * This milestone renders a single {@link Slider} spanning the current range
 * (its thumb at {@code values.start}); the second thumb and range-drag gestures
 * are deferred. {@code onChanged} carries a {@link RangeValues}.
 */
public class RangeSlider extends StatelessWidget {

    private RangeValues values;
    private Double min;
    private Double max;
    private Long divisions;
    private RangeLabels labels;
    private Color activeColor;
    private Color inactiveColor;
    private Funcs.VoidFunc1<RangeValues> onChanged;
    private Funcs.VoidFunc1<RangeValues> onChangeStart;
    private Funcs.VoidFunc1<RangeValues> onChangeEnd;

    public void values(RangeValues v) {
        this.values = v;
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

    public void labels(RangeLabels v) {
        this.labels = v;
    }

    public void activeColor(Color v) {
        this.activeColor = v;
    }

    public void inactiveColor(Color v) {
        this.inactiveColor = v;
    }

    public void onChanged(Funcs.VoidFunc1<RangeValues> v) {
        this.onChanged = v;
    }

    public void onChangeStart(Funcs.VoidFunc1<RangeValues> v) {
        this.onChangeStart = v;
    }

    public void onChangeEnd(Funcs.VoidFunc1<RangeValues> v) {
        this.onChangeEnd = v;
    }

    public void semanticFormatterCallback(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Slider s = new Slider();
        s.min(min == null ? 0.0 : min);
        s.max(max == null ? 1.0 : max);
        s.value(values != null ? values.start() : (min == null ? 0.0 : min));
        if (divisions != null) {
            s.divisions(divisions);
        }
        return s;
    }
}
