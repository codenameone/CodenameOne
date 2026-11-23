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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.notify.ShopperLanguage;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.cart.Quoter;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.DiscountUse;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.events.StoreEvents;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.DiscountUseRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Places the signed-in shopper's cart as an order.
///
/// In order, each step undoing the ones before it if it fails:
///
/// 1. **Idempotency.** A key (header or field) names the order: a repeated
///    request finds the order the first one placed and answers it.
/// 2. **Price** the cart for the address and delivery chosen ([Quoter]); a
///    line the stores cannot supply, a code that no longer applies or a gift
///    card that cannot pay stops here.
/// 3. **Reserve** every line ([Inventory#reserve] at [Inventory#pickLocation],
///    or at the pickup location), all or nothing: a line that cannot be had
///    releases those already reserved and answers 409 naming it.
/// 4. **Write** the order in one transaction with the store's next number:
///    the discount uses are counted with a conditional update (so a limited
///    code is never used past its limit), the gift card is debited the same
///    way, and the lines, the gift card tender and the first event go in.
/// 5. **Hold** what is left on the card ([Payments#authorize]), with no
///    transaction open. A decline gives everything back: the gift card is
///    credited, the uses are uncounted, the order is erased and the stock
///    released, so a retry starts clean.
/// 6. **Finish**: the financial status, the card tender, the store's
///    customer record (orders and spend), the cart emptied.
@Component
public class Checkouts {
    /// Field-injected, so the constructor stays as it was.
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.customer.StoreCredit storeCredit;

    private final Carts carts;
    private final Quoter quoter;
    private final Inventory inventory;
    private final Orders orders;
    private final Payments payments;
    private final GiftCards giftCards;
    private final DiscountRepository discounts;
    private final DiscountUseRepository uses;
    private final CustomerRepository customers;
    private final StoreOrderRepository orderRows;
    private final StoreEvents storeEvents;
    private final com.codenameone.examples.bazaar.risk.RiskHolds holds;
    private final com.codenameone.examples.bazaar.risk.Fraud fraud;
    private final com.codenameone.examples.bazaar.order.Trials trials;
    private final com.codenameone.examples.bazaar.taxes.TaxExemptions exemptions;

    private final com.codenameone.examples.bazaar.ops.Attribution attribution;
    private final CheckoutSettings settings;
    private final com.codenameone.examples.bazaar.routing.OrderRouting routing;

    public Checkouts(Carts carts, Quoter quoter, Inventory inventory, Orders orders,
            Payments payments, GiftCards giftCards, DiscountRepository discounts,
            DiscountUseRepository uses, CustomerRepository customers,
            StoreOrderRepository orderRows, StoreEvents storeEvents,
            com.codenameone.examples.bazaar.risk.RiskHolds holds,
            com.codenameone.examples.bazaar.risk.Fraud fraud,
            com.codenameone.examples.bazaar.taxes.TaxExemptions exemptions,
            com.codenameone.examples.bazaar.ops.Attribution attribution,
            CheckoutSettings settings,
            com.codenameone.examples.bazaar.routing.OrderRouting routing,
            com.codenameone.examples.bazaar.order.Trials trials) {
        this.trials = trials;
        this.routing = routing;
        this.attribution = attribution;
        this.settings = settings;
        this.carts = carts;
        this.quoter = quoter;
        this.inventory = inventory;
        this.orders = orders;
        this.payments = payments;
        this.giftCards = giftCards;
        this.discounts = discounts;
        this.uses = uses;
        this.customers = customers;
        this.orderRows = orderRows;
        this.storeEvents = storeEvents;
        this.holds = holds;
        this.fraud = fraud;
        this.exemptions = exemptions;
    }

    /// The `client_id` an idempotency key becomes: per shopper, so two
    /// shoppers sending the same key never meet.
    static String clientId(String username, String key) throws IOException {
        return "k" + Ids.sha256(username + "\n" + key).substring(0, 63);
    }

    public OrderDto place(String username, String storeId, String headerKey, PlaceOrderDto req)
            throws IOException {
        return place(username, storeId, headerKey, req, null, null);
    }

    /// [#place] for the web checkout, whose cart is the visitor's session cart
    /// (found by its token, signed in or not) and whose card may be one typed
    /// for this order alone ([Payments#checkCard]).
    ///
    /// @param username the account, or for a guest a name of the guest's own
    ///     that no account can have (see the web checkout), so the idempotency
    ///     key, the customer record and once-per-customer codes stay theirs
    /// @param given the cart to place, or null for `username`'s own
    /// @param card the card to hold the amount on, or null for
    ///     `req.paymentMethodId()` (or the account's default)
    public OrderDto place(String username, String storeId, String headerKey, PlaceOrderDto req,
            Cart given, Payments.OneOffCard card) throws IOException {
        return place(username, storeId, headerKey, req, given, card, null);
    }

    /// [#place] charged in the shopper's market currency.
    ///
    /// Prices are worked out in the store's currency, as always; the amount
    /// due is then converted at `pricer`'s rate -- the rate the storefront
    /// showed, locked for this order -- and held on the card in the market's
    /// currency. The order keeps both: its totals in the store's currency
    /// (what the books settle in) and the presentment currency, total and
    /// rate (what the shopper paid), so refunds go back in what was charged
    /// (`Orders.presentmentRefund`).
    ///
    /// @param pricer the shopper's market, or null (or one in the store's
    ///     currency) to charge the store's currency
    public OrderDto place(String username, String storeId, String headerKey, PlaceOrderDto req,
            Cart given, Payments.OneOffCard card, Markets.Pricer pricer) throws IOException {
        return place(username, storeId, headerKey, req, given, card, pricer, null);
    }

    /// How an order is paid when it is not a saved or typed card: a wallet's
    /// hold the browser already made (an express checkout), or a payment
    /// method that is not a card, finished in the browser after the order is
    /// placed.
    public static final class Via {
        String heldReference;
        long heldCents;
        String heldLabel = "";
        String methodType;
        /// For a [#method], the intent opened for the order: the pay page
        /// hands its client secret to the processor's script.
        public com.codenameone.examples.bazaar.pay.PaymentProvider.Intent intent;

        private Via() {
        }

        /// A hold of `cents` (in the charge currency) the processor already
        /// made under `reference`. The order is placed only if it comes to
        /// exactly that.
        public static Via held(String reference, long cents, String label) {
            Via v = new Via();
            v.heldReference = reference;
            v.heldCents = cents;
            v.heldLabel = label == null ? "" : label;
            return v;
        }

        /// Paid with processor payment method `type` (`klarna`, `ideal`): the
        /// order is placed as payment pending and the webhook settles it.
        public static Via method(String type) {
            Via v = new Via();
            v.methodType = type;
            return v;
        }
    }

    /// [#place] paid `via` a wallet hold or a payment method that is not a
    /// card; null pays as [#place] always has.
    public OrderDto place(String username, String storeId, String headerKey, PlaceOrderDto req,
            Cart given, Payments.OneOffCard card, Markets.Pricer pricer, Via via)
            throws IOException {
        if (req == null) {
            throw new ResponseStatusException(400, "Say where the order goes");
        }
        String key = headerKey != null && headerKey.trim().length() > 0 ? headerKey.trim()
                : req.idempotencyKey();
        String clientId;
        if (key != null && key.trim().length() > 0) {
            clientId = clientId(username, key.trim());
            OrderDto again = existing(clientId);
            if (again != null) {
                return again;
            }
        } else {
            clientId = Ids.next();
        }
        Cart cart = given != null ? given : carts.cart(storeId, username, null);
        AddressDto address = req.address();
        boolean hasCountry = address != null && address.country() != null
                && address.country().trim().length() == 2;
        Quote quote = quoter.price(cart, hasCountry ? address : null, req.shippingRateId(),
                username);
        // Digital goods only: nothing ships, so neither an address nor a
        // delivery option is asked for. The country, when given, still
        // decides the tax; without one the quote above already stands.
        boolean ships = quote.shippingRequired();
        if (!ships && !hasCountry) {
            address = new AddressDto(address == null ? "" : address.name(), "", "", "", "", "",
                    "", address == null ? "" : address.phone(), 0, 0);
        }
        if (ships && !hasCountry) {
            throw new ResponseStatusException(400, "The address needs a two letter country");
        }
        if (quote.lines.isEmpty()) {
            throw new ResponseStatusException(400, "The cart is empty");
        }
        if (!quote.problems.isEmpty()) {
            throw new ResponseStatusException(409, quote.problems.get(0));
        }
        if (ships && quote.shipping == null) {
            throw new ResponseStatusException(400, "Choose how the order is delivered");
        }
        boolean pickup = ships && ShippingCalculator.PICKUP.equals(quote.shipping.rate.getKind());
        if (ships && !pickup && (empty(address.name()) || empty(address.line1())
                || empty(address.city()))) {
            throw new ResponseStatusException(400, "The address needs a name, a street and a city");
        }
        // The tip and the checkout fields, both checked against the store's
        // settings before anything is reserved. The tip is a share of the
        // goods after discounts, outside the total, and never taxed.
        com.codenameone.examples.bazaar.api.CheckoutSettingsDto rules = settings.of(storeId);
        final long tip = CheckoutSettings.tip(rules, req.tip(),
                quote.subtotalCents - quote.discountCents());
        final List<com.codenameone.examples.bazaar.api.OrderFieldDto> answers =
                CheckoutSettings.answers(rules, req.fields());
        String payCurrency = pricer != null ? pricer.currency() : quote.store.getCurrency();
        if (via != null && via.methodType != null && (!rules.methods().contains(via.methodType)
                || !com.codenameone.examples.bazaar.pay.PaymentTypes.allowed(via.methodType,
                    payCurrency, hasCountry ? address.country().trim() : ""))) {
            throw new ResponseStatusException(400, "That payment method is not offered here");
        }
        final StoreOrder order = orders.newOrder(quote, Orders.ONLINE, username,
                empty(req.email()) ? username : req.email(), address, clientId);
        order.setSource(given != null ? Orders.SOURCE_WEB : Orders.SOURCE_APP);
        order.setNote(Codes.cut(req.note(), 1000));
        order.setTipCents(tip);
        // The language its mail goes out in: the storefront's, or what the
        // app or API client asked for (Accept-Language), else the cart's.
        String language = ShopperLanguage.fromRequest();
        order.setLanguage(language.length() > 0 ? language : cart.getLanguage());
        String orderId = order.getId();

        // Reserve, all or nothing.
        final String[] at = new String[quote.lines.size()];
        final int[] back = new int[quote.lines.size()];
        List<String[]> held = new ArrayList<String[]>();
        // Where each shipped line leaves from: the store's routing rules.
        String[] routed = new String[quote.lines.size()];
        if (!pickup) {
            String[] vs = new String[quote.lines.size()];
            int[] qs = new int[quote.lines.size()];
            for (int iter = 0; iter < vs.length; iter++) {
                Quote.Line line = quote.lines.get(iter);
                vs[iter] = line.preorder || line.variant.isDigital() ? null
                        : line.variant.getId();
                qs[iter] = line.price.quantity;
            }
            boolean point = address != null
                    && com.codenameone.examples.bazaar.geo.Geo.isPoint(address.lat(),
                            address.lng());
            com.codenameone.examples.bazaar.routing.Router.Plan plan = routing.plan(storeId, vs,
                    qs, point, point ? address.lat() : 0, point ? address.lng() : 0);
            routed = plan.locations;
        }
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line line = quote.lines.get(iter);
            if (line.preorder || line.variant.isDigital()) {
                // A download holds no stock. Sold ahead of stock: there is nothing on hand to hold, so
                // the line is placed with no location and reserves nothing;
                // Quoter#preorderAllowance counts it against the allowance.
                continue;
            }
            String location = pickup ? quote.shipping.rate.getLocationId()
                    : routed[iter];
            int over = location == null ? -1 : inventory.reserveCounted(storeId,
                    line.variant.getId(), location, line.price.quantity, orderId, username);
            if (over < 0) {
                orders.releaseAll(storeId, held, orderId, username);
                throw new ResponseStatusException(409, "Not enough " + line.product.getTitle()
                        + (line.variant.getTitle() == null || line.variant.getTitle().length() == 0
                            ? "" : " (" + line.variant.getTitle() + ")") + " in stock");
            }
            at[iter] = location;
            back[iter] = over;
            held.add(new String[] {line.cartLineId, line.variant.getId(), location,
                Integer.toString(line.price.quantity)});
        }

        final Quote q = quote;
        final String who = username;
        StoreOrder placed;
        try {
            placed = orders.insertNumbered(order, new Orders.Writer() {
                @Override
                public void write(StoreOrder o) throws IOException {
                    claimDiscounts(o, q, who);
                    if (q.giftCard != null && q.giftCardCents > 0) {
                        if (!giftCards.redeem(q.giftCard.getId(), o.getId(), q.giftCardCents)) {
                            throw new ResponseStatusException(409,
                                    "The gift card's balance changed; look at the cart again");
                        }
                        orders.tender(o.getId(), "GIFT_CARD", q.giftCardCents,
                                q.giftCard.getLastFour());
                    }
                    if (q.creditCustomer != null && q.storeCreditCents > 0) {
                        if (!storeCredit.take(q.creditCustomer.getId(), q.storeCreditCents,
                                o.getId())) {
                            throw new ResponseStatusException(409,
                                    "Your store credit changed; look at the cart again");
                        }
                        orders.tender(o.getId(), "STORE_CREDIT", q.storeCreditCents, "");
                    }
                    orders.insertLines(o, q, at, back);
                    settings.keep(o.getId(), o.getStoreId(), answers);
                    orders.event(o.getId(), "PLACED", "Order placed", who);
                }
            });
        } catch (IOException | RuntimeException failed) {
            orders.releaseAll(storeId, held, orderId, username);
            throw failed;
        }
        if (placed == null) {
            // The same key placed it meanwhile.
            orders.releaseAll(storeId, held, orderId, username);
            OrderDto again = existing(clientId);
            if (again != null) {
                return again;
            }
            throw new ResponseStatusException(409, "The order is being placed; try again");
        }

        // The tip rides on what the card pays, beside the total.
        long due = quote.dueCents() + tip;
        String reference = "";
        boolean pending = false;
        String chargeCurrency = quote.store.getCurrency();
        long charge = due;
        long rate = 0;
        long presentmentTotal = 0;
        if (pricer != null && !pricer.currency().equals(quote.store.getCurrency())) {
            rate = pricer.rateMicros();
            chargeCurrency = pricer.currency();
            charge = atRate(due, rate);
            presentmentTotal = atRate(quote.totalCents, rate);
        }
        if (due > 0) {
            String method = empty(req.paymentMethodId()) ? payments.defaultMethod(username)
                    : req.paymentMethodId();
            try {
                if (via != null && via.heldReference != null) {
                    if (charge != via.heldCents) {
                        // The wallet approved a different amount than the
                        // order comes to (the cart changed after the last
                        // re-quote): never place it on a hold that does not
                        // match.
                        throw new ResponseStatusException(409,
                                "The total changed; look at the order again");
                    }
                    reference = payments.keepHeld(orderId, username, via.heldReference, charge,
                            chargeCurrency, via.heldLabel);
                } else if (via != null && com.codenameone.examples.bazaar.pay.PaymentTypes.manual(via.methodType)) {
                    // Cash on delivery or a bank transfer: placed unpaid,
                    // and the merchant marks it paid when the money arrives.
                    reference = "";
                    pending = true;
                } else if (via != null && via.methodType != null) {
                    via.intent = payments.startIntent(orderId, username, via.methodType, charge,
                            chargeCurrency);
                    reference = via.intent.reference;
                    pending = true;
                } else {
                    reference = card != null
                            ? payments.authorizeCard(orderId, username, card, charge,
                                chargeCurrency)
                            : payments.authorize(orderId, username, method, charge,
                                chargeCurrency);
                }
            } catch (IOException | RuntimeException failed) {
                // The fraud score counts refused payments; the order itself is erased.
                holds.recordFailure(storeId, username);
                undo(quote, orderId, username);
                orders.releaseAll(storeId, held, orderId, username);
                throw failed;
            }
        }
        StoreOrder done = finish(orderId, quote, due, reference, username, pending);
        if (rate > 0) {
            done = presentment(orderId, chargeCurrency, presentmentTotal, rate,
                    pricer.source());
        }
        if (quote.exemptCertificate != null) {
            exemptions.recordUse(storeId, orderId, quote.exemptCertificate);
        }
        fraud.assess(orderId, username);
        trials.begin(orderId);
        attribution.stampOrder(storeId, cart.getId(), orderId);
        carts.clear(cart.getId());
        orders.publish(done);
        storeEvents.orderPlaced(orderId);
        return orders.detailOf(orderId);
    }

    /// The order `username` already placed with idempotency key `key`, or
    /// null: what a repeated submit of a checkout page answers, before it
    /// looks at a cart that placing the order has emptied.
    public OrderDto placedBefore(String username, String key) throws IOException {
        if (key == null || key.trim().length() == 0) {
            return null;
        }
        return existing(clientId(username, key.trim()));
    }

    private static boolean empty(String text) {
        return text == null || text.trim().length() == 0;
    }

    @Transactional(readOnly = true)
    private OrderDto existing(String clientId) throws IOException {
        StoreOrder found = orderRows.findByClientId(clientId);
        return found == null ? null : orders.detailOf(found.getId());
    }

    /// Counts a use of every discount the quote applied, refusing one that
    /// ran out or that this shopper already used once, and records who used
    /// it. Runs inside the order's transaction.
    private void claimDiscounts(StoreOrder order, Quote quote, String username)
            throws IOException {
        if (quote.discounts == null) {
            return;
        }
        for (int iter = 0; iter < quote.discounts.applied.size(); iter++) {
            DiscountEngine.Applied a = quote.discounts.applied.get(iter);
            Discount d = a.discount;
            if (d.isOncePerCustomer() && uses.countByDiscountIdAndUsername(d.getId(), username) > 0) {
                throw new ResponseStatusException(409, "You already used " + label(d));
            }
            if (discounts.useIf(d.getId()) != 1) {
                throw new ResponseStatusException(409, label(d) + " has been used up");
            }
            DiscountUse use = new DiscountUse();
            use.setId(Ids.next());
            use.setDiscountId(d.getId());
            use.setUsername(username);
            use.setOrderId(order.getId());
            use.setCreatedAt(Instant.now());
            uses.insert(use);
        }
    }

    private static String label(Discount d) {
        return d.getCode() != null && d.getCode().length() > 0 ? d.getCode() : d.getTitle();
    }

    /// Gives back what [#claimDiscounts] and the gift card took, and erases
    /// the order: its card was declined.
    private void undo(Quote quote, String orderId, String username) throws IOException {
        if (quote.giftCard != null && quote.giftCardCents > 0) {
            giftCards.credit(quote.giftCard.getId(), orderId, quote.giftCardCents,
                    "Payment declined");
        }
        if (quote.storeCreditCents > 0) {
            storeCredit.restore(orderId, quote.storeCreditCents);
        }
        undoDiscounts(orderId);
        orders.erase(orderId);
    }

    @Transactional(rollbackFor = IOException.class)
    private void undoDiscounts(String orderId) throws IOException {
        List<DiscountUse> rows = uses.findByOrderId(orderId);
        for (int iter = 0; iter < rows.size(); iter++) {
            discounts.unuse(rows.get(iter).getDiscountId());
        }
        uses.deleteByOrderId(orderId);
    }

    /// Store cents at a rate in millionths, half up; a total too large is a 400.
    public static long atRate(long cents, long rateMicros) {
        try {
            return Math.addExact(Math.multiplyExact(cents, rateMicros), 500000L) / 1000000L;
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(400, "That order is too large to convert");
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder presentment(String orderId, String currency, long total, long rate,
            String origin)
            throws IOException {
        StoreOrder order = orderRows.findById(orderId);
        order.setPresentmentCurrency(currency);
        order.setPresentmentTotalCents(total);
        order.setFxRateMicros(rate);
        order.setFxSource(origin);
        orderRows.save(order);
        return order;
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder finish(String orderId, Quote quote, long due, String reference,
            String username, boolean pending) throws IOException {
        StoreOrder order = orderRows.findById(orderId);
        if (pending) {
            // Paid in the browser after this: the webhook settles it.
            order.setFinancial(Orders.PENDING);
        } else if (due <= 0) {
            order.setFinancial(Orders.PAID);
        } else if (Payments.CASH.equals(reference)) {
            // Paid on pickup or delivery: the money is taken by hand.
            order.setFinancial(Orders.PENDING);
        } else {
            order.setFinancial(Orders.AUTHORIZED);
            orders.tender(orderId, "CARD", due, reference);
        }
        Customer customer = customers.findByStoreIdAndUsername(order.getStoreId(), username);
        boolean fresh = customer == null;
        if (fresh) {
            customer = new Customer();
            customer.setId(Ids.next());
            customer.setStoreId(order.getStoreId());
            customer.setUsername(username);
            customer.setEmail(order.getEmail());
            customer.setPhone(order.getShipPhone());
            String name = order.getShipName();
            int space = name.lastIndexOf(' ');
            customer.setFirstName(Codes.cut(space > 0 ? name.substring(0, space) : name, 80));
            customer.setLastName(Codes.cut(space > 0 ? name.substring(space + 1) : "", 80));
            customer.setCreatedAt(Instant.now());
        }
        if (order.getLanguage().length() > 0) {
            customer.setLanguage(order.getLanguage());
        }
        customer.setOrdersCount(customer.getOrdersCount() + 1);
        customer.setSpentCents(customer.getSpentCents() + order.getTotalCents());
        customer.setUpdatedAt(Instant.now());
        if (fresh) {
            customers.insert(customer);
        } else {
            customers.save(customer);
        }
        order.setCustomerId(customer.getId());
        order.setUpdatedAt(Instant.now());
        orderRows.save(order);
        boolean held = Orders.AUTHORIZED.equals(order.getFinancial());
        orders.event(orderId, pending ? "PAYMENT_PENDING" : held ? "AUTHORIZED" : "PAYMENT",
                pending ? "Payment pending" : held ? "Payment held on the card"
                    : "Payment recorded", username);
        return order;
    }
}
