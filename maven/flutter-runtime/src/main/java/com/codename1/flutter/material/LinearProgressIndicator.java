package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.ColoredBox;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A horizontal material progress bar — Flutter's {@code LinearProgressIndicator}.
 * This milestone renders a thin 4lp bar filled with the indicator {@code color}
 * (a determinate {@code value} is accepted but the fill fraction and the
 * indeterminate sweep animation are deferred).
 */
public class LinearProgressIndicator extends StatelessWidget {

    private Double value;
    private Color color;
    private Color backgroundColor;
    private Double minHeight;

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

    public void minHeight(double v) {
        this.minHeight = v;
    }

    public void semanticsLabel(String v) {
    }

    public void semanticsValue(String v) {
    }

    public void borderRadius(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        SizedBox box = new SizedBox();
        box.height(minHeight != null ? minHeight : 4.0);
        Color fill = color != null ? color : backgroundColor;
        if (fill != null) {
            ColoredBox cb = new ColoredBox();
            cb.color(fill);
            box.child(cb);
        }
        return box;
    }
}
