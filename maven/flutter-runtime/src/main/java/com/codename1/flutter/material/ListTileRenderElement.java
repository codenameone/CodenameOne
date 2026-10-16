/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

import dart.runtime.Funcs;

/**
 * Composite render element for {@link ListTile}. Owns a background Container
 * (UIID "FlutterListTile", attached before the children so it paints behind
 * them) and mounts leading / title / subtitle / trailing subtrees plus a
 * synthesized transparent tap overlay LAST (so it sits on top for pointer
 * dispatch, like GestureDetector's overlay).
 *
 * <p>Material geometry: 16lp horizontal padding, 16lp gaps between the
 * leading / text block / trailing sections, title stacked above subtitle,
 * leading and trailing vertically centered, 56lp minimum height (8lp
 * vertical padding when the content is taller).</p>
 */
public class ListTileRenderElement extends RenderElement {

    /** Material list-tile minimum height in logical pixels, for a tile with no subtitle. */
    public static final double MIN_HEIGHT_LP = 56;

    /// Material's height for a tile that has a subtitle.
    ///
    /// A list tile's height comes from how many LINES it has, not from how big the
    /// things inside it are, and in particular the leading widget never drives it -- it
    /// is centred in the tile and is allowed to overflow. Letting it drive the height
    /// made every tile as tall as its own avatar: Crane's destination list carries a
    /// 60lp thumbnail, which with the vertical padding came to 76lp where Material's
    /// two-line tile is 72lp, so each row ran 4lp long and the rows below it drifted
    /// further out of register with every one that was added -- 43 device pixels by the
    /// fourth row.
    public static final double TWO_LINE_HEIGHT_LP = 72;
    /** Horizontal padding in logical pixels. */
    public static final double HPAD_LP = 16;
    /** Gap between sections in logical pixels. */
    public static final double GAP_LP = 16;
    /** Vertical padding applied when the content overflows 56lp. */
    public static final double VPAD_LP = 8;

    private Element leadingChild;
    private Element titleChild;
    private Element subtitleChild;
    private Element trailingChild;
    private Element overlayChild;

    public ListTileRenderElement(ListTile widget) {
        super(widget);
    }

    private ListTile tile() {
        return (ListTile) widget();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Container c = new Container();
        c.setUIID("FlutterListTile");
        c.getAllStyles().setPadding(0, 0, 0, 0);
        c.getAllStyles().setMargin(0, 0, 0, 0);
        return c;
    }

    @Override
    protected void syncChildren() {
        leadingChild = updateChild(leadingChild, tile().getLeading(), 0);
        titleChild = updateChild(titleChild, tile().getTitle(), 1);
        subtitleChild = updateChild(subtitleChild, tile().getSubtitle(), 2);
        trailingChild = updateChild(trailingChild, tile().getTrailing(), 3);
        overlayChild = updateChild(overlayChild, new TileOverlay(), 4);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (leadingChild != null) {
            visitor.call(leadingChild);
        }
        if (titleChild != null) {
            visitor.call(titleChild);
        }
        if (subtitleChild != null) {
            visitor.call(subtitleChild);
        }
        if (trailingChild != null) {
            visitor.call(trailingChild);
        }
        if (overlayChild != null) {
            visitor.call(overlayChild);
        }
    }

    void fireTap() {
        Funcs.VoidFunc0 f = tile().getOnTap();
        if (f != null) {
            f.call();
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double hpad = Dp.px(HPAD_LP);
        double gap = Dp.px(GAP_LP);
        double vpad = Dp.px(VPAD_LP);

        RenderElement leading = RenderElement.findRenderElement(leadingChild);
        RenderElement title = RenderElement.findRenderElement(titleChild);
        RenderElement subtitle = RenderElement.findRenderElement(subtitleChild);
        RenderElement trailing = RenderElement.findRenderElement(trailingChild);
        RenderElement overlay = RenderElement.findRenderElement(overlayChild);

        BoxConstraints loose = new BoxConstraints(
                0, constraints.hasBoundedWidth() ? constraints.maxWidth() : Double.POSITIVE_INFINITY,
                0, Double.POSITIVE_INFINITY);

        Size leadingSize = leading != null ? leading.layout(loose) : Size.ZERO;
        Size trailingSize = trailing != null ? trailing.layout(loose) : Size.ZERO;

        double sideWidth = hpad * 2
                + (leading != null ? leadingSize.width() + gap : 0)
                + (trailing != null ? trailingSize.width() + gap : 0);

        BoxConstraints textConstraints;
        if (constraints.hasBoundedWidth()) {
            double avail = Math.max(0, constraints.maxWidth() - sideWidth);
            textConstraints = new BoxConstraints(0, avail, 0, Double.POSITIVE_INFINITY);
        } else {
            textConstraints = loose;
        }
        Size titleSize = title != null ? title.layout(textConstraints) : Size.ZERO;
        Size subtitleSize = subtitle != null ? subtitle.layout(textConstraints) : Size.ZERO;

        double textW = Math.max(titleSize.width(), subtitleSize.width());
        double textH = titleSize.height() + subtitleSize.height();

        double width = constraints.hasBoundedWidth()
                ? constraints.maxWidth()
                : sideWidth + textW;
        // The TEXT may push a tile past its nominal height; the leading and trailing may
        // not. Material fixes the height by line count and centres the other two inside
        // it, overflowing them if it must.
        double nominal = Dp.px(subtitle != null ? TWO_LINE_HEIGHT_LP : MIN_HEIGHT_LP);
        double height = constraints.constrainHeight(
                Math.max(nominal, textH + vpad * 2));

        if (leading != null) {
            setChildOffset(leading, hpad, (height - leadingSize.height()) / 2);
        }
        double textX = hpad + (leading != null ? leadingSize.width() + gap : 0);
        double textY = (height - textH) / 2;
        if (title != null) {
            setChildOffset(title, textX, textY);
        }
        if (subtitle != null) {
            setChildOffset(subtitle, textX, textY + titleSize.height());
        }
        if (trailing != null) {
            setChildOffset(trailing, width - hpad - trailingSize.width(),
                    (height - trailingSize.height()) / 2);
        }
        if (overlay != null) {
            overlay.layout(BoxConstraints.tight(width, height));
            setChildOffset(overlay, 0, 0);
        }
        return constraints.constrain(new Size(width, height));
    }

    // ------------------------------------------------------------------
    // Tap overlay (synthesized, mounts after the visible children)
    // ------------------------------------------------------------------

    static class TileOverlay extends Widget {
        @Override
        public Element createElement() {
            return new TileOverlayElement(this);
        }
    }

    static class TileOverlayElement extends RenderElement {

        TileOverlayElement(TileOverlay widget) {
            super(widget);
        }

        private ListTileRenderElement tileElement() {
            Element p = parent();
            return p instanceof ListTileRenderElement ? (ListTileRenderElement) p : null;
        }

        @Override
        protected Component createComponent() {
            if (!Display.isInitialized()) {
                // headless unit tests: no CN1 components can exist
                return null;
            }
            return new OverlayComponent();
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            // the parent hands us tight constraints matching the tile bounds
            return constraints.smallest();
        }

        class OverlayComponent extends Component {

            OverlayComponent() {
                setUIID("FlutterGesture");
                setGrabsPointerEvents(true);
                setFocusable(false);
                getAllStyles().setBgTransparency(0);
                getAllStyles().setPadding(0, 0, 0, 0);
                getAllStyles().setMargin(0, 0, 0, 0);
            }

            @Override
            public void paint(Graphics g) {
                // paints nothing — pure hit area
            }

            @Override
            public void pointerReleased(int x, int y) {
                boolean wasDrag = isDragActivated();
                super.pointerReleased(x, y);
                if (!wasDrag && contains(x, y)) {
                    ListTileRenderElement t = tileElement();
                    if (t != null) {
                        t.fireTap();
                    }
                }
            }
        }
    }
}
