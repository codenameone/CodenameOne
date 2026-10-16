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

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Flutter's Flex layout algorithm (RenderFlex, M1 subset):
 * <ol>
 *   <li>Non-flex children get an unbounded main axis and the incoming cross
 *       axis (tight when stretch, loose otherwise).</li>
 *   <li>Remaining main-axis space is distributed to flex children
 *       (Expanded), each laid out with a tight main extent of
 *       {@code freeSpace * flex / totalFlex}.</li>
 *   <li>The main size is the max constraint when {@code MainAxisSize.max}
 *       and bounded, otherwise the sum of the children.</li>
 *   <li>Leading/between spacing per {@link MainAxisAlignment}, cross-axis
 *       placement per {@link CrossAxisAlignment}.</li>
 * </ol>
 * Owns no CN1 component — pure positioning math over the flattened leaves.
 */
public class FlexRenderElement extends RenderElement {

    private List<Element> children = new ArrayList<Element>();

    public FlexRenderElement(Flex widget) {
        super(widget);
    }

    private Flex flex() {
        return (Flex) widget();
    }

    @Override
    protected void syncChildren() {
        List<Widget> newWidgets = new ArrayList<Widget>();
        if (flex().getChildren() != null) {
            for (Widget w : flex().getChildren()) {
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
        boolean vertical = flex().isVertical();
        MainAxisAlignment mainAlign = flex().getMainAxisAlignment();
        CrossAxisAlignment crossAlign = flex().getCrossAxisAlignment();
        MainAxisSize mainSizeMode = flex().getMainAxisSize();

        double maxMain = vertical ? constraints.maxHeight() : constraints.maxWidth();
        double maxCross = vertical ? constraints.maxWidth() : constraints.maxHeight();
        boolean boundedMain = maxMain != Double.POSITIVE_INFINITY;
        boolean boundedCross = maxCross != Double.POSITIVE_INFINITY;

        List<RenderElement> renderChildren = renderChildren();
        int n = renderChildren.size();

        // Cross-axis constraints shared by all children.
        double minCrossChild = (crossAlign == CrossAxisAlignment.stretch && boundedCross) ? maxCross : 0;
        double maxCrossChild = maxCross;

        // Pass 1: layout inflexible children, tally flex factors.
        long totalFlex = 0;
        double allocatedMain = 0;
        double maxChildCross = 0;
        for (RenderElement child : renderChildren) {
            long f = flexOf(child);
            if (f > 0) {
                totalFlex += f;
            } else {
                BoxConstraints childConstraints = vertical
                        ? new BoxConstraints(minCrossChild, maxCrossChild, 0, Double.POSITIVE_INFINITY)
                        : new BoxConstraints(0, Double.POSITIVE_INFINITY, minCrossChild, maxCrossChild);
                Size cs = child.layout(childConstraints);
                allocatedMain += mainOf(cs, vertical);
                maxChildCross = Math.max(maxChildCross, crossOf(cs, vertical));
            }
        }

        // Pass 2: layout flexible children in the remaining space.
        double freeSpace = boundedMain ? Math.max(0, maxMain - allocatedMain) : 0;
        if (totalFlex > 0) {
            double allocatedFlex = 0;
            int flexSeen = 0;
            int flexCount = 0;
            for (RenderElement child : renderChildren) {
                if (flexOf(child) > 0) {
                    flexCount++;
                }
            }
            for (RenderElement child : renderChildren) {
                long f = flexOf(child);
                if (f <= 0) {
                    continue;
                }
                flexSeen++;
                double extent;
                if (boundedMain) {
                    // last flex child absorbs rounding remainder
                    extent = (flexSeen == flexCount)
                            ? freeSpace - allocatedFlex
                            : freeSpace * f / totalFlex;
                    allocatedFlex += extent;
                } else {
                    extent = 0;
                }
                BoxConstraints childConstraints;
                if (boundedMain) {
                    // Expanded is a TIGHT fit — it must fill its share. Flexible
                    // defaults to LOOSE: it may take less, and whatever it leaves
                    // is free space for mainAxisAlignment to distribute. Treating
                    // both as tight is why a `Row(spaceBetween, [Text,
                    // Flexible(Text)])` put its second child straight after the
                    // first instead of at the far end — the colors demo's shade
                    // name and hex value ran together as "50#FFFFEBEE".
                    double minExtent = looseFit(child) ? 0 : extent;
                    childConstraints = vertical
                            ? new BoxConstraints(minCrossChild, maxCrossChild, minExtent, extent)
                            : new BoxConstraints(minExtent, extent, minCrossChild, maxCrossChild);
                } else {
                    // Degenerate case (flex inside unbounded main axis is an
                    // error in Flutter); fall back to intrinsic sizing.
                    childConstraints = vertical
                            ? new BoxConstraints(minCrossChild, maxCrossChild, 0, Double.POSITIVE_INFINITY)
                            : new BoxConstraints(0, Double.POSITIVE_INFINITY, minCrossChild, maxCrossChild);
                }
                Size cs = child.layout(childConstraints);
                allocatedMain += mainOf(cs, vertical);
                maxChildCross = Math.max(maxChildCross, crossOf(cs, vertical));
            }
        }

        // Own size.
        double mainSize = (mainSizeMode == MainAxisSize.max && boundedMain) ? maxMain : allocatedMain;
        mainSize = vertical ? constraints.constrainHeight(mainSize) : constraints.constrainWidth(mainSize);
        double crossSize = vertical ? constraints.constrainWidth(maxChildCross) : constraints.constrainHeight(maxChildCross);

        // Spacing per main-axis alignment.
        double remaining = Math.max(0, mainSize - allocatedMain);
        double leading = 0;
        double between = 0;
        switch (mainAlign) {
            case start:
                break;
            case end:
                leading = remaining;
                break;
            case center:
                leading = remaining / 2;
                break;
            case spaceBetween:
                between = n > 1 ? remaining / (n - 1) : 0;
                break;
            case spaceAround:
                between = n > 0 ? remaining / n : 0;
                leading = between / 2;
                break;
            case spaceEvenly:
                between = remaining / (n + 1);
                leading = between;
                break;
        }

        // Position children.
        double mainPos = leading;
        for (RenderElement child : renderChildren) {
            Size cs = child.size();
            double childCross = crossOf(cs, vertical);
            double crossPos;
            switch (crossAlign) {
                case start:
                case stretch:
                    crossPos = 0;
                    break;
                case end:
                    crossPos = crossSize - childCross;
                    break;
                case center:
                default:
                    crossPos = (crossSize - childCross) / 2;
                    break;
            }
            if (vertical) {
                setChildOffset(child, crossPos, mainPos);
            } else {
                setChildOffset(child, mainPos, crossPos);
            }
            mainPos += mainOf(cs, vertical) + between;
        }

        return vertical ? new Size(crossSize, mainSize) : new Size(mainSize, crossSize);
    }

    /** Whether this flexible child may take less than its share ({@code FlexFit.loose}). */
    private static boolean looseFit(RenderElement child) {
        if (!(child instanceof ExpandedRenderElement)) {
            return false;
        }
        com.codename1.flutter.Widget w = child.widget();
        return w instanceof Flexible
                && ((Flexible) w).getFit() == com.codename1.flutter.FlexFit.loose;
    }

    private static long flexOf(RenderElement child) {
        if (child instanceof ExpandedRenderElement) {
            return ((ExpandedRenderElement) child).flex();
        }
        return 0;
    }

    private static double mainOf(Size s, boolean vertical) {
        return vertical ? s.height() : s.width();
    }

    private static double crossOf(Size s, boolean vertical) {
        return vertical ? s.width() : s.height();
    }
}
