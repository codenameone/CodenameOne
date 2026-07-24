package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A circular material progress indicator — Flutter's {@code
 * CircularProgressIndicator}. A determinate {@code value} or an indeterminate
 * spin are accepted; this pass reserves a square box sized to the default
 * indicator diameter, deferring the arc paint and spin animation.
 */
public class CircularProgressIndicator extends StatelessWidget {

    private Double value;
    private Color color;
    private Color backgroundColor;
    private Double strokeWidth;

    public void value(double v) {
        this.value = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void valueColor(Object v) {
    }

    public void strokeWidth(double v) {
        this.strokeWidth = v;
    }

    public void semanticsLabel(String v) {
    }

    public void semanticsValue(String v) {
    }

    public Double getValue() {
        return value;
    }

    @Override
    public Widget build(BuildContext context) {
        SizedBox box = new SizedBox();
        box.width(36.0);
        box.height(36.0);
        return box;
    }
}
