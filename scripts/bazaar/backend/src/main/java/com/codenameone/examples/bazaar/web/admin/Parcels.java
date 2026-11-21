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
package com.codenameone.examples.bazaar.web.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.web.Money;

import java.io.IOException;
import java.util.List;

/// The fulfilment half of the back office, over [Fulfillments]: the parcels
/// on the order page with their pack, label and ship steps, and the board
/// whose columns follow the orders through. The rules -- what a parcel may do
/// next, which role may do it, what a board move means -- are the service's;
/// this turns its DTOs into rows and decides only which buttons to offer.
@Component
public class Parcels {
    private final Fulfillments fulfillments;

    public Parcels(Fulfillments fulfillments) {
        this.fulfillments = fulfillments;
    }

    /// Fills the order page's parcels. A role that may read the order but not
    /// work its parcels (a cashier) sees them without their buttons.
    void fill(String storeId, String orderId, AdminView v, String currency) throws IOException {
        String me = Caller.name();
        List<FulfillmentDto> fs = fulfillments.ofOrder(me, storeId, orderId);
        String base = "/admin/" + storeId + "/orders/" + orderId;
        v.slipUrl = base + "/packing-slip.pdf";
        for (int iter = 0; iter < fs.size(); iter++) {
            FulfillmentDto f = fs.get(iter);
            ParcelView p = new ParcelView();
            p.id = f.id();
            p.title = "Parcel " + (iter + 1);
            p.status = AdminData.words(f.status());
            p.tone = AdminData.tone(Fulfillments.DELIVERED.equals(f.status()) ? "FULFILLED"
                    : f.status());
            StringBuilder items = new StringBuilder();
            List<FulfilLineDto> ls = f.lines();
            for (int k = 0; ls != null && k < ls.size(); k++) {
                if (k > 0) {
                    items.append(", ");
                }
                items.append(ls.get(k).quantity()).append(" x ").append(ls.get(k).title());
            }
            p.detail = items.toString();
            p.tracking = f.trackingNumber() == null ? "" : f.trackingNumber();
            p.hasLabel = p.tracking.length() > 0;
            String parcel = base + "/parcels/" + f.id();
            p.slipUrl = base + "/packing-slip.pdf?fulfillment=" + f.id();
            p.labelUrl = parcel + "/label.pdf";
            p.packAction = parcel + "/pack";
            p.labelAction = parcel + "/label";
            p.shipAction = parcel + "/ship";
            boolean ship = Fulfillments.SHIP.equals(f.method());
            boolean open = Fulfillments.PENDING.equals(f.status())
                    || Fulfillments.PACKED.equals(f.status());
            p.canPack = Fulfillments.PENDING.equals(f.status());
            if (ship && open && !p.hasLabel) {
                try {
                    List<RateQuoteDto> quotes = fulfillments.rates(me, storeId, f.id());
                    int cheapest = -1;
                    for (int q = 0; q < quotes.size(); q++) {
                        if (cheapest < 0 || quotes.get(q).cents() < quotes.get(cheapest).cents()) {
                            cheapest = q;
                        }
                    }
                    for (int q = 0; q < quotes.size(); q++) {
                        RateQuoteDto r = quotes.get(q);
                        AdminRow row = new AdminRow();
                        row.id = r.service();
                        row.title = r.title() + " - " + Money.format(r.cents(), currency) + ", "
                                + r.days() + (r.days() == 1 ? " day" : " days");
                        row.flag = q == cheapest;
                        p.rates.add(row);
                    }
                    p.canLabel = !p.rates.isEmpty();
                } catch (ResponseStatusException notForThisRole) {
                    if (notForThisRole.getStatus() != 403) {
                        throw notForThisRole;
                    }
                }
            }
            p.canShip = ship && open && p.hasLabel;
            v.parcels.add(p);
        }
    }

    /// The board, columns left to right, each card a form that moves it on.
    void board(String storeId, MarketingView v) throws IOException {
        List<BoardColumnDto> cols = fulfillments.board(Caller.name(), storeId);
        String move = "/admin/" + storeId + "/fulfilment/move";
        for (int iter = 0; iter < cols.size(); iter++) {
            BoardColumnDto c = cols.get(iter);
            BoardColumn col = new BoardColumn();
            col.id = c.id();
            col.title = c.title();
            col.count = Integer.toString(c.cards().size());
            if (iter + 1 < cols.size()) {
                col.next = cols.get(iter + 1).id();
                col.nextLabel = "Move to " + cols.get(iter + 1).title();
            }
            for (int k = 0; k < c.cards().size(); k++) {
                OrderCardDto o = c.cards().get(k);
                AdminRow r = new AdminRow();
                r.id = o.orderId();
                r.title = "#" + o.number();
                r.detail = o.customer() == null ? "" : o.customer();
                r.meta = o.items() + (o.items() == 1 ? " item" : " items") + ", "
                        + AdminData.date(o.createdAt());
                r.amount = Money.format(o.totalCents(), o.currency());
                r.sku = o.trackingNumber() == null ? "" : o.trackingNumber();
                r.url = "/admin/" + storeId + "/orders/" + o.orderId();
                r.action = move;
                col.cards.add(r);
            }
            v.columns.add(col);
        }
    }
}
