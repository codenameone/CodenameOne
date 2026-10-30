/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.pulse.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/// One line of a record: what it is, and its value at the far side.
///
/// A row of two labels in a `BorderLayout` gives the value all the width it
/// asks for and the caption what is left, so a long value cut the caption to
/// "Legal nar" or "Veh". This row puts the two side by side while they fit,
/// and otherwise the caption on a line of its own with the value under it.
final class FactRow extends Container {
    private final Label caption;
    private final Label value;
    /// Whether the height last reported was the stacked one.
    private boolean plannedStacked;

    FactRow(Label caption, Label value) {
        this.caption = caption;
        this.value = value;
        value.setEndsWith3Points(true);
        setLayout(new Rows());
        add(caption);
        add(value);
    }

    private int inner(int width) {
        Style s = getStyle();
        return width - s.getHorizontalPadding();
    }

    private static int outerWidth(Component c) {
        return c.getPreferredW() + c.getStyle().getHorizontalMargins();
    }

    private static int outerHeight(Component c) {
        return c.getPreferredH() + c.getStyle().getVerticalMargins();
    }

    private boolean stacked(int room) {
        // A gap the width of a space or two between the caption and its value.
        return outerWidth(caption) + CN.convertToPixels(3) + outerWidth(value) > room;
    }

    private int roomOffered() {
        if (getWidth() > 0) {
            return inner(getWidth());
        }
        Container parent = getParent();
        while (parent != null) {
            if (parent.getWidth() > 0) {
                return inner(parent.getWidth() - parent.getStyle().getHorizontalPadding());
            }
            parent = parent.getParent();
        }
        return inner(CN.getDisplayWidth());
    }

    private final class Rows extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            Style s = getStyle();
            int left = s.getPaddingLeftNoRTL();
            int top = s.getPaddingTop();
            int room = inner(getWidth());
            boolean stack = stacked(room);
            boolean rtl = isRTL();
            int cw = Math.min(room, outerWidth(caption));
            int vw = Math.min(room, outerWidth(value));
            int ch = outerHeight(caption);
            int vh = outerHeight(value);
            Style cs = caption.getStyle();
            Style vs = value.getStyle();
            if (stack) {
                place(caption, rtl ? left + room - cw : left, top, cw, ch, cs);
                place(value, rtl ? left : left + room - vw, top + ch, vw, vh, vs);
            } else {
                int h = Math.max(ch, vh);
                place(caption, rtl ? left + room - cw : left, top + (h - ch) / 2, cw, ch, cs);
                place(value, rtl ? left : left + room - vw, top + (h - vh) / 2, vw, vh, vs);
            }
            if (stack != plannedStacked) {
                // The height was answered for another width.
                plannedStacked = stack;
                CN.callSerially(() -> {
                    setShouldCalcPreferredSize(true);
                    Form form = getComponentForm();
                    if (form != null) {
                        form.revalidate();
                    }
                });
            }
        }

        private void place(Component c, int x, int y, int w, int h, Style st) {
            c.setX(x + st.getMarginLeftNoRTL());
            c.setY(y + st.getMarginTop());
            c.setWidth(w - st.getHorizontalMargins());
            c.setHeight(h - st.getVerticalMargins());
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            Style s = getStyle();
            plannedStacked = stacked(roomOffered());
            int ch = outerHeight(caption);
            int vh = outerHeight(value);
            int h = plannedStacked ? ch + vh : Math.max(ch, vh);
            int w = plannedStacked ? Math.max(outerWidth(caption), outerWidth(value))
                    : outerWidth(caption) + CN.convertToPixels(3) + outerWidth(value);
            return new Dimension(w + s.getHorizontalPadding(), h + s.getVerticalPadding());
        }
    }
}
