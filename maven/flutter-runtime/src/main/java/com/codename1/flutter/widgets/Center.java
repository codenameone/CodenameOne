package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Centers its child within itself. Expands to the incoming constraints when
 * they are bounded, otherwise sizes to the child.
 */
public class Center extends Widget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new CenterRenderElement(this);
    }
}
