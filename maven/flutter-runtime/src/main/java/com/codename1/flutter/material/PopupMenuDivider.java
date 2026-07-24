package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.HasChild;
import com.codename1.flutter.widgets.PassThroughRenderElement;

/**
 * A horizontal divider between entries in a {@link PopupMenuButton}'s menu —
 * Flutter's {@code PopupMenuDivider}. Rendered as a {@link Divider}.
 */
public class PopupMenuDivider extends PopupMenuEntry<Object> implements HasChild {

    private double height = 16;
    private final Divider divider = new Divider();

    public void height(double v) {
        this.height = v;
    }

    @Override
    public Widget getChild() {
        return divider;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
