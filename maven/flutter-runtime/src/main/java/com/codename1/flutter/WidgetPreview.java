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
package com.codename1.flutter;

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.widgets.HasChild;
import com.codename1.flutter.widgets.LayoutBuilder;

import com.codename1.ui.Display;

/**
 * Asks what a widget would render, without mounting it.
 *
 * <p>A few places have to look THROUGH a composed widget rather than mount it —
 * a button consumes its icon instead of hosting it, for one — and each of them
 * used to guess at the shapes it knew. The gallery hands every demo page's
 * options button an {@code IconButton(icon: FeatureDiscovery(child: Icon(...)))},
 * and FeatureDiscovery builds a {@code LayoutBuilder}: a shape none of those
 * guesses covered, so the button drew nothing and the options icon was missing
 * from every demo screen in the app.</p>
 *
 * <p>Preview building is best-effort by nature — the widget is built outside the
 * tree, with no element behind it — so every step is guarded and a failure just
 * says "cannot tell" rather than propagating.</p>
 */
public final class WidgetPreview {

    private WidgetPreview() {
    }

    /**
     * One step of composition: the child of a wrapper, or the result of building
     * a composed widget. Null when {@code w} renders itself (a Text, an Icon, a
     * render widget) or when the step could not be taken.
     */
    public static Widget step(Widget w, BuildContext context) {
        if (w == null) {
            return null;
        }
        try {
            if (w instanceof HasChild) {
                return ((HasChild) w).getChild();
            }
            if (w instanceof LayoutBuilder) {
                dart.runtime.Funcs.Func2<BuildContext, BoxConstraints, Widget> b =
                        ((LayoutBuilder) w).getBuilder();
                return b == null ? null : b.call(context, viewport());
            }
            if (w instanceof StatelessWidget) {
                return ((StatelessWidget) w).build(context);
            }
            if (w instanceof StatefulWidget) {
                // A throwaway state, purely to see what the widget renders. It is
                // attached to its widget first: a State's build almost always reads
                // `widget.something`, and an unattached one throws on the first read.
                State<?> s = ((StatefulWidget) w).createState();
                if (s == null) {
                    return null;
                }
                s.attach(null, (StatefulWidget) w);
                return s.build(context);
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    /**
     * Follows {@link #step} until {@code type} turns up, or the walk runs out.
     * Bounded, because a preview build is not a mounted tree and a cycle here
     * would be a hang with no frame to show for it.
     */
    public static <T> T findLeaf(Widget from, Class<T> type, BuildContext context) {
        Widget cur = from;
        for (int depth = 0; depth < 6 && cur != null; depth++) {
            if (type.isInstance(cur)) {
                return type.cast(cur);
            }
            cur = step(cur, context);
        }
        return null;
    }

    /** The viewport, in logical pixels — what a LayoutBuilder is asked about. */
    private static BoxConstraints viewport() {
        double w = 400;
        double h = 800;
        if (Display.isInitialized()) {
            double scale = Dp.scale();
            if (scale > 0) {
                w = Display.getInstance().getDisplayWidth() / scale;
                h = Display.getInstance().getDisplayHeight() / scale;
            }
        }
        return new BoxConstraints(0, w, 0, h);
    }
}
