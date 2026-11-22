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
package com.codenameone.examples.bazaar.routing;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.risk.RiskHolds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Hands orders to the store's fulfilment service and ships them as it
/// reports. Every tick:
///
/// 1. Each open, paid order of a store with a service, holding lines routed
///    to the service's location, is submitted once (the unique order and
///    location row makes that idempotent). Orders held for a fraud review
///    wait until released.
/// 2. Each order the service reports SHIPPED becomes a fulfilment with the
///    service's carrier and tracking number, through
///    [Fulfillments#shippedByService]: stock, the order's status, the
///    `orders/fulfilled` webhook, the push and the shipping mail follow as
///    for a parcel the store sent itself.
/// 3. Scans of parcels it shipped are recorded, as a carrier's are.
@Component
public class ThirdPartyFulfilment {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    private static final String ACTOR = "fulfilment-service";

    private final DataSource db;
    private final Fulfillments fulfillments;
    private final Orders orderService;
    private final RiskHolds holds;
    private final FulfilmentService service;

    public ThirdPartyFulfilment(DataSource db, Fulfillments fulfillments, Orders orderService,
            RiskHolds holds) {
        this.db = db;
        this.fulfillments = fulfillments;
        this.orderService = orderService;
        this.holds = holds;
        this.service = new SimulatedFulfilmentService(Long.getLong(
                "bazaar.fulfilment.simulated.step.millis", 60000L).longValue());
    }

    /// The service a store names; null for none or one this build lacks.
    FulfilmentService serviceNamed(String name) {
        return service.name().equals(name) ? service : null;
    }

    @Scheduled(fixedDelay = 20000, initialDelay = 40000, lock = "bazaar-fulfilment-service")
    public void scheduled() {
        try {
            tick(System.currentTimeMillis());
        } catch (IOException | RuntimeException err) {
            System.err.println("[3pl] " + err);
        }
    }

    /// One pass, as of `now`.
    public void tick(long now) throws IOException {
        List<Map> stores = stores();
        for (int iter = 0; iter < stores.size(); iter++) {
            Map s = stores.get(iter);
            FulfilmentService svc = serviceNamed(Rows.text(s.get("service")));
            if (svc != null) {
                submit(svc, Rows.text(s.get("store_id")),
                        Rows.text(s.get("service_location_id")), now);
            }
        }
        List<Map> open = handedOver();
        for (int iter = 0; iter < open.size(); iter++) {
            Map r = open.get(iter);
            FulfilmentService svc = serviceNamed(Rows.text(r.get("service")));
            if (svc == null) {
                continue;
            }
            try {
                follow(svc, r, now);
            } catch (IOException | RuntimeException err) {
                // One order the service cannot report on never stops the rest.
                System.err.println("[3pl] " + Rows.text(r.get("external_id")) + ": " + err);
            }
        }
    }

    @Transactional(readOnly = true)
    private List<Map> stores() throws IOException {
        return db.query("SELECT store_id, service, service_location_id FROM routing_setting "
                + "WHERE service <> '' AND service_location_id <> ''", new Object[0]);
    }

    @Transactional(readOnly = true)
    private List<Map> handedOver() throws IOException {
        return db.query("SELECT id, store_id, order_id, location_id, service, external_id, "
                + "status, fulfillment_id, created_at FROM fulfilment_service_order WHERE "
                + "status IN (?, ?) ORDER BY created_at, id", new Object[] {ACCEPTED, SHIPPED});
    }

    private void submit(FulfilmentService svc, String storeId, String locationId, long now)
            throws IOException {
        List<Map> waiting = waitingOrders(storeId, locationId);
        for (int iter = 0; iter < waiting.size(); iter++) {
            Map o = waiting.get(iter);
            String orderId = Rows.text(o.get("id"));
            try {
                holds.requireReleased(orderId);
            } catch (RuntimeException held) {
                continue;
            }
            String external = svc.submit(new FulfilmentService.Submission(orderId,
                    Rows.number(o.get("number")), locationId, skus(orderId, locationId)));
            if (recordSubmitted(storeId, orderId, locationId, svc.name(), external, now)) {
                orderService.event(orderId, "FULFILMENT_SERVICE", "Handed to the fulfilment "
                        + "service as " + external, ACTOR);
            }
        }
    }

    /// Open orders paid for (or authorised) with lines at `locationId` that
    /// have not been handed over yet.
    @Transactional(readOnly = true)
    private List<Map> waitingOrders(String storeId, String locationId) throws IOException {
        return db.query("SELECT DISTINCT o.id, o.number FROM store_order o JOIN order_line l "
                + "ON l.order_id = o.id WHERE o.store_id = ? AND o.status = 'OPEN' AND "
                + "o.financial IN ('PAID', 'AUTHORIZED', 'PARTIALLY_REFUNDED') AND "
                + "o.fulfilment <> 'FULFILLED' AND l.location_id = ? AND NOT EXISTS (SELECT 1 "
                + "FROM fulfilment_service_order f WHERE f.order_id = o.id AND "
                + "f.location_id = ?) ORDER BY o.number", new Object[] {storeId, locationId,
                    locationId});
    }

    @Transactional(readOnly = true)
    private List<String> skus(String orderId, String locationId) throws IOException {
        List rows = db.query("SELECT sku FROM order_line WHERE order_id = ? AND location_id = ?",
                new Object[] {orderId, locationId});
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(Rows.text(((Map) rows.get(iter)).get("sku")));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    private boolean recordSubmitted(String storeId, String orderId, String locationId,
            String svc, String external, long now) throws IOException {
        if (db.queryOne("SELECT id FROM fulfilment_service_order WHERE order_id = ? AND "
                + "location_id = ?", new Object[] {orderId, locationId}) != null) {
            return false;
        }
        db.execute("INSERT INTO fulfilment_service_order (id, store_id, order_id, location_id, "
                + "service, external_id, status, carrier, tracking_number, fulfillment_id, "
                + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, '', '', '', ?, ?)",
                new Object[] {Ids.next(), storeId, orderId, locationId, svc, external, ACCEPTED,
                    Long.valueOf(now), Long.valueOf(now)});
        return true;
    }

    private void follow(FulfilmentService svc, Map r, long now) throws IOException {
        String id = Rows.text(r.get("id"));
        FulfilmentService.Update u = svc.status(Rows.text(r.get("external_id")),
                Rows.number(r.get("created_at")), now);
        String fulfillmentId = Rows.text(r.get("fulfillment_id"));
        if (ACCEPTED.equals(Rows.text(r.get("status")))) {
            if (!SHIPPED.equals(u.status)) {
                return;
            }
            String made = fulfillments.shippedByService(Rows.text(r.get("store_id")),
                    Rows.text(r.get("order_id")), Rows.text(r.get("location_id")), u.carrier,
                    u.trackingNumber, ACTOR);
            fulfillmentId = made == null ? "" : made;
            update(id, made == null ? DELIVERED : SHIPPED, u.carrier, u.trackingNumber,
                    fulfillmentId, now);
        }
        if (fulfillmentId.length() == 0) {
            return;
        }
        fulfillments.serviceScans(fulfillmentId, u.scans);
        for (int iter = 0; iter < u.scans.size(); iter++) {
            if (DELIVERED.equals(u.scans.get(iter).status)) {
                update(id, DELIVERED, u.carrier, u.trackingNumber, fulfillmentId, now);
            }
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private void update(String id, String status, String carrier, String tracking,
            String fulfillmentId, long now) throws IOException {
        db.execute("UPDATE fulfilment_service_order SET status = ?, carrier = ?, "
                + "tracking_number = ?, fulfillment_id = ?, updated_at = ? WHERE id = ?",
                new Object[] {status, carrier, tracking, fulfillmentId, Long.valueOf(now), id});
    }
}
