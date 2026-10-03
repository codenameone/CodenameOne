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

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.RoundRectBorder;

/**
 * Render element for {@link Card}: a Container (UIID "FlutterCard") styled
 * programmatically with a 12lp-corner round-rect border and a subtle shadow
 * scaled from the elevation (best effort on CN1's shadow model). Layout is
 * padding-like: the child is inset by the margin (default 4lp) and the card
 * face component covers the element bounds minus the margin band — so the
 * child's components, attached after the face in tree order, paint on top of
 * it.
 */
public class CardRenderElement extends SingleChildRenderElement {

    /** Flutter's default card margin in logical pixels. */
    public static final double DEFAULT_MARGIN_LP = 4;
    /** Material 3 card corner radius in logical pixels. */
    public static final double CORNER_LP = 12;

    private EdgeInsets marginPx = EdgeInsets.all(0);

    public CardRenderElement(Card widget) {
        super(widget);
    }

    private Card card() {
        return (Card) widget();
    }

    @Override
    protected Widget childWidget() {
        return card().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Container face = new Container();
        face.setUIID("FlutterCard");
        face.getAllStyles().setPadding(0, 0, 0, 0);
        face.getAllStyles().setMargin(0, 0, 0, 0);
        applyStyle(face);
        return face;
    }

    @Override
    protected void updateComponent(Component c) {
        applyStyle(c);
    }

    /// The inputs the current border was built from, so a rebuild that changes
    /// neither reuses it.
    ///
    /// RoundRectBorder caches its rendered shadow against a client property keyed
    /// by the BORDER INSTANCE, so handing the component a new instance on every
    /// rebuild misses that cache every time -- and a miss on a shadowed border is
    /// a full offscreen render plus a gaussian blur, plus one more client
    /// property that is never read again. Rebuilding the style is cheap; its
    /// border is not.
    private int styledBg = -1;
    private double styledElevation = -1;
    private com.codename1.ui.plaf.Border styledBorder;

    private void applyStyle(Component face) {
        try {
            int bg = card().getColor() != null
                    ? card().getColor().rgb()
                    : Theme.of(this).colorScheme().surface().rgb();
            double elevation = card().getElevation() != null ? card().getElevation() : 1;
            if (styledBorder != null && styledBg == bg && styledElevation == elevation) {
                face.getAllStyles().setBgColor(bg);
                face.getAllStyles().setBgTransparency(255);
                face.getAllStyles().setBorder(styledBorder);
                return;
            }
            // useCache(true) is NOT a performance choice, it is a correctness one.
            //
            // With the cache off, RoundRectBorder renders through
            // createTargetComponentImage, which translates the Graphics by the
            // shadow's shape offset and returns without undoing it. That
            // Graphics is the live one, so the offset leaks into everything
            // painted after this card: the cards demo drifted 9 device pixels
            // down and 4 across per card, accumulating down the page, while the
            // LAYOUT was exactly the reference's -- the component tree reported
            // the right boxes and the pixels were somewhere else. The cached
            // path renders into an offscreen image of its own, so the same
            // unbalanced translate is discarded with it.
            RoundRectBorder border = RoundRectBorder.create()
                    .useCache(true)
                    .cornerRadius(Dp.mm(CORNER_LP));
            if (elevation > 0) {
                border = border
                        .shadowOpacity(Math.min(255, (int) Math.round(20 + elevation * 15)))
                        .shadowSpread((float) Math.min(3, 0.25f + elevation * 0.25f))
                        .shadowY(1);
            }
            face.getAllStyles().setBgColor(bg);
            face.getAllStyles().setBgTransparency(255);
            face.getAllStyles().setBorder(border);
            styledBg = bg;
            styledElevation = elevation;
            styledBorder = border;
        } catch (Exception err) {
            // styling is best-effort; layout must survive regardless
        }
    }

    private EdgeInsets marginLp() {
        EdgeInsets m = card().getMargin();
        return m == null ? EdgeInsets.all(DEFAULT_MARGIN_LP) : m;
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        EdgeInsets lp = marginLp();
        marginPx = EdgeInsets.only(Dp.px(lp.left()), Dp.px(lp.top()), Dp.px(lp.right()), Dp.px(lp.bottom()));
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(marginPx.horizontal(), marginPx.vertical()));
        }
        Size cs = child.layout(constraints.deflate(marginPx));
        setChildOffset(child, marginPx.left(), marginPx.top());
        return constraints.constrain(new Size(
                cs.width() + marginPx.horizontal(), cs.height() + marginPx.vertical()));
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        Component face = component();
        if (face != null) {
            // shrink the card face to exclude the margin band
            face.setX(x + (int) Math.round(marginPx.left()));
            face.setY(y + (int) Math.round(marginPx.top()));
            face.setWidth(Math.max(0, (int) Math.round(size().width() - marginPx.horizontal())));
            face.setHeight(Math.max(0, (int) Math.round(size().height() - marginPx.vertical())));
        }
    }
}
