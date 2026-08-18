package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * An iOS-style scrollbar wrapper — Flutter's {@code CupertinoScrollbar}. CN1
 * scrollables draw their own scrollbar, so this is a pass-through: it composes
 * its child directly.
 */
public class CupertinoScrollbar extends StatelessWidget {

    private Widget child;

    public void controller(Object v) {
    }

    public void thumbVisibility(boolean v) {
    }

    public void thickness(double v) {
    }

    public void thicknessWhileDragging(double v) {
    }

    public void radius(Object v) {
    }

    public void radiusWhileDragging(Object v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        // CN1's scrollables draw their own scrollbar, so wrapping one adds nothing. This
        // is a genuine pass-through rather than a missing feature.
        return child;
    }
}
