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
package com.codenameone.examples.bazaar.pos;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;

/// The register's panels. Every question the register asks opens inside its
/// own layout instead of a popup: on a wide screen a panel slides in from the
/// right beside the cart, on a phone a sheet slides up from the bottom. Both
/// close on their close button or a tap outside them, and both take their
/// look from the `PosSheet*` styles in `theme.css`.
final class PosSheet {
    private PosSheet() {
    }

    /// What a two way question answers.
    interface Choice {
        void chose(boolean first);
    }

    /// A new register panel titled `title` (already translated), named `name`
    /// for the tour and the tests. Fill its content pane and call `show()`.
    static Sheet open(String title, String name) {
        final boolean wide = PosUi.wide();
        Sheet sheet = new Sheet(null, title, "PosSheet") {
            @Override
            protected void initComponent() {
                super.initComponent();
                // The sheet's own dim is a fixed 30% black that ignores the
                // theme's tintColor: over the light title bar it read as a
                // flat grey slab. The layered pane it sits in takes the same
                // 40% scrim dialogs use instead, in light and dark alike.
                Container host = getParent();
                if (host != null && !(host.getStyle().getBgPainter() instanceof Scrim)) {
                    host.getStyle().setBgPainter(new Scrim());
                    host.repaint();
                }
            }

            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                if (wide) {
                    // The side panel used to take its content's preferred
                    // width, measured once when it opened. Bold titles and the
                    // milestone headline measure narrower than they paint, so
                    // they were cut at the right padding. The panel takes a
                    // fixed share of the screen instead, wide enough for every
                    // register panel, and never more than the screen leaves.
                    com.codename1.ui.geom.Dimension w = super.calcPreferredSize();
                    int dw = com.codename1.ui.CN.getDisplayWidth();
                    int target = Math.max(com.codename1.ui.CN.convertToPixels(95), dw / 3);
                    w.setWidth(Math.min(Math.max(w.getWidth(), target), dw / 2));
                    return w;
                }
                // A phone sheet stops below the title bar: grown to the top of
                // the screen it slid over the title and cut it in half. The
                // tallest content scrolls inside it instead.
                com.codename1.ui.geom.Dimension d = super.calcPreferredSize();
                com.codename1.ui.Form f = com.codename1.ui.CN.getCurrentForm();
                if (f != null && f.getTitleArea() != null) {
                    Component t = f.getTitleArea();
                    int max = f.getHeight() - (t.getAbsoluteY() + t.getHeight())
                            - getStyle().getVerticalMargins();
                    if (max > 0 && d.getHeight() > max) {
                        d.setHeight(max);
                    }
                }
                return d;
            }
        };
        // The bold title face paints wider than it measures, and the title
        // sits at exactly its measured width: on a tablet "Sale complete"
        // lost its last letter. A title of our own carries the slack.
        com.codename1.ui.Label head = new com.codename1.ui.Label(title, "PosSheetTitle") {
            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                com.codename1.ui.geom.Dimension d = super.calcPreferredSize();
                d.setWidth(d.getWidth() + getStyle().getFont().getHeight());
                return d;
            }
        };
        head.setAlignment(Component.CENTER);
        sheet.setTitleComponent(head);
        if (name != null) {
            sheet.setName(name);
        }
        sheet.setPosition(PosUi.wide() ? BorderLayout.EAST : BorderLayout.SOUTH);
        sheet.getContentPane().setLayout(BoxLayout.y());
        sheet.getContentPane().setUIID("PosSheetBody");
        return sheet;
    }

    /// The dim behind a register panel: 40% black, the `tintColor` the theme
    /// gives dialogs, so the screen behind reads as the same screen set back.
    static final class Scrim implements com.codename1.ui.Painter {
        @Override
        public void paint(com.codename1.ui.Graphics g, com.codename1.ui.geom.Rectangle rect) {
            int alpha = g.getAlpha();
            g.setAlpha(alpha * 0x66 / 0xff);
            g.setColor(0x0);
            g.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            g.setAlpha(alpha);
        }
    }

    /// A new register panel the caller names itself.
    static Sheet open(String title) {
        return open(title, null);
    }

    /// The row a panel ends with: the quiet choice first, the main one last
    /// and wider, both full touch height.
    static Container actions(Component secondary, Component primary) {
        Container row = new Container(new GridLayout(1, 2), "PosSheetActions");
        row.add(secondary).add(primary);
        return row;
    }

    /// Asks a yes or no question; `yes` runs only when the main button is
    /// tapped. A tap outside, the close button or `no` leave things as they
    /// were. `danger` paints the main button as a destructive action.
    static void confirm(String title, String message, String yes, String no, String name,
            boolean danger, final Runnable onYes) {
        final Sheet sheet = open(title, name);
        sheet.getContentPane().add(PosUi.label(message, "PosSheetText"));
        sheet.getContentPane().add(actions(
                PosUi.key(no, name + "-no", "PosSecondary", e -> sheet.back()),
                PosUi.key(yes, name + "-yes", danger ? "PosDanger" : "PosPrimary", e -> {
                    sheet.back();
                    onYes.run();
                })));
        sheet.show();
    }

    /// Asks which of two answers applies. Dismissing the panel answers
    /// nothing.
    static void choose(String title, String message, String first, String second, String name,
            final Choice got) {
        final Sheet sheet = open(title, name);
        sheet.getContentPane().add(PosUi.label(message, "PosSheetText"));
        sheet.getContentPane().add(actions(
                PosUi.key(second, name + "-second", "PosSecondary", e -> {
                    sheet.back();
                    got.chose(false);
                }),
                PosUi.key(first, name + "-first", "PosSecondary", e -> {
                    sheet.back();
                    got.chose(true);
                })));
        sheet.show();
    }
}
