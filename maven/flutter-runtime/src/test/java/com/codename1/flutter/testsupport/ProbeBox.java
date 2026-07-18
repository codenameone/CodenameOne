package com.codename1.flutter.testsupport;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * A leaf render widget with a stubbed intrinsic size — no CN1 component, no
 * font measurement — so layout and reconciliation can run headless.
 */
public class ProbeBox extends Widget {

    private final double width;
    private final double height;

    public ProbeBox(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    @Override
    public Element createElement() {
        return new ProbeBoxElement(this);
    }

    public static class ProbeBoxElement extends RenderElement {
        public int layoutCount;

        public ProbeBoxElement(ProbeBox widget) {
            super(widget);
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            layoutCount++;
            ProbeBox w = (ProbeBox) widget();
            return constraints.constrain(new Size(w.width(), w.height()));
        }
    }
}
