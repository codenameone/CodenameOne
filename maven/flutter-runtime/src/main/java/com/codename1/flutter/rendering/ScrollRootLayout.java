package com.codename1.flutter.rendering;

import com.codename1.flutter.RenderElement;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/**
 * The CN1 layout installed on a scrollable Flutter boundary container (the
 * pane owned by SingleChildScrollView/ListView/GridView). Unlike
 * {@link FlutterRootLayout} — which forces the subtree into the container's
 * exact bounds — this lays the content out with a tight cross axis (the
 * viewport width) and an unbounded vertical main axis, and reports the
 * content extent as the preferred size so CN1's own tensile scrolling takes
 * over when the content is taller than the viewport.
 */
public class ScrollRootLayout extends FlutterRootLayout {

    /**
     * The content width used by the last real layout pass. getPreferredSize
     * must lay content out at the SAME width layoutContainer used (it can't
     * compute the side gap itself — see the recursion note below); otherwise
     * the two passes alternate between different widths and the UI "bounces"
     * whenever CN1 interleaves scroll-size and layout computations.
     */
    private int lastLayoutWidth = -1;

    public ScrollRootLayout(RenderHost host) {
        super(host);
    }

    @Override
    public void layoutContainer(Container parent) {
        RenderElement root = host().rootRenderElement();
        if (root == null) {
            return;
        }
        Style s = parent.getStyle();
        int width = parent.getLayoutWidth() - parent.getSideGap() - s.getHorizontalPadding();
        if (width < 0) {
            width = 0;
        }
        lastLayoutWidth = width;
        root.layout(contentConstraints(width));
        root.position(s.getPaddingLeftNoRTL(), s.getPaddingTop());
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        RenderElement root = host().rootRenderElement();
        if (root == null) {
            return new Dimension(0, 0);
        }
        Style s = parent.getStyle();
        // Width preference order:
        //  1. the width the real layout pass used (keeps both passes
        //     consistent — inconsistent widths make the UI bounce);
        //  2. Component.getWidth() — NOT Container.getLayoutWidth(), which
        //     falls back to getPreferredW() pre-layout and re-enters this
        //     method; likewise getSideGap() routes through isScrollableY ->
        //     getScrollDimension -> calcPreferredSize and recurses.
        int width = lastLayoutWidth > 0 ? lastLayoutWidth : parent.getWidth() - s.getHorizontalPadding();
        Size sz = root.layout(width > 0
                ? contentConstraints(width)
                : BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        int w = (int) Math.ceil(sz.width()) + s.getHorizontalPadding();
        int h = (int) Math.ceil(sz.height()) + s.getVerticalPadding();
        return new Dimension(w, h);
    }

    /**
     * Tight viewport width, unbounded height — the Flutter viewport contract
     * for a vertical scrollable.
     */
    public static BoxConstraints contentConstraints(double width) {
        return new BoxConstraints(width, width, 0, Double.POSITIVE_INFINITY);
    }
}
