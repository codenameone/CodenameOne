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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.PosPickupDto;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ShippingRateRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The register's pickup queue: online orders the shopper chose to collect
/// at the register's location (a PICKUP rate of that location), oldest
/// first, until they are collected.
///
/// Making one ready turns its open lines into a pickup fulfilment at the
/// location and marks it packed, which mails and notifies the shopper
/// ([Fulfillments#pickupReadyAtCounter]); handing it over completes it. The
/// fulfilment is the same one the back office sees, so either side can move it.
@Component
public class Pickups {
    private final StoreAccess access;
    private final Pos pos;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final ShippingRateRepository rates;
    private final FulfillmentRepository fulfillmentRows;
    private final Fulfillments fulfillments;

    public Pickups(StoreAccess access, Pos pos, StoreOrderRepository orders,
            OrderLineRepository lines, ShippingRateRepository rates,
            FulfillmentRepository fulfillmentRows, Fulfillments fulfillments) {
        this.access = access;
        this.pos = pos;
        this.orders = orders;
        this.lines = lines;
        this.rates = rates;
        this.fulfillmentRows = fulfillmentRows;
        this.fulfillments = fulfillments;
    }

    /// Pickups at the register's location not yet collected, oldest first.
    @Transactional(readOnly = true)
    public List<PosPickupDto> queue(String username, String storeId, String registerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register register = pos.register(storeId, registerId);
        Set<String> here = pickupRates(storeId, register.getLocationId());
        List<PosPickupDto> out = new ArrayList<PosPickupDto>();
        if (here.isEmpty()) {
            return out;
        }
        List<StoreOrder> rows = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = rows.size() - 1; iter >= 0; iter--) {
            StoreOrder o = rows.get(iter);
            if (!Orders.OPEN.equals(o.getStatus()) || !here.contains(o.getShippingRateId())
                    || Orders.POS.equals(o.getChannel())) {
                continue;
            }
            Fulfillment f = openPickup(o.getId());
            if (f == null && Orders.FULFILLED.equals(o.getFulfilment())) {
                continue;
            }
            out.add(dto(o, f));
        }
        return out;
    }

    /// Makes an order ready to collect: a pickup of its open lines at the
    /// register's location, packed, and the shopper told.
    public PosPickupDto ready(String username, String storeId, String orderId)
            throws IOException {
        StoreOrder o = pickupIn(username, storeId, orderId);
        Fulfillment f = openPickup(o.getId());
        String id;
        if (f == null) {
            List<FulfilLineDto> wanted = new ArrayList<FulfilLineDto>();
            List<OrderLine> ls = lines.findByOrderId(o.getId());
            for (int iter = 0; iter < ls.size(); iter++) {
                OrderLine l = ls.get(iter);
                int open = l.getQuantity() - l.getFulfilledQty();
                if (open > 0) {
                    wanted.add(new FulfilLineDto(l.getId(), l.getTitle(), "", open));
                }
            }
            if (wanted.isEmpty()) {
                throw new ResponseStatusException(409, "The order has nothing left to collect");
            }
            id = fulfillments.createPickupAtCounter(username, storeId, o.getId(),
                    new NewFulfillmentDto(rateLocation(storeId, o), Fulfillments.PICKUP, wanted))
                    .id();
        } else {
            id = f.getId();
        }
        fulfillments.pickupReadyAtCounter(username, storeId, id);
        return dto(orders.findById(o.getId()), fulfillmentRows.findById(id));
    }

    /// Hands a ready order over.
    public PosPickupDto pickedUp(String username, String storeId, String orderId)
            throws IOException {
        StoreOrder o = pickupIn(username, storeId, orderId);
        Fulfillment f = openPickup(o.getId());
        if (f == null || !Fulfillments.PACKED.equals(f.getStatus())) {
            throw new ResponseStatusException(409, "Make the order ready first");
        }
        fulfillments.pickedUp(username, storeId, f.getId());
        return dto(orders.findById(o.getId()), fulfillmentRows.findById(f.getId()));
    }

    @Transactional(readOnly = true)
    private StoreOrder pickupIn(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || !storeId.equals(o.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        if (rateLocation(storeId, o).length() == 0) {
            throw new ResponseStatusException(409, "That order is not a pickup");
        }
        return o;
    }

    /// The location of the order's pickup rate, or empty when it has none.
    private String rateLocation(String storeId, StoreOrder o) {
        ShippingRate r = o.getShippingRateId().length() == 0 ? null
                : rates.findById(o.getShippingRateId());
        return r == null || !storeId.equals(r.getStoreId())
                || !com.codenameone.examples.bazaar.pricing.ShippingCalculator.PICKUP.equals(
                    r.getKind()) ? "" : r.getLocationId();
    }

    private Set<String> pickupRates(String storeId, String locationId) {
        Set<String> out = new HashSet<String>();
        List<ShippingRate> all = rates.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            ShippingRate r = all.get(iter);
            if (com.codenameone.examples.bazaar.pricing.ShippingCalculator.PICKUP.equals(
                    r.getKind()) && locationId.equals(r.getLocationId())) {
                out.add(r.getId());
            }
        }
        return out;
    }

    /// The order's pickup fulfilment still waiting (PENDING or PACKED), or null.
    private Fulfillment openPickup(String orderId) {
        List<Fulfillment> fs = fulfillmentRows.findByOrderId(orderId);
        for (int iter = 0; iter < fs.size(); iter++) {
            Fulfillment f = fs.get(iter);
            if (Fulfillments.PICKUP.equals(f.getCarrier())
                    && (Fulfillments.PENDING.equals(f.getStatus())
                    || Fulfillments.PACKED.equals(f.getStatus()))) {
                return f;
            }
        }
        return null;
    }

    private PosPickupDto dto(StoreOrder o, Fulfillment f) {
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        int units = 0;
        StringBuilder items = new StringBuilder();
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine l = ls.get(iter);
            units += l.getQuantity();
            if (items.length() < 200) {
                items.append(iter == 0 ? "" : ", ").append(l.getQuantity()).append(" x ")
                        .append(l.getTitle());
            }
        }
        String status = f == null ? (Orders.FULFILLED.equals(o.getFulfilment())
                ? Fulfillments.PICKED_UP : Fulfillments.PENDING) : f.getStatus();
        String who = o.getShipName().length() > 0 ? o.getShipName() : o.getEmail();
        return new PosPickupDto(f == null ? "" : f.getId(), o.getId(), o.getNumber(), who,
                o.getEmail(), status, units, items.toString(), o.getTotalCents(),
                o.getCreatedAt().toEpochMilli());
    }
}
