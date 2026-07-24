package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.SizedBox;
import com.codename1.flutter.widgets.Text;

import dart.core.DartList;

/**
 * A single tab label for a {@link TabBar} — Flutter's {@code Tab}. Composes its
 * {@code text} (and/or {@code icon}) as the visible content; an explicit
 * {@code child} overrides both.
 */
public class Tab extends StatelessWidget {

    private String text;
    private Widget icon;
    private Object iconMargin;
    private Double height;
    private Widget child;

    public void text(String v) {
        this.text = v;
    }

    public void icon(Widget v) {
        this.icon = v;
    }

    public void iconMargin(Object v) {
        this.iconMargin = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        if (child != null) {
            return child;
        }
        Widget label = text != null ? new Text(text) : null;
        if (icon != null && label != null) {
            DartList<Widget> kids = new DartList<Widget>();
            kids.add(icon);
            kids.add(label);
            Column col = new Column();
            col.children(kids);
            return col;
        }
        if (icon != null) {
            return icon;
        }
        if (label != null) {
            return label;
        }
        return new SizedBox();
    }
}
