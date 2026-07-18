package com.codename1.flutter.testsupport;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * A second component-owning marker widget type, so reconciliation replaces a
 * {@link MarkerBox} element instead of updating it in place.
 */
public class AltMarkerBox extends Widget {

    private final String tag;

    public AltMarkerBox(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    @Override
    public Element createElement() {
        return new AltMarkerBoxElement(this);
    }

    public static class AltMarkerBoxElement extends RenderElement {

        public AltMarkerBoxElement(AltMarkerBox widget) {
            super(widget);
        }

        @Override
        protected boolean ownsComponent() {
            return true;
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            return constraints.constrain(new Size(10, 10));
        }
    }
}
