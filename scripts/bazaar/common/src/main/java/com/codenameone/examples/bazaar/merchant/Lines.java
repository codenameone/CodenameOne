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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.VariantHitDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The line entry transfers and purchase orders share: scan or type a SKU or
/// barcode, then a quantity (and for a purchase order a unit cost). Each
/// line is a variant the server found by that code.
final class Lines {
    /// One entered line.
    static final class Line {
        final VariantHitDto hit;
        final int quantity;
        final long costCents;

        Line(VariantHitDto hit, int quantity, long costCents) {
            this.hit = hit;
            this.quantity = quantity;
            this.costCents = costCents;
        }
    }

    final List<Line> lines = new ArrayList<Line>();
    private final Container list = new Container(BoxLayout.y());
    private final boolean withCost;

    Lines(Container to, boolean withCost) {
        this.withCost = withCost;
        final TextField code = Scan.field("line-code", null);
        final TextField qty = new TextField("1", Lang.tr("Qty"), 4, TextField.NUMERIC);
        qty.setUIID("SvField");
        qty.setName("line-qty");
        final TextField cost = new TextField("", Lang.tr("Unit cost"), 6, TextField.DECIMAL);
        cost.setUIID("SvField");
        cost.setName("line-cost");
        final Runnable add = () -> {
            String typed = code.getText().trim();
            if (typed.length() == 0) {
                return;
            }
            Api.inventory().lookup(MerchantState.storeId(), typed, Net.to(hit -> {
                int q = Math.max(1, MUi.integer(qty));
                long c = withCost ? (cost.getText().trim().length() == 0
                        ? hit.variant().costCents() : MUi.cents(cost)) : 0;
                lines.add(new Line(hit, q, c));
                code.setText("");
                qty.setText("1");
                cost.setText("");
                paint();
                code.startEditingAsync();
            }, (s, m) -> Ui.say("No product has that code")));
        };
        code.setDoneListener(e -> add.run());
        Container entry = new Container(new BorderLayout());
        Container right = new Container(BoxLayout.x());
        right.add(qty);
        if (withCost) {
            right.add(cost);
        }
        right.add(Scan.button("line-scan", code, add));
        right.add(Ui.flat(FontImage.MATERIAL_ADD, "line-add", e -> add.run()));
        entry.add(BorderLayout.CENTER, code);
        entry.add(BorderLayout.EAST, right);
        to.add(Ui.label("Scan or type a SKU or barcode", "SvLabel"));
        to.add(entry);
        to.add(list);
    }

    private void paint() {
        list.removeAll();
        for (int iter = 0; iter < lines.size(); iter++) {
            final int at = iter;
            Line l = lines.get(iter);
            String detail = Lang.tr("Qty {0}", String.valueOf(l.quantity))
                    + (withCost ? Ui.DOT + MerchantState.money(l.costCents) : "");
            list.add(Ui.row((char) 0, l.hit.productTitle() + Ui.DOT + l.hit.variant().title(), detail,
                    Ui.flat(FontImage.MATERIAL_DELETE, "line-remove-" + iter, e -> {
                        lines.remove(at);
                        paint();
                    })));
        }
        Ui.relayout(list);
    }
}
