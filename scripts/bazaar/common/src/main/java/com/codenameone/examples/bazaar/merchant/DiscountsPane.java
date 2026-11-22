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

import com.codename1.appkit.Net;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Discount codes and automatic discounts: percent or fixed off, free
/// shipping, buy X get Y, with a minimum, a usage limit and a date range.
final class DiscountsPane extends MerchantPane {
    DiscountsPane() {
        super("Discounts", "merchant-discounts");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add discount", "discount-add", () -> edit(null));
        final String store = MerchantState.storeId();
        Api.discounts().list(store, Net.to(list -> {
            if (store.equals(MerchantState.storeId())) {
                fill(list);
            }
        }, Ui::fail));
    }

    static String describe(DiscountDto d) {
        if ("PERCENT".equals(d.kind())) {
            return Lang.tr("{0}% off", ProductCsv.major(d.amount()));
        }
        if ("FIXED".equals(d.kind())) {
            return Lang.tr("{0} off", MerchantState.money(d.amount()));
        }
        if ("FREE_SHIPPING".equals(d.kind())) {
            return Lang.tr("Free shipping");
        }
        return Lang.tr("Buy {0}, get {1} at {2}% off", String.valueOf(d.buyQty()),
                String.valueOf(d.getQty()), ProductCsv.major(d.amount()));
    }

    private void fill(List<DiscountDto> list) {
        Container page = MUi.page("discounts-list");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        for (final DiscountDto d : list) {
            String uses = d.usageLimit() > 0 ? Lang.tr("{0} of {1} used", String.valueOf(d.usedCount()),
                    String.valueOf(d.usageLimit())) : Lang.tr("{0} used", String.valueOf(d.usedCount()));
            Container tap = Ui.tap(Ui.row(d.automatic() ? FontImage.MATERIAL_AUTO_AWESOME
                    : FontImage.MATERIAL_SELL, d.automatic() ? d.title() : d.code(),
                    describe(d) + Ui.DOT + uses, MUi.badge(d.active() ? "ACTIVE" : "DISABLED")),
                    "discount-" + d.id(), e -> edit(d));
            RowMenus.attach(tap, Command.create(Lang.tr("Delete"), null, e -> {
                if (MUi.confirm("Delete discount", "Customers can no longer use it.", "Delete")) {
                    Api.discounts().delete(MerchantState.storeId(), d.id(), Net.to(x -> load(), Ui::fail));
                }
            }),
                    Command.create(Lang.tr("Generate unique codes"), null, e -> codes(d)));
            card.add(tap);
        }
        if (list.isEmpty()) {
            card.add(Ui.empty(FontImage.MATERIAL_SELL, "No discounts",
                    "Create a code for customers to type, or a discount applied automatically."));
        }
        page.add(card);
        root(page);
    }

    /// Makes single-use copies of `d` with codes of their own, and saves them
    /// as a CSV to hand out.
    private void codes(final DiscountDto d) {
        if (d.automatic()) {
            Ui.say("Automatic discounts have no codes");
            return;
        }
        Container body = new Container(BoxLayout.y());
        com.codename1.ui.TextField count = Ui.field(body, "How many", "codes-count", "100",
                com.codename1.ui.TextArea.NUMERIC);
        com.codename1.ui.TextField prefix = MUi.text(body, "Prefix", "codes-prefix", "");
        Command ok = Command.create(Lang.tr("Generate"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (com.codename1.ui.Dialog.show(Lang.tr("Generate unique codes"), body, cancel, ok)
                != ok) {
            return;
        }
        int n = CustomerMatches.count(count.getText());
        if (n <= 0) {
            return;
        }
        Api.discounts().generateCodes(MerchantState.storeId(), d.id(),
                new com.codenameone.examples.bazaar.api.DiscountCodesDto(n,
                        prefix.getText().trim()), Net.to(x -> {
                    int rows = x.rows() == null ? 0 : x.rows().size();
                    String[][] out = new String[rows + 1][];
                    out[0] = x.columns().toArray(new String[x.columns().size()]);
                    for (int iter = 0; iter < rows; iter++) {
                        java.util.List<String> cells = x.rows().get(iter).cells();
                        out[iter + 1] = cells.toArray(new String[cells.size()]);
                    }
                    Files.saveCsv("codes-" + d.code() + ".csv", out);
                }, Ui::fail));
    }

    private void edit(DiscountDto d) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        CheckBox automatic = new CheckBox(Lang.tr("Apply automatically, no code"));
        automatic.setSelected(d != null && d.automatic());
        body.add(automatic);
        TextField code = MUi.text(body, "Code", "discount-code", d == null ? "" : d.code());
        TextField title = MUi.text(body, "Title", "discount-title", d == null ? "" : d.title());
        Picker kind = MUi.picker(body, Lang.tr("Type"), "discount-kind", Words.DISCOUNT_KINDS,
                d == null ? "PERCENT" : d.kind());
        TextField amount = Ui.field(body, Lang.tr("Percent, or amount for a fixed discount"),
                "discount-amount", d == null ? "10" : ProductCsv.major(d.amount()), TextField.DECIMAL);
        TextField min = MUi.money(body, "Minimum subtotal", "discount-min",
                d == null ? 0 : d.minSubtotalCents());
        TextField buy = MUi.number(body, "Buy quantity", "discount-buy", d == null ? 0 : d.buyQty());
        TextField get = MUi.number(body, "Get quantity", "discount-get", d == null ? 0 : d.getQty());
        TextField limit = MUi.number(body, "Usage limit (0 for none)", "discount-limit",
                d == null ? 0 : d.usageLimit());
        CheckBox once = new CheckBox(Lang.tr("Once per customer"));
        once.setSelected(d != null && d.oncePerCustomer());
        CheckBox combinable = new CheckBox(Lang.tr("Combines with other discounts"));
        combinable.setSelected(d != null && d.combinable());
        CheckBox active = new CheckBox(Lang.tr("Active"));
        active.setSelected(d == null || d.active());
        body.add(once).add(combinable).add(active);
        Command ok = Command.create(Lang.tr("Save"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Discount"), body, cancel, ok) != ok) {
            return;
        }
        String k = MUi.code(kind, Words.DISCOUNT_KINDS);
        String c = code.getText().trim();
        if (!automatic.isSelected() && c.length() == 0) {
            Ui.say("A discount needs a code");
            return;
        }
        // Percent and buy-X-get-Y amounts are basis points; "12.5" percent
        // parses as 1250 just as "12.50" dollars parses as 1250 cents.
        long amt = ProductCsv.cents(amount.getText());
        Api.discounts().save(MerchantState.storeId(), new DiscountDto(d == null ? null : d.id(), c,
                title.getText().trim().length() == 0 ? c : title.getText().trim(), k, Math.max(0, amt),
                MUi.cents(min), MUi.integer(buy), MUi.integer(get), d == null ? "ALL" : d.appliesTo(),
                d == null ? null : d.targetId(), automatic.isSelected(), MUi.integer(limit),
                d == null ? 0 : d.usedCount(), once.isSelected(), combinable.isSelected(),
                d == null ? 0 : d.startsAt(), d == null ? 0 : d.endsAt(), active.isSelected()),
                Net.to(x -> load(), Ui::fail));
    }
}
