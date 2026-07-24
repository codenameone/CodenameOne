package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

/**
 * A material bottom app bar: a container docked to the bottom of a
 * {@link Scaffold}, typically hosting a row of actions and (with a
 * {@code shape}) a notch for a docked FloatingActionButton. This milestone
 * renders it as its {@code child} on a colored surface; the notch geometry is
 * retained as configuration but not yet cut.
 */
public class BottomAppBar extends StatelessWidget {

    private Color color;
    private Double elevation;
    private Object shape;
    private Double notchMargin;
    private Clip clipBehavior;
    private Widget child;

    public void color(Color v) {
        this.color = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void notchMargin(double v) {
        this.notchMargin = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Color getColor() {
        return color;
    }

    public Double getElevation() {
        return elevation;
    }

    public Object getShape() {
        return shape;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        Container c = new Container();
        if (color != null) {
            c.color(color);
        }
        c.child(child);
        return c;
    }
}
