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
package com.codenameone.examples.bazaar.fulfil;

import com.codename1.appkit.server.push.PushSender;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.notify.ShopperLanguage;
import com.codenameone.examples.bazaar.web.StoreText;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.BoardMoveDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OpenLineDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.TrackingEventDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import com.codenameone.examples.bazaar.carrier.CarrierProvider;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.FulfillmentLine;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.TrackingEvent;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.push.PushDevices;
import com.codenameone.examples.bazaar.repository.FulfillmentLineRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.TrackingEventRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Sending orders out.
///
/// A fulfilment is some units of an order leaving one location, by one of
/// three methods, told apart by its `carrier` column:
///
/// - **SHIP**: PENDING, PACKED, a label bought from the [CarrierProvider]
///   (the carrier's name goes in `carrier`), SHIPPED, then IN_TRANSIT,
///   OUT_FOR_DELIVERY and DELIVERED as the carrier scans it ([#advance]).
/// - **PICKUP** (`carrier` = PICKUP): PENDING, PACKED (ready at the
///   counter), PICKED_UP.
/// - **LOCAL** (`carrier` = LOCAL): PENDING, PACKED, SHIPPED (with the
///   store's driver), DELIVERED.
///
/// The units leave the shelf when the fulfilment ships or is handed over:
/// [Inventory#ship] per line, then [Orders#recordFulfilled], which also takes
/// a payment that was only held -- so the card is charged once, by the first
/// shipment. Before that a fulfilment can be cancelled and its units go back
/// to the queue, still promised to the order.
///
/// Public methods check the caller and call the carrier and the push service
/// outside any transaction; the private `@Transactional` steps do the writes,
/// each status change claimed with a conditional update so that two clicks
/// cannot ship twice.
@Component
public class Fulfillments {
    public static final String PENDING = "PENDING";
    public static final String PACKED = "PACKED";
    public static final String SHIPPED = "SHIPPED";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String DELIVERED = "DELIVERED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String CANCELLED = "CANCELLED";
    public static final String LABEL_CREATED = "LABEL_CREATED";

    public static final String SHIP = "SHIP";
    public static final String PICKUP = "PICKUP";
    public static final String LOCAL = "LOCAL";

    public static final String COLUMN_TO_PACK = "TO_PACK";
    public static final String COLUMN_PACKED = "PACKED";
    public static final String COLUMN_SHIPPED = "SHIPPED";
    public static final String COLUMN_DELIVERED = "DELIVERED";
    private static final String[] COLUMNS = {COLUMN_TO_PACK, COLUMN_PACKED, COLUMN_SHIPPED,
        COLUMN_DELIVERED};
    private static final String[] COLUMN_TITLES = {"To pack", "Packed", "Shipped", "Delivered"};
    /// How many delivered orders the board keeps showing.
    private static final int DELIVERED_CARDS = 30;

    private final FulfillmentRepository fulfillments;
    private final FulfillmentLineRepository fulfillmentLines;
    private final TrackingEventRepository tracking;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final LocationRepository locations;
    private final VariantRepository variants;
    private final Inventory inventory;
    private final Orders orderService;
    private final StoreAccess access;
    private final CarrierProvider carrier;
    private final PushSender push;
    private final PushDevices devices;
    private final Emails emails;

    /// The language pushes to the shopper go out in. Field-injected, so the
    /// constructor stays as it was.
    @com.codename1.backend.annotations.Autowired
    private ShopperLanguage language;

    /// Refuses to fulfil an order held for a fraud review.
    private final com.codenameone.examples.bazaar.risk.RiskHolds holds;

    public Fulfillments(FulfillmentRepository fulfillments,
            FulfillmentLineRepository fulfillmentLines, TrackingEventRepository tracking,
            StoreOrderRepository orders, OrderLineRepository lines, LocationRepository locations,
            VariantRepository variants, Inventory inventory, Orders orderService,
            StoreAccess access, CarrierProvider carrier, PushSender push, PushDevices devices, Emails emails,
            com.codenameone.examples.bazaar.risk.RiskHolds holds) {
        this.emails = emails;
        this.holds = holds;
        this.fulfillments = fulfillments;
        this.fulfillmentLines = fulfillmentLines;
        this.tracking = tracking;
        this.orders = orders;
        this.lines = lines;
        this.locations = locations;
        this.variants = variants;
        this.inventory = inventory;
        this.orderService = orderService;
        this.access = access;
        this.carrier = carrier;
        this.push = push;
        this.devices = devices;
    }

    /// How a fulfilment goes out: SHIP, PICKUP or LOCAL.
    public static String method(Fulfillment f) {
        if (PICKUP.equals(f.getCarrier())) {
            return PICKUP;
        }
        return LOCAL.equals(f.getCarrier()) ? LOCAL : SHIP;
    }

    /// Whether a status means the units have left the store.
    public static boolean isDispatched(String status) {
        return SHIPPED.equals(status) || IN_TRANSIT.equals(status)
                || OUT_FOR_DELIVERY.equals(status) || DELIVERED.equals(status)
                || PICKED_UP.equals(status);
    }

    private static boolean isOpen(String status) {
        return PENDING.equals(status) || PACKED.equals(status);
    }

    // ---------------------------------------------------------------- queue

    @Transactional(readOnly = true)
    public List<UnfulfilledDto> queue(String username, String storeId, String locationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<StoreOrder> rows = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<UnfulfilledDto> out = new ArrayList<UnfulfilledDto>();
        for (int iter = rows.size() - 1; iter >= 0; iter--) {
            StoreOrder o = rows.get(iter);
            if (!awaitsShipping(o)) {
                continue;
            }
            Map<String, Integer> waiting = waiting(o.getId());
            List<OpenLineDto> open = new ArrayList<OpenLineDto>();
            List<OrderLine> ls = lines.findByOrderId(o.getId());
            for (int l = 0; l < ls.size(); l++) {
                OrderLine line = ls.get(l);
                int left = openQty(line, waiting);
                if (left > 0 && (empty(locationId) || locationId.equals(line.getLocationId()))) {
                    open.add(new OpenLineDto(line.getId(), line.getTitle(), line.getVariantTitle(),
                            line.getSku(), line.getLocationId(), left));
                }
            }
            if (!open.isEmpty()) {
                out.add(new UnfulfilledDto(o.getId(), o.getNumber(), customer(o),
                        o.getShippingTitle(), millis(o.getCreatedAt()), open));
            }
        }
        return out;
    }

    private static boolean awaitsShipping(StoreOrder o) {
        return Orders.OPEN.equals(o.getStatus()) && !Orders.FULFILLED.equals(o.getFulfilment())
                && !Orders.isUnconvertedDraft(o) && !Orders.POS.equals(o.getChannel());
    }

    /// Units of each line already in a fulfilment that has not left yet.
    private Map<String, Integer> waiting(String orderId) throws IOException {
        Map<String, Integer> out = new HashMap<String, Integer>();
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        for (int iter = 0; iter < fs.size(); iter++) {
            if (!isOpen(fs.get(iter).getStatus())) {
                continue;
            }
            List<FulfillmentLine> fl = fulfillmentLines.findByFulfillmentId(fs.get(iter).getId());
            for (int l = 0; l < fl.size(); l++) {
                Integer had = out.get(fl.get(l).getOrderLineId());
                out.put(fl.get(l).getOrderLineId(), Integer.valueOf((had == null ? 0
                        : had.intValue()) + fl.get(l).getQuantity()));
            }
        }
        return out;
    }

    private static int openQty(OrderLine line, Map<String, Integer> waiting) {
        Integer w = waiting.get(line.getId());
        return line.getQuantity() - line.getRefundedQty() - line.getFulfilledQty()
                - (w == null ? 0 : w.intValue());
    }

    // ------------------------------------------------------------- creating

    @Transactional(readOnly = true)
    public List<FulfillmentDto> ofOrder(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        StoreOrder order = orderIn(storeId, orderId);
        List<FulfillmentDto> out = new ArrayList<FulfillmentDto>();
        List<Fulfillment> fs = fulfillments.findByOrderId(order.getId());
        for (int iter = 0; iter < fs.size(); iter++) {
            out.add(dto(fs.get(iter), order, false));
        }
        return out;
    }

    /// Puts chosen units of an order into a new fulfilment from one location.
    /// Each line can be split across as many fulfilments as the merchant
    /// likes, never past what is left to send.
    public FulfillmentDto create(String username, String storeId, String orderId,
            NewFulfillmentDto req) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = createTx(storeId, orderId, req, username);
        orderService.publish(orders.findById(orderId));
        return detail(f.getId());
    }

    @Transactional(rollbackFor = IOException.class)
    private Fulfillment createTx(String storeId, String orderId, NewFulfillmentDto req,
            String username) throws IOException {
        StoreOrder order = orderIn(storeId, orderId);
        if (!awaitsShipping(order)) {
            throw new ResponseStatusException(409, "The order has nothing to send");
        }
        holds.requireReleased(orderId);
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new ResponseStatusException(400, "Choose what goes in the parcel");
        }
        Location location = req.locationId() == null ? null : locations.findById(req.locationId());
        if (location == null || !storeId.equals(location.getStoreId())) {
            throw new ResponseStatusException(404, "No such location");
        }
        String method = empty(req.method()) ? SHIP : req.method().trim();
        if (!SHIP.equalsIgnoreCase(method) && !PICKUP.equalsIgnoreCase(method)
                && !LOCAL.equalsIgnoreCase(method)) {
            throw new ResponseStatusException(400, "A fulfilment is shipped, picked up or "
                    + "delivered locally");
        }
        Map<String, Integer> waiting = waiting(orderId);
        List<OrderLine> ls = lines.findByOrderId(orderId);
        Map<String, Integer> chosen = new HashMap<String, Integer>();
        int grams = 0;
        for (int iter = 0; iter < req.lines().size(); iter++) {
            FulfilLineDto w = req.lines().get(iter);
            OrderLine line = w == null ? null : find(ls, w.orderLineId());
            if (line == null) {
                throw new ResponseStatusException(400, "That line is not on the order");
            }
            if (w.quantity() <= 0) {
                throw new ResponseStatusException(400, "A quantity is at least 1");
            }
            Integer before = chosen.get(line.getId());
            int total = w.quantity() + (before == null ? 0 : before.intValue());
            if (total > openQty(line, waiting)) {
                throw new ResponseStatusException(409, "More of " + line.getTitle()
                        + " than is left to send");
            }
            chosen.put(line.getId(), Integer.valueOf(total));
            grams += line.getWeightGrams() * w.quantity();
        }
        Fulfillment f = new Fulfillment();
        f.setId(Ids.next());
        f.setOrderId(orderId);
        f.setStoreId(storeId);
        f.setLocationId(location.getId());
        f.setStatus(PENDING);
        f.setCarrier(PICKUP.equalsIgnoreCase(method) ? PICKUP : LOCAL.equalsIgnoreCase(method)
                ? LOCAL : "");
        f.setWeightGrams(grams);
        f.setCreatedAt(Instant.now());
        f.setUpdatedAt(f.getCreatedAt());
        fulfillments.insert(f);
        for (Map.Entry<String, Integer> e : chosen.entrySet()) {
            FulfillmentLine row = new FulfillmentLine();
            row.setId(Ids.next());
            row.setFulfillmentId(f.getId());
            row.setOrderLineId(e.getKey());
            row.setQuantity(e.getValue().intValue());
            fulfillmentLines.insert(row);
        }
        orderService.event(orderId, "FULFILMENT", "Fulfilment created from "
                + location.getName(), username);
        return f;
    }

    // ---------------------------------------------------------- transitions

    public FulfillmentDto pack(String username, String storeId, String id, PackDto pack)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        move(f, PENDING, PACKED, pack == null ? 0 : pack.weightGrams(), "Packed", username);
        return published(id);
    }

    /// A pickup packed and waiting: the shopper is told to come.
    public FulfillmentDto pickupReady(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        if (!PICKUP.equals(method(f))) {
            throw new ResponseStatusException(409, "That is not a pickup");
        }
        if (PENDING.equals(f.getStatus())) {
            move(f, PENDING, PACKED, 0, "Ready for pickup", username);
        } else if (!PACKED.equals(f.getStatus())) {
            throw new ResponseStatusException(409, "The pickup is " + f.getStatus());
        }
        notifyShopper(f.getOrderId(), "push.pickup.title", "push.pickup.body",
                locationName(f.getLocationId()));
        return published(id);
    }

    @Transactional(readOnly = true)
    public List<RateQuoteDto> rates(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        if (!SHIP.equals(method(f)) || !isOpen(f.getStatus())) {
            throw new ResponseStatusException(409, "Only a parcel waiting to ship has rates");
        }
        return carrier.rates(new CarrierProvider.Parcel(f.getWeightGrams()),
                from(f.getLocationId()), to(orders.findById(f.getOrderId())));
    }

    public FulfillmentDto buyLabel(String username, String storeId, String id,
            LabelRequestDto req) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        if (!SHIP.equals(method(f)) || !isOpen(f.getStatus())) {
            throw new ResponseStatusException(409, "Only a parcel waiting to ship gets a label");
        }
        if (f.getTrackingNumber().length() > 0) {
            throw new ResponseStatusException(409, "The parcel already has a label");
        }
        if (req == null || empty(req.service())) {
            throw new ResponseStatusException(400, "Choose a service");
        }
        CarrierProvider.Label label = carrier.buyLabel(
                new CarrierProvider.Parcel(f.getWeightGrams()), from(f.getLocationId()),
                to(orders.findById(f.getOrderId())), req.service().trim(), f.getId());
        labelTx(id, label, username);
        return published(id);
    }

    @Transactional(rollbackFor = IOException.class)
    private void labelTx(String id, CarrierProvider.Label label, String username)
            throws IOException {
        Fulfillment f = fulfillments.findById(id);
        if (f.getTrackingNumber().length() > 0) {
            throw new ResponseStatusException(409, "The parcel already has a label");
        }
        f.setCarrier(carrier.name());
        f.setService(label.service);
        f.setTrackingNumber(label.trackingNumber);
        f.setLabelCents(label.costCents);
        f.setLabelUrl(Codes.cut(label.labelUrl, 500));
        f.setUpdatedAt(Instant.now());
        fulfillments.save(f);
        Location from = locations.findById(f.getLocationId());
        scan(id, LABEL_CREATED, "Label created, " + label.service, from == null ? ""
                : from.getName(), from == null ? 0 : from.getLat(), from == null ? 0
                : from.getLng(), System.currentTimeMillis());
        orderService.event(f.getOrderId(), "LABEL", "Label " + label.trackingNumber + " bought",
                username);
    }

    /// The parcel leaves: with the carrier (it needs a label) or with the
    /// store's driver. Stock leaves the shelf and a held payment is taken.
    public FulfillmentDto ship(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        String method = method(f);
        if (PICKUP.equals(method)) {
            throw new ResponseStatusException(409, "A pickup is handed over, not shipped");
        }
        if (SHIP.equals(method) && f.getTrackingNumber().length() == 0) {
            throw new ResponseStatusException(409, "Buy a label first");
        }
        dispatch(f, SHIPPED, LOCAL.equals(method) ? "Out with the store's driver"
                : "Handed to " + f.getCarrier(), username);
        notifyShopper(f.getOrderId(), "push.shipped.title", LOCAL.equals(method)
                ? "push.shipped.driver" : "push.shipped.tracking", f.getTrackingNumber());
        emails.shipped(id);
        return published(id);
    }

    public FulfillmentDto pickedUp(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Fulfillment f = fulfillmentIn(storeId, id);
        if (!PICKUP.equals(method(f))) {
            throw new ResponseStatusException(409, "That is not a pickup");
        }
        dispatch(f, PICKED_UP, "Picked up", username);
        return published(id);
    }

    /// A local delivery reached the door. One that never got the SHIPPED step
    /// leaves the shelf now.
    public FulfillmentDto delivered(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        if (!LOCAL.equals(method(f))) {
            throw new ResponseStatusException(409, "The carrier reports when a parcel arrives");
        }
        if (isOpen(f.getStatus())) {
            dispatch(f, DELIVERED, "Delivered by the store", username);
        } else {
            move(f, SHIPPED, DELIVERED, 0, "Delivered by the store", username);
        }
        notifyShopper(f.getOrderId(), "mail.delivered.title", "push.delivered.body");
        emails.delivered(id);
        return published(id);
    }

    /// Takes back a fulfilment that has not left; its units go back to the
    /// queue, still promised to the order.
    public FulfillmentDto cancel(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        Fulfillment f = fulfillmentIn(storeId, id);
        if (!isOpen(f.getStatus())) {
            throw new ResponseStatusException(409, "The fulfilment has already left");
        }
        move(f, f.getStatus(), CANCELLED, 0, "Fulfilment cancelled", username);
        return published(id);
    }

    /// A status change that moves no stock.
    private void move(Fulfillment f, String from, String to, int grams, String message,
            String username) throws IOException {
        moveTx(f.getId(), from, to, grams, message, username);
    }

    @Transactional(rollbackFor = IOException.class)
    private void moveTx(String id, String from, String to, int grams, String message,
            String username) throws IOException {
        if (fulfillments.claimStatus(id, from, to) != 1) {
            throw new ResponseStatusException(409, "The fulfilment is not " + from);
        }
        Fulfillment f = fulfillments.findById(id);
        if (grams > 0) {
            f.setWeightGrams(grams);
        }
        f.setUpdatedAt(Instant.now());
        fulfillments.save(f);
        if (!PACKED.equals(to)) {
            Location at = locations.findById(f.getLocationId());
            scan(id, to, message, at == null ? "" : at.getName(), 0, 0,
                    System.currentTimeMillis());
        }
        orderService.event(f.getOrderId(), "FULFILMENT_" + to, message, username);
    }

    /// The units leave: the status is claimed, each line ships from the
    /// fulfilment's location, and then the order records them (taking a held
    /// payment on the first shipment).
    private void dispatch(Fulfillment f, String to, String message, String username)
            throws IOException {
        Map<String, Integer> shipped = dispatchTx(f.getId(), to, message, username);
        orderService.recordFulfilled(f.getOrderId(), shipped, username);
    }

    @Transactional(rollbackFor = IOException.class)
    private Map<String, Integer> dispatchTx(String id, String to, String message,
            String username) throws IOException {
        Fulfillment f = fulfillments.findById(id);
        String from = f.getStatus();
        if (!isOpen(from) || fulfillments.claimStatus(id, from, to) != 1) {
            throw new ResponseStatusException(409, "The fulfilment is not waiting to leave");
        }
        f = fulfillments.findById(id);
        f.setUpdatedAt(Instant.now());
        fulfillments.save(f);
        Map<String, Integer> shipped = new HashMap<String, Integer>();
        List<OrderLine> ls = lines.findByOrderId(f.getOrderId());
        List<FulfillmentLine> fl = fulfillmentLines.findByFulfillmentId(id);
        for (int iter = 0; iter < fl.size(); iter++) {
            OrderLine line = find(ls, fl.get(iter).getOrderLineId());
            int q = fl.get(iter).getQuantity();
            if (line == null || q <= 0) {
                continue;
            }
            shipped.put(line.getId(), Integer.valueOf(q));
            String held = line.getLocationId();
            if (held.length() == 0) {
                continue;
            }
            if (held.equals(f.getLocationId())) {
                inventory.ship(f.getStoreId(), line.getVariantId(), held, q, id, username);
            } else {
                // Promised at one location, sent from another: the promise is
                // let go there and the units leave from here.
                inventory.release(f.getStoreId(), line.getVariantId(), held, q, id, username);
                Variant v = variants.findById(line.getVariantId());
                if (v != null && v.isTrackInventory()) {
                    inventory.change(f.getStoreId(), line.getVariantId(), f.getLocationId(), -q,
                            0, 0, Inventory.SALE, id, username);
                }
            }
        }
        Location at = locations.findById(f.getLocationId());
        scan(id, to, message, at == null ? "" : at.getName(), at == null ? 0 : at.getLat(),
                at == null ? 0 : at.getLng(), System.currentTimeMillis());
        return shipped;
    }

    // ---------------------------------------------------------------- board

    @Transactional(readOnly = true)
    public List<BoardColumnDto> board(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<List<OrderCardDto>> cards = new ArrayList<List<OrderCardDto>>();
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            cards.add(new ArrayList<OrderCardDto>());
        }
        List<StoreOrder> rows = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        int delivered = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreOrder o = rows.get(iter);
            if (!Orders.OPEN.equals(o.getStatus()) || Orders.isUnconvertedDraft(o)
                    || Orders.POS.equals(o.getChannel())) {
                continue;
            }
            OrderCardDto card = card(o);
            if (card == null) {
                continue;
            }
            int column = indexOf(card.column());
            if (column == 3 && delivered++ >= DELIVERED_CARDS) {
                continue;
            }
            // Newest first in the list; the board wants oldest at the top.
            cards.get(column).add(0, card);
        }
        List<BoardColumnDto> out = new ArrayList<BoardColumnDto>();
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            out.add(new BoardColumnDto(COLUMNS[iter], COLUMN_TITLES[iter], cards.get(iter)));
        }
        return out;
    }

    /// Where an order sits: To pack while anything of it is unassigned or in
    /// a PENDING fulfilment (an order with no fulfilment at all starts here),
    /// Packed while a fulfilment is packed, Shipped while one is on its way,
    /// Delivered once everything arrived or was collected. Null for an order
    /// with nothing to show (all refunded).
    private OrderCardDto card(StoreOrder o) throws IOException {
        List<Fulfillment> fs = fulfillments.findByOrderId(o.getId());
        Map<String, Integer> waiting = waiting(o.getId());
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        boolean unassigned = false;
        int items = 0;
        for (int iter = 0; iter < ls.size(); iter++) {
            unassigned |= openQty(ls.get(iter), waiting) > 0;
            items += ls.get(iter).getQuantity() - ls.get(iter).getRefundedQty();
        }
        Fulfillment pending = first(fs, PENDING, null, null);
        Fulfillment packed = first(fs, PACKED, null, null);
        Fulfillment moving = first(fs, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY);
        Fulfillment arrived = first(fs, DELIVERED, PICKED_UP, null);
        String column;
        Fulfillment shown;
        if (unassigned || pending != null) {
            column = COLUMN_TO_PACK;
            shown = pending;
        } else if (packed != null) {
            column = COLUMN_PACKED;
            shown = packed;
        } else if (moving != null) {
            column = COLUMN_SHIPPED;
            shown = moving;
        } else if (arrived != null) {
            column = COLUMN_DELIVERED;
            shown = arrived;
        } else {
            return null;
        }
        return new OrderCardDto(o.getId(), o.getNumber(), customer(o), items, o.getTotalCents(),
                o.getCurrency(), column, shown == null ? "" : shown.getId(),
                shown == null ? "" : shown.getTrackingNumber(), millis(o.getCreatedAt()));
    }

    private static Fulfillment first(List<Fulfillment> fs, String a, String b, String c) {
        for (int iter = 0; iter < fs.size(); iter++) {
            String s = fs.get(iter).getStatus();
            if (s.equals(a) || s.equals(b) || s.equals(c)) {
                return fs.get(iter);
            }
        }
        return null;
    }

    private static int indexOf(String column) {
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (COLUMNS[iter].equalsIgnoreCase(column)) {
                return iter;
            }
        }
        return -1;
    }

    /// Moves a card one column to the right, doing what that column means:
    ///
    /// - To pack to Packed: whatever is unassigned goes into one fulfilment
    ///   per location it is held at (SHIP), and every PENDING fulfilment is packed.
    /// - Packed to Shipped: each packed parcel leaves -- a SHIP one buys the
    ///   cheapest label first if it has none, a pickup is handed over (and so
    ///   lands in Delivered), a local one goes out with the driver.
    /// - Shipped to Delivered: the parcels on their way are marked delivered
    ///   by the store, for a carrier that does not scan.
    ///
    /// Any other move -- backwards, or skipping a column -- is a 409.
    public List<BoardColumnDto> moveCard(String username, String storeId, BoardMoveDto req)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        if (req == null) {
            throw new ResponseStatusException(400, "Say which card goes where");
        }
        StoreOrder order = orderRead(storeId, req.orderId());
        OrderCardDto card = cardRead(order);
        int from = card == null ? -1 : indexOf(card.column());
        int to = indexOf(req.toColumn());
        if (from < 0 || to != from + 1) {
            throw new ResponseStatusException(409, "A card moves one column to the right");
        }
        List<Fulfillment> fs = fulfillmentsRead(order.getId());
        if (to == 1) {
            packAll(username, order, fs);
        } else if (to == 2) {
            for (int iter = 0; iter < fs.size(); iter++) {
                Fulfillment f = fs.get(iter);
                if (!PACKED.equals(f.getStatus())) {
                    continue;
                }
                String method = method(f);
                if (PICKUP.equals(method)) {
                    pickedUp(username, storeId, f.getId());
                } else {
                    if (SHIP.equals(method) && f.getTrackingNumber().length() == 0) {
                        List<RateQuoteDto> quotes = rates(username, storeId, f.getId());
                        RateQuoteDto best = null;
                        for (int q = 0; q < quotes.size(); q++) {
                            if (best == null || quotes.get(q).cents() < best.cents()) {
                                best = quotes.get(q);
                            }
                        }
                        if (best == null) {
                            throw new ResponseStatusException(409, "No carrier service fits");
                        }
                        buyLabel(username, storeId, f.getId(),
                                new LabelRequestDto(best.service()));
                    }
                    ship(username, storeId, f.getId());
                }
            }
        } else {
            for (int iter = 0; iter < fs.size(); iter++) {
                Fulfillment f = fs.get(iter);
                String s = f.getStatus();
                if (SHIPPED.equals(s) || IN_TRANSIT.equals(s) || OUT_FOR_DELIVERY.equals(s)) {
                    move(f, s, DELIVERED, 0, "Marked delivered by the store", username);
                }
            }
            notifyShopper(order.getId(), "mail.delivered.title", "push.delivered.body");
            orderService.publish(orders.findById(order.getId()));
        }
        return board(username, storeId);
    }

    private void packAll(String username, StoreOrder order, List<Fulfillment> fs)
            throws IOException {
        Map<String, List<FulfilLineDto>> byLocation = unassignedByLocation(order);
        for (Map.Entry<String, List<FulfilLineDto>> e : byLocation.entrySet()) {
            FulfillmentDto made = create(username, order.getStoreId(), order.getId(),
                    new NewFulfillmentDto(e.getKey(), SHIP, e.getValue()));
            fs = new ArrayList<Fulfillment>(fs);
            fs.add(fulfillmentRead(made.id()));
        }
        for (int iter = 0; iter < fs.size(); iter++) {
            if (PENDING.equals(fs.get(iter).getStatus())) {
                move(fs.get(iter), PENDING, PACKED, 0, "Packed", username);
            }
        }
        orderService.publish(orders.findById(order.getId()));
    }

    @Transactional(readOnly = true)
    private Map<String, List<FulfilLineDto>> unassignedByLocation(StoreOrder order)
            throws IOException {
        Map<String, Integer> waiting = waiting(order.getId());
        List<OrderLine> ls = lines.findByOrderId(order.getId());
        Map<String, List<FulfilLineDto>> out = new HashMap<String, List<FulfilLineDto>>();
        String fallback = null;
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine line = ls.get(iter);
            int left = openQty(line, waiting);
            if (left <= 0) {
                continue;
            }
            String loc = line.getLocationId();
            if (loc.length() == 0) {
                if (fallback == null) {
                    fallback = shippingLocation(order.getStoreId());
                }
                loc = fallback;
            }
            List<FulfilLineDto> list = out.get(loc);
            if (list == null) {
                list = new ArrayList<FulfilLineDto>();
                out.put(loc, list);
            }
            list.add(new FulfilLineDto(line.getId(), null, null, left));
        }
        return out;
    }

    private String shippingLocation(String storeId) {
        List<Location> ls = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < ls.size(); iter++) {
            if (ls.get(iter).isActive() && ls.get(iter).isFulfilsOnline()) {
                return ls.get(iter).getId();
            }
        }
        if (ls.isEmpty()) {
            throw new ResponseStatusException(409, "The store has no location to ship from");
        }
        return ls.get(0).getId();
    }

    @Transactional(readOnly = true)
    private OrderCardDto cardRead(StoreOrder order) throws IOException {
        return card(order);
    }

    @Transactional(readOnly = true)
    private List<Fulfillment> fulfillmentsRead(String orderId) throws IOException {
        return fulfillments.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    private Fulfillment fulfillmentRead(String id) throws IOException {
        return fulfillments.findById(id);
    }

    // ------------------------------------------------------------- tracking

    /// The shopper's own order's shipments, cancelled ones left out.
    @Transactional(readOnly = true)
    public List<FulfillmentDto> tracking(String username, String orderId) throws IOException {
        // Answers 404 unless the order is the caller's.
        orderService.mineDetail(username, orderId);
        StoreOrder order = orders.findById(orderId);
        List<FulfillmentDto> out = new ArrayList<FulfillmentDto>();
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        for (int iter = 0; iter < fs.size(); iter++) {
            if (!CANCELLED.equals(fs.get(iter).getStatus())) {
                out.add(dto(fs.get(iter), order, false));
            }
        }
        return out;
    }

    /// What anyone with the number may see: status and scans, no order, no
    /// names, no lines.
    @Transactional(readOnly = true)
    public FulfillmentDto track(String trackingNumber) throws IOException {
        Fulfillment f = empty(trackingNumber) ? null
                : fulfillments.findByTrackingNumber(trackingNumber.trim());
        if (f == null) {
            throw new ResponseStatusException(404, "No such tracking number");
        }
        return dto(f, null, true);
    }

    /// Asks the carrier for news of every parcel on its way and records each
    /// new scan; a parcel whose newest scan is a later status moves to it,
    /// and the shopper hears of "out for delivery" and "delivered".
    @Scheduled(fixedDelay = 20000, initialDelay = 30000, lock = "bazaar-tracking")
    public void poll() throws IOException {
        advance(System.currentTimeMillis());
    }

    /// [#poll] as of `now`; answers how many parcels changed status. Public
    /// so that tests can run the clock forward.
    public int advance(long now) throws IOException {
        List<String> moving = new ArrayList<String>();
        moving.add(SHIPPED);
        moving.add(IN_TRANSIT);
        moving.add(OUT_FOR_DELIVERY);
        List<Fulfillment> fs = movingRead(moving);
        int changed = 0;
        for (int iter = 0; iter < fs.size(); iter++) {
            Fulfillment f = fs.get(iter);
            if (!carrier.name().equals(f.getCarrier()) || f.getTrackingNumber().length() == 0) {
                continue;
            }
            try {
                String to = advanceOne(f, now);
                if (to != null) {
                    changed++;
                    announce(f, to);
                }
            } catch (IOException | RuntimeException err) {
                System.err.println("[tracking] " + f.getTrackingNumber() + ": " + err);
            }
        }
        return changed;
    }

    /// Tells the shopper a parcel moved to `to` and republishes the order.
    private void announce(Fulfillment f, String to) throws IOException {
        if (OUT_FOR_DELIVERY.equals(to)) {
            notifyShopper(f.getOrderId(), "push.outForDelivery.title", "push.outForDelivery.body");
        } else if (DELIVERED.equals(to)) {
            notifyShopper(f.getOrderId(), "mail.delivered.title", "push.parcelDelivered.body");
            emails.delivered(f.getId());
        }
        orderService.publish(orderRead(f.getStoreId(), f.getOrderId()));
    }

    /// Scans a carrier pushed for `trackingNumber` (its signed webhook): recorded
    /// as polling records them, and the shopper hears the same news. Answers
    /// false when no parcel of this carrier has that number.
    public boolean trackingUpdate(String trackingNumber, List<CarrierProvider.Scan> scans)
            throws IOException {
        Fulfillment f = byTracking(trackingNumber);
        if (f == null || !carrier.name().equals(f.getCarrier())) {
            return false;
        }
        String to = recordScans(f.getId(), scans);
        if (to != null) {
            announce(f, to);
        }
        return true;
    }

    @Transactional(readOnly = true)
    private Fulfillment byTracking(String trackingNumber) throws IOException {
        return trackingNumber == null || trackingNumber.length() == 0 ? null
                : fulfillments.findByTrackingNumber(trackingNumber);
    }

    @Transactional(readOnly = true)
    private List<Fulfillment> movingRead(List<String> statuses) throws IOException {
        return fulfillments.findByStatusIn(statuses);
    }

    /// Answers the status the parcel moved to, or null.
    private String advanceOne(Fulfillment f, long now) throws IOException {
        Object[] ends = endsRead(f);
        long shippedAt = ((Long) ends[2]).longValue();
        List<CarrierProvider.Scan> scans = carrier.track(f.getTrackingNumber(), shippedAt, now,
                (CarrierProvider.Place) ends[0], (CarrierProvider.Place) ends[1]);
        return recordScans(f.getId(), scans);
    }

    /// `{from, to, shippedAt}` for a parcel.
    @Transactional(readOnly = true)
    private Object[] endsRead(Fulfillment f) throws IOException {
        long shippedAt = millis(f.getUpdatedAt());
        List<TrackingEvent> evs = tracking.findByFulfillmentIdOrderByCreatedAtAsc(f.getId());
        for (int iter = 0; iter < evs.size(); iter++) {
            if (SHIPPED.equals(evs.get(iter).getStatus())) {
                shippedAt = millis(evs.get(iter).getCreatedAt());
                break;
            }
        }
        return new Object[] {from(f.getLocationId()), to(orders.findById(f.getOrderId())),
            Long.valueOf(shippedAt)};
    }

    @Transactional(rollbackFor = IOException.class)
    private String recordScans(String id, List<CarrierProvider.Scan> scans) throws IOException {
        List<TrackingEvent> had = tracking.findByFulfillmentIdOrderByCreatedAtAsc(id);
        String latest = null;
        for (int iter = 0; iter < scans.size(); iter++) {
            CarrierProvider.Scan s = scans.get(iter);
            latest = s.status;
            if (!seen(had, s)) {
                scan(id, s.status, s.message, s.place, s.lat, s.lng, s.at);
            }
        }
        Fulfillment f = fulfillments.findById(id);
        if (latest == null || rank(latest) <= rank(f.getStatus())) {
            return null;
        }
        if (fulfillments.claimStatus(id, f.getStatus(), latest) != 1) {
            return null;
        }
        f = fulfillments.findById(id);
        f.setUpdatedAt(Instant.now());
        fulfillments.save(f);
        if (DELIVERED.equals(latest)) {
            orderService.event(f.getOrderId(), "DELIVERED", "Parcel " + f.getTrackingNumber()
                    + " delivered", "");
        }
        return latest;
    }

    private static boolean seen(List<TrackingEvent> had, CarrierProvider.Scan s) {
        for (int iter = 0; iter < had.size(); iter++) {
            TrackingEvent e = had.get(iter);
            if (e.getStatus().equals(s.status) && e.getMessage().equals(s.message)) {
                return true;
            }
        }
        return false;
    }

    private static int rank(String status) {
        if (SHIPPED.equals(status)) {
            return 1;
        }
        if (IN_TRANSIT.equals(status)) {
            return 2;
        }
        if (OUT_FOR_DELIVERY.equals(status)) {
            return 3;
        }
        return DELIVERED.equals(status) ? 4 : 0;
    }

    private void scan(String fulfillmentId, String status, String message, String place,
            double lat, double lng, long at) throws IOException {
        TrackingEvent e = new TrackingEvent();
        e.setId(Ids.next());
        e.setFulfillmentId(fulfillmentId);
        e.setStatus(status);
        e.setMessage(Codes.cut(message, 200));
        e.setPlace(Codes.cut(place, 120));
        e.setLat(lat);
        e.setLng(lng);
        e.setCreatedAt(Instant.ofEpochMilli(at));
        tracking.insert(e);
    }

    /// When the order's units last arrived or were collected; 0 when none did.
    @Transactional(readOnly = true)
    public long deliveredAt(String orderId) throws IOException {
        long out = 0;
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        for (int iter = 0; iter < fs.size(); iter++) {
            String s = fs.get(iter).getStatus();
            if (!DELIVERED.equals(s) && !PICKED_UP.equals(s)) {
                continue;
            }
            long at = millis(fs.get(iter).getUpdatedAt());
            List<TrackingEvent> evs = tracking.findByFulfillmentIdOrderByCreatedAtAsc(
                    fs.get(iter).getId());
            for (int e = 0; e < evs.size(); e++) {
                if (s.equals(evs.get(e).getStatus())) {
                    at = millis(evs.get(e).getCreatedAt());
                }
            }
            out = Math.max(out, at);
        }
        return out;
    }

    /// Units of each order line that arrived or were collected.
    @Transactional(readOnly = true)
    public Map<String, Integer> deliveredUnits(String orderId) throws IOException {
        Map<String, Integer> out = new HashMap<String, Integer>();
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        for (int iter = 0; iter < fs.size(); iter++) {
            String s = fs.get(iter).getStatus();
            if (!DELIVERED.equals(s) && !PICKED_UP.equals(s)) {
                continue;
            }
            List<FulfillmentLine> fl = fulfillmentLines.findByFulfillmentId(fs.get(iter).getId());
            for (int l = 0; l < fl.size(); l++) {
                Integer had = out.get(fl.get(l).getOrderLineId());
                out.put(fl.get(l).getOrderLineId(), Integer.valueOf(
                        (had == null ? 0 : had.intValue()) + fl.get(l).getQuantity()));
            }
        }
        return out;
    }

    // ------------------------------------------------------------- pushing

    /// Pushes `titleKey` and `bodyKey` ([StoreText] keys, `args` filled into
    /// the body) to the shopper's devices, in the order's language.
    private void notifyShopper(String orderId, String titleKey, String bodyKey,
            Object... args) {
        try {
            StoreOrder o = orderRead(null, orderId);
            if (o == null || o.getUsername().length() == 0) {
                return;
            }
            List<PushTarget> to = devices.targets(o.getUsername());
            if (to.isEmpty()) {
                return;
            }
            Map<String, String> t = StoreText.table(language.of(o));
            push.send(to, PushNotification.builder()
                    .title(StoreText.format(t, titleKey))
                    .body(StoreText.format(t, "push.order", o.getNumber(),
                            StoreText.format(t, bodyKey, args)))
                    .collapseKey("order-" + orderId)
                    .data("orderId", orderId)
                    .data("kind", "shipment")
                    .build(), null);
        } catch (IOException | RuntimeException err) {
            // A notification that cannot go must never undo a shipment.
            System.err.println("[push] nothing was sent for order " + orderId + ": " + err);
        }
    }

    // -------------------------------------------------------------- reading

    private FulfillmentDto published(String id) throws IOException {
        FulfillmentDto out = detail(id);
        orderService.publish(orderRead(null, out.orderId()));
        return out;
    }

    @Transactional(readOnly = true)
    public FulfillmentDto detail(String id) throws IOException {
        Fulfillment f = fulfillments.findById(id);
        return dto(f, orders.findById(f.getOrderId()), false);
    }

    @Transactional(readOnly = true)
    private StoreOrder orderRead(String storeId, String orderId) throws IOException {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (storeId != null && (o == null || !storeId.equals(o.getStoreId()))) {
            throw new ResponseStatusException(404, "No such order");
        }
        return o;
    }

    private StoreOrder orderIn(String storeId, String orderId) {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || !storeId.equals(o.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return o;
    }

    @Transactional(readOnly = true)
    private Fulfillment fulfillmentIn(String storeId, String id) {
        Fulfillment f = id == null ? null : fulfillments.findById(id);
        if (f == null || !storeId.equals(f.getStoreId())) {
            throw new ResponseStatusException(404, "No such fulfilment");
        }
        return f;
    }

    /// A fulfilment of the store, for the documents.
    @Transactional(readOnly = true)
    public Fulfillment require(String storeId, String id) {
        return fulfillmentIn(storeId, id);
    }

    /// Its lines with titles, for the documents.
    @Transactional(readOnly = true)
    public List<FulfilLineDto> linesOf(Fulfillment f) throws IOException {
        List<FulfilLineDto> out = new ArrayList<FulfilLineDto>();
        List<OrderLine> ls = lines.findByOrderId(f.getOrderId());
        List<FulfillmentLine> fl = fulfillmentLines.findByFulfillmentId(f.getId());
        for (int iter = 0; iter < fl.size(); iter++) {
            OrderLine line = find(ls, fl.get(iter).getOrderLineId());
            if (line != null) {
                String title = line.getVariantTitle().length() > 0 ? line.getTitle() + " - "
                        + line.getVariantTitle() : line.getTitle();
                out.add(new FulfilLineDto(line.getId(), title, line.getSku(),
                        fl.get(iter).getQuantity()));
            }
        }
        return out;
    }

    private FulfillmentDto dto(Fulfillment f, StoreOrder order, boolean anonymous)
            throws IOException {
        List<TrackingEventDto> evs = new ArrayList<TrackingEventDto>();
        List<TrackingEvent> rows = tracking.findByFulfillmentIdOrderByCreatedAtAsc(f.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            TrackingEvent e = rows.get(iter);
            evs.add(new TrackingEventDto(e.getStatus(), e.getMessage(), e.getPlace(), e.getLat(),
                    e.getLng(), millis(e.getCreatedAt())));
        }
        return new FulfillmentDto(f.getId(), anonymous ? "" : f.getOrderId(),
                anonymous || order == null ? 0 : order.getNumber(), f.getStoreId(),
                f.getLocationId(), method(f), f.getStatus(), f.getCarrier(), f.getService(),
                f.getTrackingNumber(), anonymous ? 0 : f.getLabelCents(), f.getWeightGrams(),
                millis(f.getCreatedAt()), millis(f.getUpdatedAt()),
                anonymous ? null : linesOf(f), evs, anonymous ? "" : f.getLabelUrl());
    }

    private CarrierProvider.Place from(String locationId) {
        Location l = locations.findById(locationId);
        if (l == null) {
            return new CarrierProvider.Place("", "", "", 0, 0);
        }
        return new CarrierProvider.Place(l.getName(), l.getAddress(), l.getName(), l.getLat(),
                l.getLng());
    }

    private static CarrierProvider.Place to(StoreOrder o) {
        return new CarrierProvider.Place(o.getShipName(), o.getShipLine1(), o.getShipCity(),
                o.getShipLat(), o.getShipLng(), o.getShipPostal(), o.getShipRegion(),
                o.getShipCountry());
    }

    @Transactional(readOnly = true)
    private String locationName(String id) {
        Location l = locations.findById(id);
        return l == null ? "the store" : l.getName();
    }

    private static String customer(StoreOrder o) {
        return o.getShipName().length() > 0 ? o.getShipName() : o.getEmail();
    }

    private static OrderLine find(List<OrderLine> rows, String id) {
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getId().equals(id)) {
                return rows.get(iter);
            }
        }
        return null;
    }

    static long millis(Instant at) {
        return at == null ? 0 : at.toEpochMilli();
    }

    private static boolean empty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
