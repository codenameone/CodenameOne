package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.EdgeInsetsGeometry;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Insets its child by the given edge padding (logical pixels).
 */
public class Padding extends Widget {

    private EdgeInsets padding;
    private Widget child;

    /**
     * Flutter's {@code Padding.padding} is an {@code EdgeInsetsGeometry}; the render pass needs
     * the resolved {@link EdgeInsets}, so a direction-relative inset (never used by these
     * layouts) is dropped rather than resolved here.
     */
    public void padding(EdgeInsetsGeometry v) {
        this.padding = (v instanceof EdgeInsets) ? (EdgeInsets) v : null;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PaddingRenderElement(this);
    }
}
