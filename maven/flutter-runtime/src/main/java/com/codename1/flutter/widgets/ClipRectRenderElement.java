package com.codename1.flutter.widgets;

import com.codename1.flutter.Clip;
import com.codename1.flutter.FlutterErrorReport;
import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Clips its subtree to its own bounds, which is what makes an expand/collapse animation
 * read as one: {@code ClipRect(child: Align(heightFactor: t, child: ...))} shrinks the box
 * while the child keeps its full size, and everything past the box has to disappear.
 *
 * <p>The clip itself costs nothing to apply. Codename One already confines a component's
 * paint - its own and its children's - to its bounds before calling
 * {@code paint} (see {@code Component.internalPaintImpl}). The work is getting the subtree
 * to be that component's children at all: the render tree is otherwise flat, every element
 * absolutely positioned as a sibling in one host. {@link EffectRenderElement} supplies the
 * nested pane that makes the subtree genuinely nested, so this element only has to exist,
 * not to paint anything special.</p>
 */
public class ClipRectRenderElement extends EffectRenderElement {

    private boolean reportedPassThrough;

    public ClipRectRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected Widget effectChild() {
        return ((HasChild) widget()).getChild();
    }

    /** The clip mode this widget asks for, defaulting to a hard edge as Flutter's ClipRect does. */
    private Clip behavior() {
        Widget w = widget();
        Clip c = w instanceof ClipRect ? ((ClipRect) w).getClipBehavior() : null;
        return c == null ? Clip.hardEdge : c;
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Runnable paintChildren) {
        if (behavior() == Clip.none && !reportedPassThrough) {
            // Clip.none asks for NO clipping, and the nested pane clips regardless - so say
            // so rather than quietly cutting content the caller expected to overflow.
            reportedPassThrough = true;
            FlutterErrorReport.unimplemented("ClipRect", "clipBehavior: Clip.none still clips to the bounds");
        }
        paintChildren.run();
    }
}
