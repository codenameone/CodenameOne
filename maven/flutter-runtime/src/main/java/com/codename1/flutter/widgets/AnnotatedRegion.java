package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Annotates the layer tree with a value (e.g. a {@code SystemUiOverlayStyle})
 * over the region its child occupies. The value is retained but not yet
 * applied; the child renders unchanged. See {@link PassThroughRenderElement}.
 *
 * @param <T> the annotation value type (e.g. SystemUiOverlayStyle)
 */
public class AnnotatedRegion<T> extends Widget implements HasChild {

    private Widget child;
    private Object value;
    private boolean sized = true;

    public void child(Widget v) {
        this.child = v;
    }

    public void value(Object v) {
        this.value = v;
    }

    public void sized(boolean v) {
        this.sized = v;
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
