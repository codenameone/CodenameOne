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

import com.codename1.flutter.Axis;
import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.WrapCrossAlignment;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flutter's RenderWrap: children are laid out with loose constraints and
 * greedily packed into runs along the main axis (wrapping when a run would
 * overflow the bounded main extent), runs stacked along the cross axis with
 * {@code runSpacing}, items separated by {@code spacing}. Main-axis alignment
 * supports start/center/end (space* variants fall back to start);
 * cross-axis-within-run alignment supports start/center/end. Owns no CN1
 * component.
 */
public class WrapRenderElement extends RenderElement {

    private List<Element> children = new ArrayList<Element>();

    public WrapRenderElement(Wrap widget) {
        super(widget);
    }

    private Wrap wrap() {
        return (Wrap) widget();
    }

    @Override
    protected void syncChildren() {
        List<Widget> newWidgets = new ArrayList<Widget>();
        if (wrap().getChildren() != null) {
            for (Widget w : wrap().getChildren()) {
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
        boolean horizontal = wrap().getDirection() == Axis.horizontal;
        double spacing = Dp.px(wrap().getSpacing());
        double runSpacing = Dp.px(wrap().getRunSpacing());
        double maxMain = horizontal ? constraints.maxWidth() : constraints.maxHeight();

        BoxConstraints childC = constraints.loosen();
        List<RenderElement> kids = renderChildren();

        List<List<RenderElement>> runs = new ArrayList<List<RenderElement>>();
        List<Double> runCross = new ArrayList<Double>();
        List<Double> runMain = new ArrayList<Double>();
        Map<RenderElement, Double> mainStart = new HashMap<RenderElement, Double>();

        List<RenderElement> cur = new ArrayList<RenderElement>();
        double curMain = 0;
        double curCross = 0;
        for (RenderElement kid : kids) {
            Size cs = kid.layout(childC);
            double m = horizontal ? cs.width() : cs.height();
            double c = horizontal ? cs.height() : cs.width();
            double add = (cur.isEmpty() ? 0 : spacing) + m;
            if (!cur.isEmpty() && curMain + add > maxMain) {
                runs.add(cur);
                runCross.add(curCross);
                runMain.add(curMain);
                cur = new ArrayList<RenderElement>();
                curMain = 0;
                curCross = 0;
            }
            if (!cur.isEmpty()) {
                curMain += spacing;
            }
            mainStart.put(kid, curMain);
            curMain += m;
            curCross = Math.max(curCross, c);
            cur.add(kid);
        }
        if (!cur.isEmpty()) {
            runs.add(cur);
            runCross.add(curCross);
            runMain.add(curMain);
        }

        double totalMain = 0;
        double totalCross = 0;
        for (int i = 0; i < runs.size(); i++) {
            totalMain = Math.max(totalMain, runMain.get(i));
            totalCross += runCross.get(i);
            if (i > 0) {
                totalCross += runSpacing;
            }
        }

        Size self = horizontal
                ? constraints.constrain(new Size(totalMain, totalCross))
                : constraints.constrain(new Size(totalCross, totalMain));
        double boundedMain = horizontal ? self.width() : self.height();

        double crossPos = 0;
        for (int i = 0; i < runs.size(); i++) {
            List<RenderElement> run = runs.get(i);
            double runCr = runCross.get(i);
            double leading = mainLeading(boundedMain, runMain.get(i));
            for (RenderElement kid : run) {
                Size cs = kid.size();
                double kidCross = horizontal ? cs.height() : cs.width();
                double within = crossWithin(kidCross, runCr);
                double mp = leading + mainStart.get(kid);
                if (horizontal) {
                    setChildOffset(kid, mp, crossPos + within);
                } else {
                    setChildOffset(kid, crossPos + within, mp);
                }
            }
            crossPos += runCr + runSpacing;
        }
        return self;
    }

    private double mainLeading(double boundedMain, double runExtent) {
        double free = boundedMain - runExtent;
        if (free <= 0) {
            return 0;
        }
        switch (wrap().getAlignment()) {
            case center:
                return free / 2;
            case end:
                return free;
            default:
                return 0;
        }
    }

    private double crossWithin(double kidCross, double runCross) {
        WrapCrossAlignment a = wrap().getCrossAxisAlignment();
        if (a == WrapCrossAlignment.center) {
            return (runCross - kidCross) / 2;
        }
        if (a == WrapCrossAlignment.end) {
            return runCross - kidCross;
        }
        return 0;
    }
}
