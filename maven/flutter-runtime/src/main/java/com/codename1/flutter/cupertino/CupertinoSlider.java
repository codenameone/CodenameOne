package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Slider;

import dart.runtime.Funcs;

/**
 * An iOS-style slider — Flutter's {@code CupertinoSlider}. Same controlled
 * double-range semantics as the material slider; composed onto material
 * {@link Slider} (visually approximate this pass).
 */
public class CupertinoSlider extends StatelessWidget {

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

    public void onChangeStart(Object v) {
    }

    public void onChangeEnd(Object v) {
    }

    public void activeColor(Color v) {
    }

    public void thumbColor(Color v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Slider s = new Slider();
        s.value(value);
        if (min != null) {
            s.min(min);
        }
        if (max != null) {
            s.max(max);
        }
        if (divisions != null) {
            s.divisions(divisions);
        }
        s.onChanged(onChanged);
        return s;
    }
}
