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
package com.codenameone.examples.bazaar.checkout;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.cart.Quoter;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.pay.PaymentProvider;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pay.SimulatedPayments;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Express checkout: Apple Pay and Google Pay through the processor's payment
/// request button, on the product page, the cart and the checkout.
///
/// The wallet sheet is the checkout. The flow, all against the visitor's
/// session cart:
///
/// 1. [#start] prices the cart and opens a held (manual capture) payment
///    intent for it; the browser gets the intent's client secret.
/// 2. As the shopper changes the address or the delivery option in the sheet,
///    [#quote] prices the cart again through the same [Quoter] as the
///    checkout -- shipping options and tax for that address -- and moves the
///    intent to the new amount.
/// 3. The wallet confirms the intent in the browser, and [#confirm] places
///    the order with [Checkouts#place], paid by that hold
///    ([Checkouts.Via#held]). The intent's id is the idempotency key, so a
///    repeated confirm (a retry, a double event) answers the same order.
///
/// The amount is never taken from the browser: the server asks the processor
/// what was held and places the order only when it matches what the cart
/// comes to. Express checkouts charge the store's currency, take no tip and
/// are not offered when the store asks for checkout fields a wallet sheet
/// cannot show (required ones).
@Component
public class ExpressCheckouts {
    static final String OPEN = "OPEN";
    static final String PLACED = "PLACED";

    private final DataSource db;
    private final Quoter quoter;
    private final Checkouts checkouts;
    private final Payments payments;
    private final CheckoutSettings settings;

    public ExpressCheckouts(DataSource db, Quoter quoter, Checkouts checkouts, Payments payments,
            CheckoutSettings settings) {
        this.db = db;
        this.quoter = quoter;
        this.checkouts = checkouts;
        this.payments = payments;
        this.settings = settings;
    }

    /// What the wallet sheet shows: the intent, the amount and its parts,
    /// and the delivery options for the address it was priced at.
    public static final class Session {
        public String id = "";
        public String clientSecret = "";
        public String currency = "";
        public long totalCents;
        public long subtotalCents;
        public long discountCents;
        public long shippingCents;
        public long taxCents;
        public String rateId = "";
        public final List<Option> options = new ArrayList<Option>();
        /// Set once confirmed.
        public OrderDto order;
    }

    /// A delivery option in the sheet.
    public static final class Option {
        public final String id;
        public final String label;
        public final long cents;

        Option(String id, String label, long cents) {
            this.id = id;
            this.label = label;
            this.cents = cents;
        }
    }

    /// Whether express checkout can take this store's orders: a processor
    /// with a browser half, and no required checkout fields.
    public boolean offered(Store store) throws IOException {
        boolean stripe = "stripe".equals(payments.provider().name())
                && payments.provider().publicKey().length() > 0;
        if (!stripe && !(payments.provider() instanceof SimulatedPayments)) {
            return false;
        }
        List<com.codenameone.examples.bazaar.api.CheckoutFieldDto> fields =
                settings.of(store.getId()).fields();
        for (int iter = 0; iter < fields.size(); iter++) {
            if (fields.get(iter).required()) {
                return false;
            }
        }
        return true;
    }

    /// Opens a held intent for `cart` as it is priced now.
    public Session start(String payer, Store store, Cart cart) throws IOException {
        if (!offered(store)) {
            throw new ResponseStatusException(404, "Express checkout is not offered here");
        }
        Quote quote = quoter.price(cart, null, null, null);
        if (quote.lines.isEmpty() || !quote.problems.isEmpty() || quote.dueCents() <= 0) {
            throw new ResponseStatusException(409, quote.problems.isEmpty()
                    ? "The cart is empty" : quote.problems.get(0));
        }
        String id = Ids.next();
        PaymentProvider.Intent intent = payments.provider().createIntent("express-" + id,
                quote.dueCents(), store.getCurrency(), "card", true, "");
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO express_checkout (id, store_id, cart_id, payer, provider_ref, "
                + "amount_cents, currency, rate_id, status, order_id, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {id, store.getId(), cart.getId(), payer, intent.reference,
                    quote.dueCents(), store.getCurrency(), "", OPEN, "", now, now});
        Session s = session(id, quote);
        s.clientSecret = intent.clientSecret;
        return s;
    }

    /// Prices the cart again for the address (country, region, postal code
    /// and city are all a wallet shares before payment) and delivery option
    /// the sheet now shows, and moves the intent to the new amount.
    public Session quote(String payer, String id, Cart cart, AddressDto partial, String rateId)
            throws IOException {
        Map row = row(payer, id, cart);
        if (!OPEN.equals(Rows.text(row.get("status")))) {
            throw new ResponseStatusException(409, "That express checkout is finished");
        }
        Quote quote = price(cart, partial, rateId);
        long due = quote.dueCents();
        if (due != Rows.number(row.get("amount_cents"))) {
            payments.provider().updateIntent(Rows.text(row.get("provider_ref")), due);
        }
        db.execute("UPDATE express_checkout SET amount_cents = ?, rate_id = ?, updated_at = ? "
                + "WHERE id = ?", new Object[] {due, quote.shipping == null ? ""
                    : quote.shipping.id, System.currentTimeMillis(), id});
        return session(id, quote);
    }

    /// Places the order once the wallet confirmed the intent in the browser.
    /// Safe to call again: a second confirm answers the order the first one
    /// placed.
    public Session confirm(String payer, String id, Cart cart, AddressDto address, String email,
            String rateId) throws IOException {
        Map row = row(payer, id, cart);
        String storeId = Rows.text(row.get("store_id"));
        String key = "express-" + id;
        if (PLACED.equals(Rows.text(row.get("status")))) {
            Session done = new Session();
            done.id = id;
            done.order = checkouts.placedBefore(payer, key);
            return done;
        }
        String ref = Rows.text(row.get("provider_ref"));
        PaymentProvider.Intent intent = payments.provider().intent(ref);
        if (intent == null || !PaymentProvider.INTENT_HELD.equals(intent.status)) {
            throw new ResponseStatusException(402, "The wallet did not approve the payment");
        }
        if (email == null || email.indexOf('@') <= 0) {
            throw new ResponseStatusException(400, "The wallet shared no email address");
        }
        OrderDto placed = checkouts.place(payer, storeId, key,
                new PlaceOrderDto(address, rateId, null, email, "", key, 0, null),
                cart, null, null,
                Checkouts.Via.held(ref, intent.amountCents, "Wallet"));
        db.execute("UPDATE express_checkout SET status = ?, order_id = ?, updated_at = ? "
                + "WHERE id = ?", new Object[] {PLACED, placed.id(), System.currentTimeMillis(),
                    id});
        Session s = new Session();
        s.id = id;
        s.order = placed;
        s.currency = placed.currency();
        s.totalCents = placed.totalCents();
        return s;
    }

    /// The development stand-in for a wallet (the simulated processor only):
    /// a demo address and the cheapest delivery, approved at once, then the
    /// same [#quote] and [#confirm] a real wallet goes through.
    public Session simulate(String payer, String id, Cart cart, String email) throws IOException {
        if (!(payments.provider() instanceof SimulatedPayments)) {
            throw new ResponseStatusException(404, "Not found");
        }
        AddressDto demo = new AddressDto("Wallet Shopper", "1 Demo Street", "", "Springfield",
                "IL", "62701", "US", "", 0, 0);
        Session quoted = quote(payer, id, cart, demo, null);
        Map row = row(payer, id, cart);
        ((SimulatedPayments) payments.provider()).complete(Rows.text(row.get("provider_ref")),
                true);
        return confirm(payer, id, cart, demo, email, quoted.rateId);
    }

    private Quote price(Cart cart, AddressDto partial, String rateId) throws IOException {
        if (partial == null || partial.country() == null
                || partial.country().trim().length() != 2) {
            throw new ResponseStatusException(400, "The address needs a two letter country");
        }
        Quote quote = quoter.price(cart, partial, rateId == null || rateId.length() == 0 ? null
                : rateId, null);
        if (quote.shippingRequired() && quote.shipping == null) {
            throw new ResponseStatusException(422, "The store does not deliver there");
        }
        if (!quote.problems.isEmpty()) {
            throw new ResponseStatusException(409, quote.problems.get(0));
        }
        return quote;
    }

    private Map row(String payer, String id, Cart cart) throws IOException {
        Map row = id == null ? null : db.queryOne("SELECT * FROM express_checkout WHERE id = ?",
                new Object[] {id});
        // Another visitor's checkout reads as none at all.
        if (row == null || !Rows.text(row.get("payer")).equals(payer)
                || !Rows.text(row.get("cart_id")).equals(cart.getId())) {
            throw new ResponseStatusException(404, "No such express checkout");
        }
        return row;
    }

    private static Session session(String id, Quote quote) {
        Session s = new Session();
        s.id = id;
        s.currency = quote.store.getCurrency();
        s.totalCents = quote.dueCents();
        s.subtotalCents = quote.subtotalCents;
        s.discountCents = quote.discountCents();
        s.shippingCents = quote.shippingCents;
        s.taxCents = quote.taxCents();
        s.rateId = quote.shipping == null ? "" : quote.shipping.id;
        for (int iter = 0; iter < quote.options.size(); iter++) {
            ShippingCalculator.Option o = quote.options.get(iter);
            s.options.add(new Option(o.id, o.name, o.priceCents));
        }
        return s;
    }
}
