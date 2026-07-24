package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.ColoredBox;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A thin vertical line, the vertical sibling of {@link Divider} — Flutter's
 * {@code VerticalDivider}. Occupies {@code width} horizontally and paints a
 * line {@code thickness} wide in {@code color}. This pass renders a full-height
 * box of the given width, filled when a color is supplied.
 */
public class VerticalDivider extends StatelessWidget {

    private Double width;
    private Double thickness;
    private Color color;

    public void width(double v) {
        this.width = v;
    }

    public void thickness(double v) {
        this.thickness = v;
    }

    public void indent(double v) {
    }

    public void endIndent(double v) {
    }

    public void color(Color v) {
        this.color = v;
    }

    @Override
    public Widget build(BuildContext context) {
        SizedBox box = new SizedBox();
        box.width(width != null ? width : 16.0);
        if (color != null) {
            ColoredBox cb = new ColoredBox();
            cb.color(color);
            box.child(cb);
        }
        return box;
    }
}
