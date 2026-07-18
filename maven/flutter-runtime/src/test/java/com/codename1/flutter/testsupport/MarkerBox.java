package com.codename1.flutter.testsupport;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * A leaf render widget that PRETENDS to own a CN1 component (without
 * instantiating one, which needs a Display) so the host's attach-order
 * bookkeeping — the flat container z-order — can be asserted headless.
 */
public class MarkerBox extends Widget {

    private final String tag;

    public MarkerBox(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    @Override
    public Element createElement() {
        return new MarkerBoxElement(this);
    }

    public static class MarkerBoxElement extends RenderElement {

        public MarkerBoxElement(MarkerBox widget) {
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
