package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Establishes a restoration namespace for its subtree — Flutter's
 * {@code RestorationScope}. Structural pass-through for this milestone: the
 * {@code child} renders unchanged and the {@code restorationId} is captured.
 */
public class RestorationScope extends Widget implements HasChild {

    private String restorationId;
    private Widget child;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /** Flutter's {@code RestorationScope.of} — no ambient bucket at this pass. */
    public static Object of(BuildContext context) {
        return null;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
