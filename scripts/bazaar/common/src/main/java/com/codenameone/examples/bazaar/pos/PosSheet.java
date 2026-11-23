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
        Sheet sheet = new Sheet(null, title, "PosSheet");
        if (name != null) {
            sheet.setName(name);
        }
        sheet.setPosition(PosUi.wide() ? BorderLayout.EAST : BorderLayout.SOUTH);
        sheet.getContentPane().setLayout(BoxLayout.y());
        sheet.getContentPane().setUIID("PosSheetBody");
        return sheet;
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
