package com.codename1.flutter.rendering;

import com.codename1.flutter.RenderElement;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/**
 * The CN1 layout installed on the single flat container hosting a Flutter
 * subtree. CN1 sees one container whose children are the subtree's leaf
 * components (Labels, buttons ...); this layout runs the Flutter constraint
 * pass on the render-element tree and writes the resulting absolute bounds
 * onto those components.
 */
public class FlutterRootLayout extends Layout {

    private final RenderHost host;

    public FlutterRootLayout(RenderHost host) {
        this.host = host;
    }

    public RenderHost host() {
        return host;
    }

    @Override
    public void layoutContainer(Container parent) {
        RenderElement root = host.rootRenderElement();
        if (root == null) {
            return;
        }
        Style s = parent.getStyle();
        int width = parent.getLayoutWidth() - parent.getSideGap() - s.getHorizontalPadding();
        int height = parent.getLayoutHeight() - parent.getBottomGap() - s.getVerticalPadding();
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
        root.layout(BoxConstraints.tight(width, height));
        root.position(s.getPaddingLeftNoRTL(), s.getPaddingTop());
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        RenderElement root = host.rootRenderElement();
        if (root == null) {
            return new Dimension(0, 0);
        }
        // Dry pass with loose unbounded constraints. It goes through dryLayout, which keeps
        // its own cache slot: these constraints differ from layoutContainer's tight ones, so
        // sharing one slot made the two passes evict each other on every box in the tree.
        Size sz = root.dryLayout(BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Style s = parent.getStyle();
        int w = (int) Math.ceil(sz.width()) + s.getHorizontalPadding();
        int h = (int) Math.ceil(sz.height()) + s.getVerticalPadding();
        return new Dimension(w, h);
    }

    @Override
    public boolean isOverlapSupported() {
        return true;
    }
}
