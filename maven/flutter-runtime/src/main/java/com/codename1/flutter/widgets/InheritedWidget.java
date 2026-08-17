package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Flutter's InheritedWidget: a widget that exposes itself to descendants via
 * {@link BuildContext#dependOnInheritedWidgetOfExactType(Class)} and otherwise
 * renders its single {@code child}. Application subclasses (PageStatus,
 * LayoutCache, CodeStyle, ...) extend this and add their own fields; the lookup
 * is by runtime type, so no per-type wiring is required.
 *
 * <p>Rendered as a {@link StatelessWidget} whose {@code build} returns the child; the
 * {@link InheritedElement} it produces sits in the tree as the discoverable ancestor, and
 * is what remembers the descendants that read it so {@link #updateShouldNotify} can rebuild
 * them.</p>
 */
public class InheritedWidget extends StatelessWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    /**
     * Whether descendants that read this widget should rebuild — Flutter's
     * {@code updateShouldNotify}. Defaults to true: a rebuilt inherited widget usually
     * carries a new value, and a false negative is invisible (stale UI) where a false
     * positive only costs a rebuild.
     */
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return true;
    }

    @Override
    public Element createElement() {
        return new InheritedElement(this);
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
