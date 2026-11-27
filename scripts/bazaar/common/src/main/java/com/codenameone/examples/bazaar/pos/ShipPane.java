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

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosShipOptionDto;
import com.codenameone.examples.bazaar.api.PosShipQuoteDto;
import com.codenameone.examples.bazaar.api.PosShipSaleDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// The sale on the register shipped to the customer from this location:
/// their address, a delivery option with the total it makes, and payment.
/// Online only: the server quotes the shipping and holds the stock.
final class ShipPane {
    private ShipPane() {
    }

    static com.codename1.ui.Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-ship");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Ship to customer", null));
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        root.add(BorderLayout.CENTER, page);
        form(page);
        return root;
    }

    private static void form(final Container page) {
        page.removeAll();
        if (PosState.cart().items().isEmpty()) {
            page.add(PosUi.label("Ring up the items first, then ship them here", "PosMuted"));
            page.revalidateLater();
            return;
        }
        PosCustomerDto cu = PosState.cart().customer();
        final TextField name = Ui.field(page, Lang.tr("Name"), "pos-ship-name", "");
        final TextField email = Ui.field(page, Lang.tr("E-mail"), "pos-ship-email", "");
        final TextField line1 = Ui.field(page, Lang.tr("Street"), "pos-ship-line1", "");
        final TextField city = Ui.field(page, Lang.tr("City"), "pos-ship-city", "");
        final TextField region = Ui.field(page, Lang.tr("State or region"), "pos-ship-region", "");
        final TextField postal = Ui.field(page, Lang.tr("Postal code"), "pos-ship-postal", "");
        final TextField country = Ui.field(page, Lang.tr("Country code"), "pos-ship-country",
                "US");
        if (cu != null) {
            email.setText(cu.email() == null ? "" : cu.email());
        }
        final Container options = new Container(BoxLayout.y());
        page.add(PosUi.key(Lang.tr("Delivery options"), "pos-ship-quote", "PosChoice", e -> {
            final AddressDto a = new AddressDto(name.getText().trim(), line1.getText().trim(), "",
                    city.getText().trim(), region.getText().trim(), postal.getText().trim(),
                    country.getText().trim(), "", 0, 0);
            options.removeAll();
            Api.posCounter().shipQuote(PosState.storeId(), PosState.registerId(),
                    new PosShipQuoteDto(PosState.cart().uploadLines(), a), Net.to(list -> {
                        if (list == null || list.isEmpty()) {
                            options.add(PosUi.label("The store does not deliver to that address",
                                    "PosMuted"));
                        }
                        for (int iter = 0; list != null && iter < list.size(); iter++) {
                            options.add(option(page, list.get(iter), a, email.getText().trim(),
                                    iter));
                        }
                        page.revalidateLater();
                    }, Ui::fail));
        }));
        page.add(options);
        page.revalidateLater();
    }

    private static Container option(final Container page, final PosShipOptionDto o,
            final AddressDto a, final String email, int index) {
        Container c = new Container(BoxLayout.y());
        c.setUIID("PosCartRow");
        c.add(PosUi.data(o.name() + Ui.DOT + PosState.money(o.shippingCents()), "PosCartTitle"));
        c.add(PosUi.data(Lang.tr("Total {0}", PosState.money(o.totalCents())), "PosCartDetail"));
        Container pay = new Container(new GridLayout(1, 2));
        pay.add(PosUi.icon(FontImage.MATERIAL_PAYMENTS, "Cash", "pos-ship-cash-" + index,
                "PosChoice", e -> sell(page, o, a, email, "CASH")));
        pay.add(PosUi.icon(FontImage.MATERIAL_CREDIT_CARD, "Card", "pos-ship-card-" + index,
                "PosChoice", e -> sell(page, o, a, email, "CARD")));
        c.add(pay);
        return c;
    }

    private static void sell(final Container page, PosShipOptionDto o, AddressDto a,
            String email, String kind) {
        List<PosTenderDto> tenders = new ArrayList<PosTenderDto>();
        tenders.add(new PosTenderDto(kind, o.totalCents(), ""));
        PosCustomerDto cu = PosState.cart().customer();
        PosShipSaleDto sale = new PosShipSaleDto(UUID.randomUUID().toString(), PosState.shiftId(),
                cu == null ? "" : cu.id(), email, PosState.cart().note(),
                PosState.cart().uploadLines(), tenders, PosState.staffUsername(),
                PosState.cart().approvalId(), a, o.rateId());
        Api.posCounter().shipSale(PosState.storeId(), PosState.registerId(), sale, Net.to(r -> {
            if (!"OK".equals(r.status())) {
                Ui.say(r.message());
                return;
            }
            PosState.cart().clear();
            PosState.fireCart();
            page.removeAll();
            page.add(PosUi.data(Lang.tr("Order #{0}", r.number()), "PosAmount"));
            page.add(PosUi.label("Paid; the order ships from this location", "PosMuted"));
            page.revalidateLater();
        }, Ui::fail));
    }
}
