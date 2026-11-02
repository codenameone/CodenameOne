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
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// A list and what was picked from it, side by side, on a wide window.
///
/// On a phone a row of a list opens its own screen. On a tablet on its side, a
/// desktop or a browser there is room for both, so the list keeps the left and
/// the row picked fills the right -- working down a list of members or
/// accounts no longer means going back to it each time. A screen asks
/// [Layouts#wide()] whether to make one; the detail is drawn by the same code
/// that draws the phone's screen, into [#detail()].
public final class Split {
    /// The width of the list, in millimetres.
    private static final float MASTER_MM = 110f;
    private final Container detail = Ui.page();
    private Container picked;

    /// Lays `top` -- a search field, a row of filters -- and `list` out on
    /// the left of `form`, and the detail on the right.
    public Split(Form form, Component top, Component list) {
        Container master = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension size = super.calcPreferredSize();
                size.setWidth(CN.convertToPixels(MASTER_MM));
                return size;
            }
        };
        master.setUIID("SvMaster");
        if (top != null) {
            master.add(BorderLayout.NORTH, top);
        }
        master.add(BorderLayout.CENTER, list);
        detail.setName("detail");
        Container both = new Container(new BorderLayout());
        both.add(BorderLayout.WEST, master);
        both.add(BorderLayout.CENTER, detail);
        form.add(BorderLayout.CENTER, both);
    }

    /// Where the row picked is drawn.
    public Container detail() {
        return detail;
    }

    /// Whether a row has been picked yet.
    public boolean hasPick() {
        return picked != null;
    }

    /// Marks `row` as the one picked, and unmarks the last.
    public void pick(Container row) {
        if (picked != null) {
            picked.setUIID("SvRow");
        }
        picked = row;
        row.setUIID("SvRowPicked");
        row.getParent().repaint();
    }

    /// Forgets the row picked: the list was filled again.
    public void clear() {
        picked = null;
    }
}
