package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A full-screen barrier that optionally dismisses a route when tapped —
 * Flutter's {@code ModalBarrier}. new_gallery's backdrop wraps one in a
 * {@code Listener} to intercept taps while the settings page is open. This
 * milestone renders an inert filled box; dismissal is wired by the enclosing
 * gesture handler.
 */
public class ModalBarrier extends StatelessWidget {

    private Color color;
    private boolean dismissible = true;

    public void color(Color v) {
        this.color = v;
    }

    public void dismissible(boolean v) {
        this.dismissible = v;
    }

    public void semanticsLabel(String v) {
    }

    public void barrierSemanticsDismissible(boolean v) {
    }

    public void onDismiss(Object v) {
    }

    public Color getColor() {
        return color;
    }

    public boolean isDismissible() {
        return dismissible;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
