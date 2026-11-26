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
package com.codenameone.examples.bazaar.returns;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.carrier.CarrierProvider;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.ReturnLine;
import com.codenameone.examples.bazaar.domain.ReturnRequest;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ReturnLineRepository;
import com.codenameone.examples.bazaar.repository.ReturnRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The prepaid return label of an approved return: bought from the store's
/// carrier for the units coming back, from the shopper's address to the
/// location that sent them (or the store's first location shipping online
/// orders, when that one is gone or is a fulfilment service's), with a
/// customs declaration when the parcel crosses a border. The approval mail
/// carries it as a PDF ([com.codenameone.examples.bazaar.docs.Documents#returnLabel]).
///
/// An order with no delivery address (a pickup, a sale at the register)
/// gets no label: the shopper brings it back.
@Component
public class ReturnLabels {
    private final ReturnRequestRepository returns;
    private final ReturnLineRepository returnLines;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final LocationRepository locations;
    private final Fulfillments fulfillments;
    private final Orders orderService;

    public ReturnLabels(ReturnRequestRepository returns, ReturnLineRepository returnLines,
            StoreOrderRepository orders, OrderLineRepository lines, LocationRepository locations,
            Fulfillments fulfillments, Orders orderService) {
        this.returns = returns;
        this.returnLines = returnLines;
        this.orders = orders;
        this.lines = lines;
        this.locations = locations;
        this.fulfillments = fulfillments;
        this.orderService = orderService;
    }

    /// Buys the label of return `returnId` unless it has one or needs none.
    /// Answers whether the return now has a label; a carrier that fails is
    /// logged and recorded on the order, never thrown.
    public boolean buy(String returnId) {
        try {
            Plan p = plan(returnId);
            if (p == null) {
                return false;
            }
            if (p.has) {
                return true;
            }
            CarrierProvider.Label label = fulfillments.returnLabel(p.order, p.locationId,
                    p.grams, p.items, returnId);
            record(returnId, label);
            orderService.event(p.order.getId(), "RETURN_LABEL", "Return label "
                    + label.trackingNumber + " bought", "");
            return true;
        } catch (IOException | RuntimeException err) {
            System.err.println("[returns] no return label for " + returnId + ": " + err);
            return false;
        }
    }

    private static final class Plan {
        StoreOrder order;
        String locationId;
        int grams;
        List<CarrierProvider.CustomsItem> items;
        boolean has;
    }

    @Transactional(readOnly = true)
    private Plan plan(String returnId) throws IOException {
        ReturnRequest r = returns.findById(returnId);
        StoreOrder o = r == null ? null : orders.findById(r.getOrderId());
        if (o == null || o.getShipCountry().length() == 0 || o.getShipLine1().length() == 0) {
            return null;
        }
        Plan p = new Plan();
        p.order = o;
        if (r.getReturnTracking().length() > 0) {
            p.has = true;
            return p;
        }
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        List<ReturnLine> back = returnLines.findByReturnId(returnId);
        List<OrderLine> picked = new ArrayList<OrderLine>();
        List<Integer> quantities = new ArrayList<Integer>();
        String held = "";
        for (int iter = 0; iter < back.size(); iter++) {
            OrderLine l = find(ls, back.get(iter).getOrderLineId());
            if (l == null || back.get(iter).getQuantity() <= 0) {
                continue;
            }
            picked.add(l);
            quantities.add(Integer.valueOf(back.get(iter).getQuantity()));
            p.grams += l.getWeightGrams() * back.get(iter).getQuantity();
            if (held.length() == 0) {
                held = l.getLocationId();
            }
        }
        if (picked.isEmpty()) {
            return null;
        }
        p.locationId = returnTo(o.getStoreId(), held);
        p.items = fulfillments.international(o) ? fulfillments.customsItems(o, picked, quantities)
                : new ArrayList<CarrierProvider.CustomsItem>();
        return p;
    }

    /// Where the parcel goes back to: the location that held the units while
    /// it is an active store location fulfilling online orders, else the
    /// first such location.
    private String returnTo(String storeId, String held) {
        Location l = held.length() == 0 ? null : locations.findById(held);
        if (l != null && storeId.equals(l.getStoreId()) && l.isActive() && l.isFulfilsOnline()) {
            return l.getId();
        }
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).isActive() && all.get(iter).isFulfilsOnline()) {
                return all.get(iter).getId();
            }
        }
        return all.isEmpty() ? "" : all.get(0).getId();
    }

    @Transactional(rollbackFor = IOException.class)
    private void record(String returnId, CarrierProvider.Label label) throws IOException {
        ReturnRequest r = returns.findById(returnId);
        r.setReturnCarrier(fulfillments.carrierName());
        r.setReturnTracking(label.trackingNumber);
        r.setReturnLabelUrl(label.labelUrl);
        r.setReturnLabelCents(Math.max(0, label.costCents));
        r.setUpdatedAt(Instant.now());
        returns.save(r);
    }

    private static OrderLine find(List<OrderLine> ls, String id) {
        for (int iter = 0; iter < ls.size(); iter++) {
            if (ls.get(iter).getId().equals(id)) {
                return ls.get(iter);
            }
        }
        return null;
    }
}
