package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import dart.runtime.Funcs;

/**
 * Render element for {@link GestureDetector} (and material InkWell). Owns no
 * component itself: it mounts the child at slot 0 and a synthesized
 * {@link GestureOverlay} at slot 1 whose transparent component covers the
 * child's bounds. Slot order puts the overlay's component AFTER the child
 * subtree in the flat container, so it sits on top for pointer dispatch.
 */
public class GestureRenderElement extends RenderElement {

    private Element childElement;
    private Element overlayElement;

    public GestureRenderElement(GestureDetector widget) {
        super(widget);
    }

    GestureDetector gesture() {
        return (GestureDetector) widget();
    }

    /** The wrapped content (slot 0) — the subtree the overlay must not shadow. */
    Element contentElement() {
        return childElement;
    }

    @Override
    protected void syncChildren() {
        childElement = updateChild(childElement, gesture().getChild(), 0);
        overlayElement = updateChild(overlayElement, new GestureOverlay(), 1);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (childElement != null) {
            visitor.call(childElement);
        }
        if (overlayElement != null) {
            visitor.call(overlayElement);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = findRenderElement(childElement);
        Size cs;
        if (child != null) {
            cs = child.layout(constraints);
            setChildOffset(child, 0, 0);
        } else {
            cs = constraints.smallest();
        }
        RenderElement overlay = findRenderElement(overlayElement);
        if (overlay != null) {
            overlay.layout(BoxConstraints.tight(cs.width(), cs.height()));
            setChildOffset(overlay, 0, 0);
        }
        return constraints.constrain(cs);
    }
}
