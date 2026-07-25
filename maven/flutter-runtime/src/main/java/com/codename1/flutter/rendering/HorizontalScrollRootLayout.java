package com.codename1.flutter.rendering;

import com.codename1.flutter.RenderElement;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/**
 * {@link ScrollRootLayout}'s horizontal twin: the content is laid out with a
 * tight viewport HEIGHT and an unbounded horizontal main axis, and the content
 * extent is reported as the preferred size so CN1's tensile scrolling takes
 * over once the content is wider than the viewport.
 *
 * <p>Used by horizontally scrolling Flutter boundaries — {@code PageView}, and
 * {@code ListView(scrollDirection: Axis.horizontal)}.</p>
 */
public class HorizontalScrollRootLayout extends FlutterRootLayout {

    /**
     * The content height used by the last real layout pass. As in the vertical
     * case, getPreferredSize must measure at the SAME extent layoutContainer
     * used, or the two passes alternate and the UI bounces.
     */
    private int lastLayoutHeight = -1;

    public HorizontalScrollRootLayout(RenderHost host) {
        super(host);
    }

    @Override
    public void layoutContainer(Container parent) {
        RenderElement root = host().rootRenderElement();
        if (root == null) {
            return;
        }
        Style s = parent.getStyle();
        int height = parent.getHeight() - s.getVerticalPadding();
        if (height < 0) {
            height = 0;
        }
        lastLayoutHeight = height;
        root.layout(contentConstraints(height));
        root.position(s.getPaddingLeftNoRTL(), s.getPaddingTop());
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        RenderElement root = host().rootRenderElement();
        if (root == null) {
            return new Dimension(0, 0);
        }
        Style s = parent.getStyle();
        int height = lastLayoutHeight > 0 ? lastLayoutHeight : parent.getHeight() - s.getVerticalPadding();
        Size sz = root.layout(height > 0
                ? contentConstraints(height)
                : BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        int w = (int) Math.ceil(sz.width()) + s.getHorizontalPadding();
        int h = (int) Math.ceil(sz.height()) + s.getVerticalPadding();
        return new Dimension(w, h);
    }

    /**
     * Tight viewport height, unbounded width — the Flutter viewport contract
     * for a horizontal scrollable.
     */
    public static BoxConstraints contentConstraints(double height) {
        return new BoxConstraints(0, Double.POSITIVE_INFINITY, height, height);
    }
}
