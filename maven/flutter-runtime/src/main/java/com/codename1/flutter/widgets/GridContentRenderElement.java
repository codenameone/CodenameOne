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

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid layout math for {@link GridView}: rows of {@code crossAxisCount}
 * tight cells; cell width is the available width divided by the count (minus
 * cross-axis spacing), cell height is {@code cellWidth / childAspectRatio}
 * (default ratio 1.0). Owns no CN1 component. An unbounded width (a vertical
 * grid needs a bounded cross axis) falls back to 100lp cells.
 */
public class GridContentRenderElement extends RenderElement {

    private static final double FALLBACK_CELL_LP = 100;

    private List<Element> children = new ArrayList<Element>();

    public GridContentRenderElement(GridContent widget) {
        super(widget);
    }

    private GridContent grid() {
        return (GridContent) widget();
    }

    @Override
    protected void syncChildren() {
        List<Widget> newWidgets = new ArrayList<Widget>();
        if (grid().getChildren() != null) {
            for (Widget w : grid().getChildren()) {
                if (w != null) {
                    newWidgets.add(w);
                }
            }
        }
        children = updateChildren(children, newWidgets);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        for (Element c : children) {
            if (c != null) {
                visitor.call(c);
            }
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        long n = Math.max(1, grid().getCrossAxisCount());
        double ratio = grid().getChildAspectRatio() == null ? 1.0 : grid().getChildAspectRatio();
        if (ratio <= 0) {
            ratio = 1.0;
        }
        double mainSp = grid().getMainAxisSpacing() == null ? 0 : Dp.px(grid().getMainAxisSpacing());
        double crossSp = grid().getCrossAxisSpacing() == null ? 0 : Dp.px(grid().getCrossAxisSpacing());

        if (grid().isHorizontal()) {
            return layoutHorizontal(constraints, n, ratio, mainSp, crossSp);
        }
        double width = constraints.hasBoundedWidth()
                ? constraints.maxWidth()
                : n * Dp.px(FALLBACK_CELL_LP) + (n - 1) * crossSp;
        double cellW = Math.max(0, (width - (n - 1) * crossSp) / n);
        double cellH = cellW / ratio;

        List<RenderElement> kids = renderChildren();
        int count = kids.size();
        for (int i = 0; i < count; i++) {
            RenderElement kid = kids.get(i);
            long row = i / n;
            long col = i % n;
            kid.layout(BoxConstraints.tight(cellW, cellH));
            setChildOffset(kid, col * (cellW + crossSp), row * (cellH + mainSp));
        }
        long rows = (count + n - 1) / n;
        double height = rows == 0 ? 0 : rows * cellH + (rows - 1) * mainSp;
        return constraints.constrain(new Size(width, height));
    }

    /// The horizontal grid: crossAxisCount rows, the cells filling each column top
    /// to bottom before the next column starts; childAspectRatio is still cross over
    /// main extent, so a cell is its height divided by the ratio wide.
    private Size layoutHorizontal(BoxConstraints constraints, long n, double ratio,
                                  double mainSp, double crossSp) {
        double height = constraints.hasBoundedHeight()
                ? constraints.maxHeight()
                : n * Dp.px(FALLBACK_CELL_LP) + (n - 1) * crossSp;
        double cellH = Math.max(0, (height - (n - 1) * crossSp) / n);
        double cellW = cellH / ratio;
        List<RenderElement> kids = renderChildren();
        int count = kids.size();
        for (int i = 0; i < count; i++) {
            RenderElement kid = kids.get(i);
            long col = i / n;
            long row = i % n;
            kid.layout(BoxConstraints.tight(cellW, cellH));
            setChildOffset(kid, col * (cellW + mainSp), row * (cellH + crossSp));
        }
        long cols = (count + n - 1) / n;
        double width = cols == 0 ? 0 : cols * cellW + (cols - 1) * mainSp;
        return constraints.constrain(new Size(width, height));
    }
}
