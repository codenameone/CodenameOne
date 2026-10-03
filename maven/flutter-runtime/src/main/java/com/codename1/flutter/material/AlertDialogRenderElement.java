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
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.widgets.Text;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Render element for {@link AlertDialog}: pure layout (the CN1 Dialog
 * provides the surface), Material metrics — 24lp content padding, a bold
 * 20lp title (synthesized when the title is a plain {@link Text}), the
 * content below it, and a right-aligned actions row with 8lp spacing.
 */
public class AlertDialogRenderElement extends RenderElement {

    /** Dialog content padding in logical pixels. */
    public static final double PAD_LP = 24;
    /** Gap between title and content in logical pixels. */
    public static final double TITLE_GAP_LP = 16;
    /** Actions row padding / spacing in logical pixels. */
    public static final double ACTION_PAD_LP = 8;
    /** Material minimum dialog width in logical pixels. */
    public static final double MIN_WIDTH_LP = 280;
    /** Synthesized title font size in logical pixels. */
    public static final double TITLE_FONT_LP = 20;

    private Element titleChild;
    private Element contentChild;
    private List<Element> actionChildren = new ArrayList<Element>();

    public AlertDialogRenderElement(AlertDialog widget) {
        super(widget);
    }

    private AlertDialog dialog() {
        return (AlertDialog) widget();
    }

    /**
     * A plain (styleless) Text title gets the Material bold headline style;
     * everything else mounts as-is. Reconciliation keeps this cheap: the
     * synthesized Text updates the same element in place on rebuilds.
     */
    private Widget titleWidget() {
        Widget t = dialog().getTitle();
        if (t instanceof Text && ((Text) t).getStyle() == null) {
            Text styled = new Text(((Text) t).getData());
            TextStyle ts = new TextStyle();
            ts.fontSize(TITLE_FONT_LP);
            ts.fontWeight(com.codename1.flutter.FontWeight.bold);
            styled.style(ts);
            return styled;
        }
        return t;
    }

    @Override
    protected void syncChildren() {
        titleChild = updateChild(titleChild, titleWidget(), 0);
        contentChild = updateChild(contentChild, dialog().getContent(), 1);
        List<Widget> actionWidgets = new ArrayList<Widget>();
        if (dialog().getActions() != null) {
            for (Widget w : dialog().getActions()) {
                if (w != null) {
                    actionWidgets.add(w);
                }
            }
        }
        actionChildren = updateChildren(actionChildren, actionWidgets);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (titleChild != null) {
            visitor.call(titleChild);
        }
        if (contentChild != null) {
            visitor.call(contentChild);
        }
        for (Element a : actionChildren) {
            if (a != null) {
                visitor.call(a);
            }
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double pad = Dp.px(PAD_LP);
        double titleGap = Dp.px(TITLE_GAP_LP);
        double actionPad = Dp.px(ACTION_PAD_LP);

        double innerMax = constraints.hasBoundedWidth()
                ? Math.max(0, constraints.maxWidth() - pad * 2)
                : Double.POSITIVE_INFINITY;
        BoxConstraints childConstraints =
                new BoxConstraints(0, innerMax, 0, Double.POSITIVE_INFINITY);

        RenderElement title = RenderElement.findRenderElement(titleChild);
        RenderElement content = RenderElement.findRenderElement(contentChild);
        List<RenderElement> actions = new ArrayList<RenderElement>();
        for (Element a : actionChildren) {
            RenderElement r = RenderElement.findRenderElement(a);
            if (r != null) {
                actions.add(r);
            }
        }

        double innerWidth = 0;
        Size titleSize = Size.ZERO;
        if (title != null) {
            titleSize = title.layout(childConstraints);
            innerWidth = Math.max(innerWidth, titleSize.width());
        }
        Size contentSize = Size.ZERO;
        if (content != null) {
            contentSize = content.layout(childConstraints);
            innerWidth = Math.max(innerWidth, contentSize.width());
        }
        double actionsW = 0;
        double actionsH = 0;
        for (RenderElement a : actions) {
            Size as = a.layout(childConstraints);
            actionsW += as.width() + (actionsW > 0 ? actionPad : 0);
            actionsH = Math.max(actionsH, as.height());
        }
        innerWidth = Math.max(innerWidth, actionsW);

        double width = constraints.hasBoundedWidth()
                ? constraints.maxWidth()
                : Math.max(Dp.px(MIN_WIDTH_LP), innerWidth + pad * 2);

        double y = pad;
        if (title != null) {
            setChildOffset(title, pad, y);
            y += titleSize.height() + (content != null ? titleGap : 0);
        }
        if (content != null) {
            setChildOffset(content, pad, y);
            y += contentSize.height();
        }
        if (!actions.isEmpty()) {
            y += actionPad * 2;
            // right-aligned, last action flush with the right padding
            double x = width - actionPad;
            for (int i = actions.size() - 1; i >= 0; i--) {
                RenderElement a = actions.get(i);
                x -= a.size().width();
                setChildOffset(a, x, y + (actionsH - a.size().height()) / 2);
                x -= actionPad;
            }
            y += actionsH + actionPad;
        } else {
            y += pad;
        }

        return constraints.constrain(new Size(width, y));
    }
}
