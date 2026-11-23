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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The merchant screens' building blocks, on top of `Ui`: pickers of server
/// codes, money and number fields, status badges, sparklines and grids.
final class MUi {
    private MUi() {
    }

    /// A scrolling page for a form.
    static Container page(String name) {
        Container page = Ui.formPage("MePage");
        page.setName(name);
        return page;
    }

    /// A status as a coloured badge.
    static Label badge(String code) {
        return Ui.badge(Lang.tr(Words.of(code)), Words.badge(code));
    }

    /// A picker of `codes`, showing their words.
    static Picker picker(Container to, String caption, String name, String[] codes,
            String current) {
        Picker p = new Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setUIID("SvField");
        p.setName(name);
        String[] labels = new String[codes.length];
        for (int iter = 0; iter < codes.length; iter++) {
            labels[iter] = Lang.tr(Words.of(codes[iter]));
        }
        p.setStrings(labels);
        int at = 0;
        for (int iter = 0; iter < codes.length; iter++) {
            if (codes[iter].equals(current)) {
                at = iter;
            }
        }
        p.setSelectedStringIndex(at);
        if (to != null) {
            if (caption != null) {
                to.add(Ui.label(caption, "SvLabel"));
            }
            to.add(p);
        }
        return p;
    }

    /// The code a picker made by `picker` has chosen.
    static String code(Picker p, String[] codes) {
        int at = p.getSelectedStringIndex();
        return at >= 0 && at < codes.length ? codes[at] : codes[0];
    }

    /// A text field already holding `value`.
    static TextField text(Container to, String caption, String name, String value) {
        TextField f = Ui.field(to, caption, name, "");
        f.setText(value == null ? "" : value);
        return f;
    }

    /// A field for an amount of money, typed in major units.
    static TextField money(Container to, String caption, String name, long cents) {
        TextField f = Ui.field(to, caption, name, "0.00", TextArea.DECIMAL);
        f.setText(cents == 0 ? "" : ProductCsv.major(cents));
        return f;
    }

    static long cents(TextField f) {
        return ProductCsv.cents(f.getText());
    }

    /// A field for a whole number.
    static TextField number(Container to, String caption, String name, long value) {
        TextField f = Ui.field(to, caption, name, "0", TextArea.NUMERIC);
        f.setText(value == 0 ? "" : String.valueOf(value));
        return f;
    }

    static int integer(TextField f) {
        return ProductCsv.typedInteger(f.getText());
    }

    /// Asks before something that cannot be undone.
    static boolean confirm(String title, String text, String ok) {
        return Dialog.show(Lang.tr(title), Lang.tr(text), Lang.tr(ok), Lang.tr("Cancel"));
    }

    /// A data grid in the merchant style: striped, rows a comfortable height,
    /// any rows selectable, the keyboard moving between cells.
    static DataGrid grid(Rows<?> rows, String name) {
        DataGrid grid = new DataGrid(rows);
        grid.setName(name);
        grid.setStriped(true);
        grid.setGridLines(true, false);
        grid.setRowHeightMM(8f);
        grid.setSelectionMode(DataGrid.SelectionMode.MULTIPLE_ROWS);
        grid.setEmptyText(Lang.tr("Nothing here yet"));
        // Columns share whatever width is left over, so a narrow grid fills
        // the page instead of ending in an empty band on the right.
        for (int iter = 0; iter < grid.getColumnCount(); iter++) {
            grid.getColumn(iter).setFlex(1);
        }
        return grid;
    }

    static int color(String uiid) {
        Style style = UIManager.getInstance().getComponentStyle(uiid);
        return 0xff000000 | style.getFgColor();
    }

    /// A card on the dashboard: a big number, its caption and a sparkline.
    static Container tile(String value, String caption, Component trailing, String name) {
        Container tile = new Container(new BorderLayout());
        tile.setUIID("MeTile");
        tile.setName(name);
        Container words = new Container(BoxLayout.y());
        // Both lines shrink to fit half a phone width rather than ending in
        // "..." ("$3720...." and "Returning cust..." did).
        Label c = Ui.plain(caption, "MeTileCaption");
        // Auto-size only ever shrinks: uncapped it grows a short caption or
        // value ("Orders", "0%") until it is clipped out of the tile, which
        // left most analytics tiles blank. The caps are the styles' sizes.
        c.setAutoSizeMode(true);
        c.setMaxAutoSize(2.6f);
        words.add(c);
        Label v = new Label(value, "MeTileValue");
        v.setShouldLocalize(false);
        v.setAutoSizeMode(true);
        v.setMaxAutoSize(5f);
        words.add(v);
        tile.add(BorderLayout.CENTER, words);
        if (trailing != null) {
            tile.add(BorderLayout.SOUTH, trailing);
        }
        return tile;
    }

    /// A line of buttons at the end of a form.
    /// Spaces a two-field grid row: `first` gets a gap to its right and
    /// `row` one under it. A grid has no gap of its own, so two SvFields side
    /// by side, and the row beneath them, drew their borders touching.
    static void pairGap(Component first, Container row) {
        int gap = com.codename1.ui.CN.convertToPixels(1.2f);
        first.getAllStyles().setMarginRight(gap);
        row.getAllStyles().setMarginBottom(gap);
    }

    static Container buttons(Component... buttons) {
        // A flow has no gap of its own, and callers add buttons after this
        // returns, so the space between them is set when the row is shown:
        // without it Duplicate, Archive and Save touched edge to edge.
        Container row = new Container(new com.codename1.ui.layouts.FlowLayout(Component.RIGHT)) {
            @Override
            protected void initComponent() {
                super.initComponent();
                int gap = com.codename1.ui.CN.convertToPixels(1.6f);
                for (int iter = 1; iter < getComponentCount(); iter++) {
                    getComponentAt(iter).getAllStyles().setMarginLeft(gap);
                }
            }
        };
        row.setUIID("MeButtons");
        for (int iter = 0; iter < buttons.length; iter++) {
            row.add(buttons[iter]);
        }
        return row;
    }

    /// Actions that sit inside a card: left aligned, no bar and no divider,
    /// so they read as part of the card rather than the foot of the screen.
    static Container cardButtons(Component... buttons) {
        Container row = buttons(buttons);
        ((com.codename1.ui.layouts.FlowLayout) row.getLayout()).setAlign(Component.LEFT);
        row.setUIID("MeCardButtons");
        return row;
    }

    /// A search field that runs `typed` as the user types, after a short pause.
    static TextField search(String name, final Runnable typed) {
        final TextField f = new TextField("", Lang.tr("Search"), 20, TextArea.ANY);
        f.setUIID("MeSearch");
        f.setName(name);
        f.getHintLabel().setUIID("SvFieldHint");
        com.codename1.ui.FontImage.setMaterialIcon(f.getHintLabel(),
                com.codename1.ui.FontImage.MATERIAL_SEARCH, 3f);
        final com.codename1.ui.util.UITimer[] timer = new com.codename1.ui.util.UITimer[1];
        f.addDataChangedListener((type, index) -> {
            if (timer[0] != null) {
                timer[0].cancel();
            }
            if (f.getComponentForm() == null) {
                typed.run();
                return;
            }
            timer[0] = com.codename1.ui.util.UITimer.timer(300, false, f.getComponentForm(),
                    typed::run);
        });
        return f;
    }

    /// Whether `text` contains `part`, ignoring ASCII case.
    static boolean has(String text, String part) {
        if (part == null || part.length() == 0) {
            return true;
        }
        if (text == null) {
            return false;
        }
        int n = part.length();
        for (int iter = 0; iter + n <= text.length(); iter++) {
            if (text.regionMatches(true, iter, part, 0, n)) {
                return true;
            }
        }
        return false;
    }

    /// A short day and time for a list.
    static String when(long millis) {
        return millis <= 0 ? "" : Ui.when(millis);
    }
}
