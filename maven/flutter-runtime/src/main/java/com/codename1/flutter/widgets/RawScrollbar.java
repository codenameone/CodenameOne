package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A scrollbar with no theme defaults wrapping a scrollable — Flutter's
 * {@code RawScrollbar}. This milestone renders the {@code child}; the scrollbar
 * track/thumb overlay is deferred (CN1 scrollables draw their own indicator).
 */
public class RawScrollbar extends StatelessWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public void controller(Object v) {
    }

    public void thumbVisibility(boolean v) {
    }

    public void thumbColor(Object v) {
    }

    public void radius(Object v) {
    }

    public void thickness(double v) {
    }

    public void interactive(boolean v) {
    }

    public void notificationPredicate(Object v) {
    }

    public void scrollbarOrientation(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
