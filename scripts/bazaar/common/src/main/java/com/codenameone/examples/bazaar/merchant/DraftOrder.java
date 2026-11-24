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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.DraftOrderDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// A draft order: a phone or email order the merchant writes up, then sends
/// the customer an invoice for or turns straight into an order. Items are
/// found by SKU or barcode, typed or scanned.
final class DraftOrder {
    interface Opened {
        void opened(OrderDto order);
    }

    private DraftOrder() {
    }

    static void show(final Runnable changed, final Opened opened) {
        final List<CartLineChangeDto> lines = new ArrayList<CartLineChangeDto>();
        Container body = new Container(BoxLayout.y());
        final TextField email = Ui.field(body, Lang.tr("Customer email"), "draft-email", "",
                TextField.EMAILADDR);
        final Container items = new Container(BoxLayout.y());
        items.setName("draft-items");
        Ui.section(body, "Items");
        body.add(items);
        Container add = new Container(new BorderLayout());
        final TextField code = Scan.field("draft-code", null);
        final TextField qty = new TextField("1", "1", 3, TextField.NUMERIC);
        qty.setUIID("SvField");
        add.add(BorderLayout.CENTER, code);
        add.add(BorderLayout.EAST, qty);
        body.add(add);
        final Runnable addLine = () -> {
            String typed = code.getText().trim();
            if (typed.length() == 0) {
                return;
            }
            Api.inventory().lookup(MerchantState.storeId(), typed, Net.to(hit -> {
                int q = Math.max(1, MUi.integer(qty));
                lines.add(new CartLineChangeDto(hit.variant().id(), q));
                items.add(Ui.row((char) 0, hit.productTitle(), hit.variant().title() + Ui.DOT
                        + q + " x " + MerchantState.money(hit.variant().priceCents()), null));
                code.setText("");
                items.getComponentForm().revalidate();
            }, Ui::fail));
        };
        code.addActionListener(e -> addLine.run());
        body.add(MUi.buttons(Scan.button("draft-scan", code, addLine),
                Ui.secondary(Lang.tr("Add item"), "draft-add", e -> addLine.run())));
        final TextField note = Ui.field(body, Lang.tr("Note"), "draft-note", "");
        Command ok = new com.codename1.ui.Command(Lang.tr("Create draft"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Draft order"), body, cancel, ok) != ok) {
            return;
        }
        if (lines.isEmpty()) {
            Ui.say("Add at least one item");
            return;
        }
        Api.orders().createDraft(MerchantState.storeId(), new DraftOrderDto(email.getText().trim(),
                lines, null, null, note.getText()), Net.to(order -> {
                    changed.run();
                    opened.opened(order);
                }, Ui::fail));
    }
}
