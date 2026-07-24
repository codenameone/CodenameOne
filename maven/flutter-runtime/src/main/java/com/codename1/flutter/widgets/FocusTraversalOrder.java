package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Assigns an explicit traversal {@code order} (a {@link FocusOrder}, e.g.
 * {@link NumericFocusOrder}) to its {@code child} within the enclosing
 * {@code FocusTraversalGroup} — Flutter's {@code FocusTraversalOrder}. This
 * pass hosts the child; ordered traversal is deferred, so the order is captured
 * for API shape only.
 */
public class FocusTraversalOrder extends StatelessWidget {

    private FocusOrder order;
    private Widget child;

    public void order(FocusOrder v) {
        this.order = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public FocusOrder getOrder() {
        return order;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
