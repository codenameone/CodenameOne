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
package com.codenameone.examples.bazaar.order;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.bazaar.api.TrialDto;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ProductTrial;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.TrialOrder;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ProductTrialRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.TrialOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Try before you buy. A store marks products as trial products; an order
/// with one is placed with its card payment only authorized, shipping does
/// not capture it ([Orders#capturePayment] waits), and [#settle] captures it
/// once the trial is over. A return asked for inside the trial, or a cancel,
/// lets the hold go instead.
///
/// A card authorization lapses after about seven days, so a trial lasts at
/// most [#MAX_DAYS] days, counted from checkout.
@Component
public class Trials {
    /// The longest trial: one day short of a typical card hold's life.
    public static final int MAX_DAYS = 6;
    public static final String OPEN = "OPEN";
    public static final String CAPTURED = "CAPTURED";
    public static final String VOIDED = "VOIDED";
    public static final String CLOSED = "CLOSED";

    private static final String ACTOR = "trial";

    private final ProductTrialRepository products;
    private final TrialOrderRepository trials;
    private final OrderLineRepository lines;
    private final StoreOrderRepository orders;
    private final ProductRepository catalog;
    private final Orders orderService;
    private final StoreAccess access;

    public Trials(ProductTrialRepository products, TrialOrderRepository trials,
            OrderLineRepository lines, StoreOrderRepository orders, ProductRepository catalog,
            Orders orderService, StoreAccess access) {
        this.products = products;
        this.trials = trials;
        this.lines = lines;
        this.orders = orders;
        this.catalog = catalog;
        this.orderService = orderService;
        this.access = access;
    }

    /// The store's trial products.
    public List<TrialDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<TrialDto> out = new ArrayList<TrialDto>();
        for (ProductTrial t : products.findByStoreId(storeId)) {
            out.add(new TrialDto(t.getProductId(), t.getDays()));
        }
        return out;
    }

    /// Offers `productId` for a trial of `days` (1 to [#MAX_DAYS]), or stops
    /// offering it with 0. Answers every trial of the store.
    public List<TrialDto> set(String username, String storeId, String productId, int days)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product p = productId == null ? null : catalog.findById(productId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such product");
        }
        if (days < 0 || days > MAX_DAYS) {
            throw new ResponseStatusException(400, "A trial lasts 1 to " + MAX_DAYS
                    + " days, inside the life of a card authorization");
        }
        ProductTrial kept = products.findById(productId);
        if (days == 0) {
            if (kept != null) {
                products.deleteById(productId);
            }
        } else if (kept == null) {
            ProductTrial t = new ProductTrial();
            t.setProductId(productId);
            t.setStoreId(storeId);
            t.setDays(days);
            products.insert(t);
        } else {
            kept.setDays(days);
            products.save(kept);
        }
        return list(username, storeId);
    }

    /// Starts the trial of a just-placed order whose payment is only
    /// authorized and that holds a trial product; the longest product trial
    /// wins. Does nothing for any other order.
    public void begin(String orderId) throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (order == null || !Orders.AUTHORIZED.equals(order.getFinancial())) {
            return;
        }
        int days = 0;
        for (OrderLine line : lines.findByOrderId(orderId)) {
            ProductTrial t = products.findById(line.getProductId());
            if (t != null && t.getDays() > days) {
                days = t.getDays();
            }
        }
        if (days == 0) {
            return;
        }
        TrialOrder t = new TrialOrder();
        t.setOrderId(orderId);
        t.setStoreId(order.getStoreId());
        t.setStatus(OPEN);
        t.setEndsAt(Instant.now().plus(Duration.ofDays(Math.min(days, MAX_DAYS))));
        trials.insert(t);
        orderService.event(orderId, "TRIAL", "Try before you buy: payment held for "
                + days + " days", ACTOR);
    }

    /// True while `orderId` is in a trial that has not ended, so its held
    /// payment must not be captured yet.
    public static boolean holding(TrialOrder t, Instant now) {
        return t != null && OPEN.equals(t.getStatus()) && t.getEndsAt().isAfter(now);
    }

    /// The shopper sent the goods back inside the trial: the hold is let go
    /// and nothing is charged. Does nothing after the trial ended.
    public void returned(String orderId) throws IOException {
        TrialOrder t = trials.findById(orderId);
        if (!holding(t, Instant.now())) {
            return;
        }
        orderService.voidHeld(orderId, ACTOR);
        t.setStatus(VOIDED);
        trials.save(t);
    }

    /// Captures the payment of every trial that ended.
    @Scheduled(fixedDelay = 600000, initialDelay = 50000, lock = "bazaar-trial-captures")
    public void settle() throws IOException {
        settleDue(Instant.now());
    }

    /// [#settle] as of `now`: a trial whose order is still held is captured;
    /// one cancelled or voided meanwhile is closed. Answers how many it
    /// captured.
    public int settleDue(Instant now) throws IOException {
        int captured = 0;
        for (TrialOrder t : trials.findByStatusAndEndsAtLessThanEqual(OPEN, now)) {
            StoreOrder order = orders.findById(t.getOrderId());
            String financial = order == null ? "" : order.getFinancial();
            if (Orders.AUTHORIZED.equals(financial)) {
                // Closed first: an OPEN trial is what makes capturePayment wait.
                t.setStatus(CLOSED);
                trials.save(t);
                try {
                    financial = orderService.capturePayment(t.getOrderId(), ACTOR);
                } catch (IOException | RuntimeException unavailable) {
                    // The processor could not be asked: try again next run.
                    t.setStatus(OPEN);
                    trials.save(t);
                    continue;
                }
                if (Orders.PAID.equals(financial)) {
                    t.setStatus(CAPTURED);
                    captured++;
                } else {
                    // Not captured (declined, or the processor was down):
                    // left for staff, as any failed capture is.
                    t.setStatus(CLOSED);
                }
            } else {
                t.setStatus(Orders.VOIDED.equals(financial) ? VOIDED : CLOSED);
            }
            trials.save(t);
        }
        return captured;
    }
}
