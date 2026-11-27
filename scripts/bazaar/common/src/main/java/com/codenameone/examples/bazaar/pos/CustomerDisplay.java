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

import com.codename1.ui.Container;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Windows;

/// The screen that faces the customer: a second window (desktop only) that
/// mirrors the cart and the total as the cashier rings it up.
final class CustomerDisplay {
    private static Window window;
    private static Container body;

    private CustomerDisplay() {
    }

    static void toggle() {
        if (window != null) {
            window.dispose();
            window = null;
            body = null;
            return;
        }
        body = new Container(BoxLayout.y());
        body.setUIID("PosDisplay");
        body.setName("pos-customer-display");
        Container wrap = new Container(new BorderLayout());
        wrap.add(BorderLayout.CENTER, body);
        update();
        window = Windows.open("Your order", wrap, 160, 110);
        if (window == null) {
            body = null;
        }
    }

    static void update() {
        if (body == null) {
            return;
        }
        body.removeAll();
        PosCart cart = PosState.cart();
        body.add(PosUi.data(PosState.storeName(), "PosDisplayStore"));
        if (cart.isEmpty()) {
            body.add(PosUi.label("Welcome", "PosDisplayTotal"));
        } else {
            long[] units = cart.unitPrices();
            for (int iter = 0; iter < cart.items().size(); iter++) {
                PosCart.Item i = cart.items().get(iter);
                Container row = new Container(new BorderLayout());
                row.add(BorderLayout.WEST, PosUi.data(i.quantity + " x " + i.item.title(),
                        "PosDisplayLine"));
                row.add(BorderLayout.EAST, PosUi.data(PosState.money(units[iter] * i.quantity),
                        "PosDisplayLine"));
                body.add(row);
            }
            body.add(PosUi.data(Lang.tr("Total {0}", PosState.money(PosState.totals().totalCents)),
                    "PosDisplayTotal"));
        }
        body.revalidateLater();
    }
}
