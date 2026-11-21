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

    public Checkouts(Carts carts, Quoter quoter, Inventory inventory, Orders orders,
            Payments payments, GiftCards giftCards, DiscountRepository discounts,
            DiscountUseRepository uses, CustomerRepository customers,
            StoreOrderRepository orderRows, StoreEvents storeEvents) {
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
        if (address == null || address.country() == null || address.country().trim().length() != 2) {
            throw new ResponseStatusException(400, "The address needs a two letter country");
        }
        Quote quote = quoter.price(cart, address, req.shippingRateId(), username);
        if (quote.lines.isEmpty()) {
            throw new ResponseStatusException(400, "The cart is empty");
        }
        if (!quote.problems.isEmpty()) {
            throw new ResponseStatusException(409, quote.problems.get(0));
        }
        if (quote.shipping == null) {
            throw new ResponseStatusException(400, "Choose how the order is delivered");
        }
        boolean pickup = ShippingCalculator.PICKUP.equals(quote.shipping.rate.getKind());
        if (!pickup && (empty(address.name()) || empty(address.line1()) || empty(address.city()))) {
            throw new ResponseStatusException(400, "The address needs a name, a street and a city");
        }
        final StoreOrder order = orders.newOrder(quote, Orders.ONLINE, username,
                empty(req.email()) ? username : req.email(), address, clientId);
        order.setNote(Codes.cut(req.note(), 1000));
        String orderId = order.getId();

        // Reserve, all or nothing.
        final String[] at = new String[quote.lines.size()];
        List<String[]> held = new ArrayList<String[]>();
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line line = quote.lines.get(iter);
            String location = pickup ? quote.shipping.rate.getLocationId()
                    : inventory.pickLocation(storeId, line.variant.getId(), line.price.quantity);
            if (location == null || !inventory.reserve(storeId, line.variant.getId(), location,
                    line.price.quantity, orderId, username)) {
                orders.releaseAll(storeId, held, orderId, username);
                throw new ResponseStatusException(409, "Not enough " + line.product.getTitle()
                        + (line.variant.getTitle() == null || line.variant.getTitle().length() == 0
                            ? "" : " (" + line.variant.getTitle() + ")") + " in stock");
            }
            at[iter] = location;
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
                    orders.insertLines(o, q, at);
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

        long due = quote.dueCents();
        String reference = "";
        if (due > 0) {
            String method = empty(req.paymentMethodId()) ? payments.defaultMethod(username)
                    : req.paymentMethodId();
            try {
                reference = card != null
                        ? payments.authorizeCard(orderId, username, card, due,
                            quote.store.getCurrency())
                        : payments.authorize(orderId, username, method, due,
                            quote.store.getCurrency());
            } catch (IOException | RuntimeException failed) {
                undo(quote, orderId, username);
                orders.releaseAll(storeId, held, orderId, username);
                throw failed;
            }
        }
        StoreOrder done = finish(orderId, quote, due, reference, username);
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

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder finish(String orderId, Quote quote, long due, String reference,
            String username) throws IOException {
        StoreOrder order = orderRows.findById(orderId);
        if (due <= 0) {
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
        orders.event(orderId, Orders.AUTHORIZED.equals(order.getFinancial()) ? "AUTHORIZED"
                : "PAYMENT", Orders.AUTHORIZED.equals(order.getFinancial())
                    ? "Payment held on the card" : "Payment recorded", username);
        return order;
    }
}
