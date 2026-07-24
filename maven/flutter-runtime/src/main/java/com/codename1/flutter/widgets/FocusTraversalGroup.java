package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Groups its descendants into a single focus-traversal scope with an optional
 * {@code policy} — Flutter's {@code FocusTraversalGroup}. This pass renders the
 * {@code child} through unchanged; directional/reading-order traversal is
 * deferred, so the policy is captured only for API shape.
 */
public class FocusTraversalGroup extends StatelessWidget {

    private Object policy;
    private Widget child;

    public void policy(Object v) {
        this.policy = v;
    }

    public void descendantsAreFocusable(boolean v) {
    }

    public void descendantsAreTraversable(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
