package com.codename1.flutter.widgets;

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Widget;

/**
 * Element for {@link Builder}: rebuilding invokes the builder closure with this
 * element as the {@link com.codename1.flutter.BuildContext} and reconciles the
 * single resulting child.
 */
public class BuilderElement extends ComposedElement {

    public BuilderElement(Builder widget) {
        super(widget);
    }

    @Override
    protected Widget build() {
        return ((Builder) widget()).getBuilder().call(this);
    }
}
