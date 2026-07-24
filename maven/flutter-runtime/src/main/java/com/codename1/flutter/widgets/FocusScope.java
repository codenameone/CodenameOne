package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A focus container that groups its subtree into a focus scope — Flutter's
 * {@code FocusScope}. This milestone renders the {@code child} through
 * unchanged; scope-based focus traversal is deferred, so the node and focus
 * flags are captured only for API shape.
 */
public class FocusScope extends StatelessWidget {

    private FocusScopeNode node;
    private Widget child;

    public void node(FocusScopeNode v) {
        this.node = v;
    }

    public void autofocus(boolean v) {
    }

    public void onFocusChange(Object v) {
    }

    public void canRequestFocus(boolean v) {
    }

    public void skipTraversal(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    /** Dart's {@code FocusScope.of(context)} — the nearest enclosing scope node. */
    public static FocusScopeNode of(BuildContext context) {
        return new FocusScopeNode();
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
