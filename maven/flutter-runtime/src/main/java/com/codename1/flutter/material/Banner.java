package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A diagonal ribbon banner drawn over a corner of its child — Flutter's
 * {@code Banner}. This milestone renders the {@code child}; the diagonal
 * {@code message} ribbon is deferred.
 */
public class Banner extends StatelessWidget {

    private Widget child;
    private String message;
    private Object location;
    private Color color;
    private TextStyle textStyle;

    public void child(Widget v) {
        this.child = v;
    }

    public void message(String v) {
        this.message = v;
    }

    public void textDirection(Object v) {
    }

    public void location(Object v) {
        this.location = v;
    }

    public void layoutDirection(Object v) {
    }

    public void color(Color v) {
        this.color = v;
    }

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
