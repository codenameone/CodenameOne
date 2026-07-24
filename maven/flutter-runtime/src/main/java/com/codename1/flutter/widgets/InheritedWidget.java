package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Flutter's InheritedWidget: a widget that exposes itself to descendants via
 * {@link BuildContext#dependOnInheritedWidgetOfExactType(Class)} and otherwise
 * renders its single {@code child}. Application subclasses (PageStatus,
 * LayoutCache, CodeStyle, ...) extend this and add their own fields; the lookup
 * is by runtime type, so no per-type wiring is required.
 *
 * <p>Rendered as a {@link StatelessWidget} whose {@code build} returns the
 * child; the element it produces sits in the tree as the discoverable ancestor.
 * {@code updateShouldNotify} is accepted for API shape (this pass does not
 * re-dispatch on inherited-widget change).</p>
 */
public class InheritedWidget extends StatelessWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return true;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
