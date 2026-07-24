package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Size;

/**
 * Adapts an arbitrary {@code child} into a {@link PreferredSizeWidget} of a
 * given size — Flutter's {@code PreferredSize}. Used as the {@code bottom} of an
 * app bar so the bar reserves {@code preferredSize.height}. Structural
 * pass-through for this milestone: the {@code child} renders unchanged.
 */
public class PreferredSize extends Widget implements HasChild, PreferredSizeWidget {

    private Size preferredSize;
    private Widget child;

    public void preferredSize(Size v) {
        this.preferredSize = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Size preferredSize() {
        return preferredSize;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
