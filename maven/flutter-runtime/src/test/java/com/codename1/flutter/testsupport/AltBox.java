package com.codename1.flutter.testsupport;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * A second leaf render widget type, used to verify that reconciliation
 * replaces the element when the widget type changes.
 */
public class AltBox extends Widget {

    private final double width;
    private final double height;

    public AltBox(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Element createElement() {
        return new AltBoxElement(this);
    }

    public static class AltBoxElement extends RenderElement {
        public AltBoxElement(AltBox widget) {
            super(widget);
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            AltBox w = (AltBox) widget();
            return constraints.constrain(new Size(w.width, w.height));
        }
    }
}
