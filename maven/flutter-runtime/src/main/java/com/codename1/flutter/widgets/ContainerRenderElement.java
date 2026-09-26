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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.AlignmentDirectional;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;

/**
 * Flutter's RenderContainer composite. Layout order: margin deflates the
 * incoming constraints; explicit width/height and additional constraints
 * tighten the box; padding insets the child; alignment positions the child
 * within the padded box (and expands the box to the bounded axes when set).
 * A CN1 Container face (UIID "FlutterBox") paints the color/decoration behind
 * the child, covering the box minus the margin band.
 */
public class ContainerRenderElement extends SingleChildRenderElement {

    private EdgeInsets marginPx = EdgeInsets.all(0);
    private Size contentSize = Size.ZERO;

    public ContainerRenderElement(Container widget) {
        super(widget);
    }

    private Container container() {
        return (Container) widget();
    }

    @Override
    protected Widget childWidget() {
        return container().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            return null;
        }
        if (!FlutterBoxStyle.paints(container().getColor(), container().getDecoration())) {
            return null;
        }
        Container c = container();
        com.codename1.ui.Container face = new com.codename1.ui.Container();
        face.setUIID("FlutterBox");
        face.getAllStyles().setPadding(0, 0, 0, 0);
        face.getAllStyles().setMargin(0, 0, 0, 0);
        FlutterBoxStyle.apply(face, c.getColor(), c.getDecoration());
        return face;
    }

    @Override
    protected void updateComponent(Component c) {
        FlutterBoxStyle.apply(c, container().getColor(), container().getDecoration());
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Container w = container();
        marginPx = toPx(resolveInsets(w.getMargin()));
        EdgeInsets paddingPx = toPx(resolveInsets(w.getPadding()));

        BoxConstraints box = constraints.deflate(marginPx);
        if (w.getConstraints() != null) {
            box = additionalToPx(w.getConstraints()).enforce(box);
        }
        Double widthPx = w.getWidth() == null ? null : Double.valueOf(Dp.px(w.getWidth()));
        Double heightPx = w.getHeight() == null ? null : Double.valueOf(Dp.px(w.getHeight()));
        box = box.tighten(widthPx, heightPx);

        Alignment align = resolveAlignment(w.getAlignment());
        RenderElement child = renderChild();

        double contentW;
        double contentH;
        if (child == null) {
            contentW = box.hasBoundedWidth() ? box.maxWidth() : paddingPx.horizontal();
            contentH = box.hasBoundedHeight() ? box.maxHeight() : paddingPx.vertical();
            contentSize = box.constrain(new Size(contentW, contentH));
        } else {
            BoxConstraints inner = box.deflate(paddingPx);
            if (align != null) {
                inner = inner.loosen();
            }
            Size cs = child.layout(inner);
            contentW = cs.width() + paddingPx.horizontal();
            contentH = cs.height() + paddingPx.vertical();
            if (align != null) {
                if (box.hasBoundedWidth()) {
                    contentW = box.maxWidth();
                }
                if (box.hasBoundedHeight()) {
                    contentH = box.maxHeight();
                }
            }
            contentSize = box.constrain(new Size(contentW, contentH));
            double innerW = contentSize.width() - paddingPx.horizontal();
            double innerH = contentSize.height() - paddingPx.vertical();
            double dx = align == null ? 0 : Alignment.along(align.x(), innerW, cs.width());
            double dy = align == null ? 0 : Alignment.along(align.y(), innerH, cs.height());
            setChildOffset(child,
                    marginPx.left() + paddingPx.left() + dx,
                    marginPx.top() + paddingPx.top() + dy);
        }
        return constraints.constrain(new Size(
                contentSize.width() + marginPx.horizontal(),
                contentSize.height() + marginPx.vertical()));
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        Component face = component();
        if (face != null) {
            face.setX(x + (int) Math.round(marginPx.left()));
            face.setY(y + (int) Math.round(marginPx.top()));
            face.setWidth(Math.max(0, (int) Math.round(contentSize.width())));
            face.setHeight(Math.max(0, (int) Math.round(contentSize.height())));
        }
    }

    // ------------------------------------------------------------------

    private static EdgeInsets resolveInsets(Object o) {
        return o instanceof EdgeInsets ? (EdgeInsets) o : EdgeInsets.all(0);
    }

    private static EdgeInsets toPx(EdgeInsets lp) {
        return EdgeInsets.only(Dp.px(lp.left()), Dp.px(lp.top()), Dp.px(lp.right()), Dp.px(lp.bottom()));
    }

    private static Alignment resolveAlignment(Object o) {
        if (o instanceof Alignment) {
            return (Alignment) o;
        }
        if (o instanceof AlignmentDirectional) {
            return ((AlignmentDirectional) o).resolve();
        }
        return null;
    }

    private static BoxConstraints additionalToPx(BoxConstraints lp) {
        return new BoxConstraints(
                pxInf(lp.minWidth()), pxInf(lp.maxWidth()),
                pxInf(lp.minHeight()), pxInf(lp.maxHeight()));
    }

    private static double pxInf(double v) {
        return v == Double.POSITIVE_INFINITY ? v : Dp.px(v);
    }
}
