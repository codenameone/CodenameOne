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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.ui.Lang;
import java.util.List;

/// Choosing a size and a colour, as Square and Shopify POS do it: the
/// product's photo, name and price on top, a row of large chips per option,
/// and one Add button that says what it will add and for how much.
///
/// One tap per option and no nested dialogs. A chip whose combination is out
/// of stock is dimmed and struck through, one the store does not make with
/// the other choices is faint; tapping either still works (it moves the other
/// options to the nearest variant that has it), so a cashier never hits a
/// dead end. Everything is redrawn in place, so the sheet never jumps.
final class VariantPicker {
    /// What the picker adds.
    interface Picked {
        void picked(PosItemDto variant, int quantity, Component photo);
    }

    private final List<PosItemDto> variants;
    private final VariantMatrix matrix;
    private final Picked picked;
    private final Sheet sheet;
    private final Container options = new Container(BoxLayout.y());
    private final Label variantLine = new Label("", "PosVpVariant");
    private final Label price = new Label("", "PosVpPrice");
    private final Label stock = new Label("", "PosVpStock");
    private final Label qtyLabel = new Label("1", "PosQty");
    private final Button add;
    private Label photo;
    private String[] chosen;
    private int quantity = 1;

    private VariantPicker(List<PosItemDto> variants, Picked picked) {
        this.variants = variants;
        this.picked = picked;
        this.matrix = new VariantMatrix(variants);
        this.chosen = matrix.initial();
        PosItemDto first = variants.get(0);
        sheet = PosSheet.open(first.title(), "pos-variants");
        add = PosUi.key("", "pos-variant-add", "PosPay", e -> addChosen());
    }

    /// Opens the picker over the register for the variants of one product.
    static void show(List<PosItemDto> variants, Picked picked) {
        new VariantPicker(variants, picked).build();
    }

    private void build() {
        PosItemDto first = variants.get(0);
        Container content = sheet.getContentPane();
        content.setLayout(new BorderLayout());
        content.setUIID("PosVpBody");

        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        body.setScrollVisible(false);

        // The hero: photo, the chosen variant, its price and stock.
        Container hero = new Container(new BorderLayout());
        hero.setUIID("PosVpHero");
        int side = CN.convertToPixels(PosUi.wide() ? 26 : 22);
        photo = TileArt.rounded(first.productId(), first.title(), first.photoId(), side);
        hero.add(BorderLayout.WEST, photo);
        Container facts = new Container(BoxLayout.y());
        facts.setUIID("PosVpFacts");
        price.setName("pos-variant-price");
        stock.setName("pos-variant-stock");
        facts.add(price).add(variantLine).add(stock);
        hero.add(BorderLayout.CENTER, facts);
        body.add(hero);
        body.add(options);

        // Quantity, for the cashier ringing up three of the same.
        Container qty = new Container(new BorderLayout());
        qty.setUIID("PosVpQtyRow");
        qty.add(BorderLayout.CENTER, PosUi.label("Quantity", "PosVpGroup"));
        Container step = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        step.setUIID("PosStepper");
        step.add(PosUi.icon(FontImage.MATERIAL_REMOVE, "", "pos-variant-minus", "PosStep", e -> {
            quantity = Math.max(1, quantity - 1);
            refresh();
        }));
        qtyLabel.setName("pos-variant-qty");
        step.add(qtyLabel);
        step.add(PosUi.icon(FontImage.MATERIAL_ADD, "", "pos-variant-plus", "PosStep", e -> {
            quantity = Math.min(99, quantity + 1);
            refresh();
        }));
        qty.add(BorderLayout.EAST, step);
        body.add(qty);

        content.add(BorderLayout.CENTER, body);
        Container foot = new Container(BoxLayout.y());
        foot.setUIID("PosVpFoot");
        foot.add(add);
        if (PosUi.wide()) {
            // Beside the cart the panel is the screen's full height; pinned
            // to its foot the button left a hand's width of nothing under the
            // quantity, so it follows the content there.
            body.add(foot);
        } else {
            body.setScrollableY(true);
            content.add(BorderLayout.SOUTH, foot);
        }
        refresh();
        sheet.show();
    }

    private void refresh() {
        options.removeAll();
        for (int g = 0; g < matrix.groups(); g++) {
            final int group = g;
            String n = matrix.name(g);
            String caption = "Size".equals(n) ? Lang.tr("Size") : "Colour".equals(n) ? Lang.tr("Colour")
                    : "Style".equals(n) ? Lang.tr("Style") : Lang.tr("Option");
            String picked = chosen[g] == null ? "" : chosen[g];
            Container head = new Container(new BorderLayout());
            head.setUIID("PosVpGroupRow");
            head.add(BorderLayout.WEST, PosUi.data(caption, "PosVpGroup"));
            head.add(BorderLayout.CENTER, PosUi.data(picked, "PosVpGroupValue"));
            options.add(head);
            Container chips = new Container(new FlowLayout(Component.LEFT));
            chips.setUIID("PosVpChips");
            List<String> values = matrix.values(g);
            for (int v = 0; v < values.size(); v++) {
                final String value = values.get(v);
                int state = matrix.state(g, value, chosen);
                boolean on = value.equals(chosen[g]);
                String uiid = on ? (state == VariantMatrix.AVAILABLE ? "PosVpChipOn" : "PosVpChipOnOut")
                        : state == VariantMatrix.AVAILABLE ? "PosVpChip"
                        : state == VariantMatrix.SOLD_OUT ? "PosVpChipOut" : "PosVpChipNone";
                Button chip = new Button(value, uiid);
                chip.setShouldLocalize(false);
                chip.setName("pos-opt-" + g + "-" + v);
                chip.addActionListener(e -> {
                    chosen = matrix.choose(chosen, group, value);
                    refresh();
                });
                chips.add(chip);
            }
            options.add(chips);
        }
        PosItemDto v = matrix.find(chosen);
        if (v == null) {
            variantLine.setText("");
            price.setText("");
            stock.setText(Lang.tr("Choose an option"));
            stock.setUIID("PosVpStock");
            add.setText(Lang.tr("Choose an option"));
            add.setUIID("PosPayIdle");
            add.setEnabled(false);
        } else {
            variantLine.setText(v.variantTitle());
            String now = PosState.money(v.priceCents());
            price.setText(now);
            boolean sellable = VariantMatrix.inStock(v);
            if (!v.trackInventory()) {
                stock.setText(Lang.tr("Always in stock"));
                stock.setUIID("PosVpStock");
            } else if (v.available() <= 0) {
                stock.setText(Lang.tr("Out of stock"));
                stock.setUIID("PosVpStockOut");
            } else if (v.available() <= 3) {
                stock.setText(Lang.tr("Only {0} left", v.available()));
                stock.setUIID("PosVpStockLow");
            } else {
                stock.setText(Lang.tr("{0} in stock", v.available()));
                stock.setUIID("PosVpStock");
            }
            // Out of stock still sells (the count may be behind the shelf),
            // but the button says so.
            add.setText((sellable ? Lang.tr("Add to sale") : Lang.tr("Add anyway")) + "  "
                    + PosState.money(v.priceCents() * quantity));
            add.setUIID(sellable ? "PosPay" : "PosVpAddAnyway");
            add.setEnabled(true);
        }
        qtyLabel.setText(String.valueOf(quantity));
        sheet.getContentPane().revalidateLater();
    }

    private void addChosen() {
        PosItemDto v = matrix.find(chosen);
        if (v == null) {
            return;
        }
        sheet.back();
        picked.picked(v, quantity, photo);
    }
}
