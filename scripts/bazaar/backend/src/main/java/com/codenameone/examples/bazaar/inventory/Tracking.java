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
package com.codenameone.examples.bazaar.inventory;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.LotDto;
import com.codenameone.examples.bazaar.api.SerialAssignDto;
import com.codenameone.examples.bazaar.api.SerialDto;
import com.codenameone.examples.bazaar.api.TrackedReceiptDto;
import com.codenameone.examples.bazaar.api.TrackingDto;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Serial and lot tracking.
///
/// A variant is tracked NONE (the default), SERIAL or LOT. Receiving records
/// what arrived: a serial number per unit, or a lot code with its quantity and
/// expiry. Selling uses them up: serials are assigned to an order line when
/// they are scanned -- at fulfilment or at the register -- and lots are drawn
/// down first-expiring-first-out ([#pickLots]) when the units leave, with
/// every draw kept against the order so a recall can find who has a lot.
///
/// Tracking is a record beside the stock ledger, not a second ledger: on hand
/// is still what [Inventory] says, and receiving serials or a lot here does
/// not add units (the purchase order or count that brought them did).
@Component
public class Tracking {
    public static final String NONE = "NONE";
    public static final String SERIAL = "SERIAL";
    public static final String LOT = "LOT";
    public static final String IN_STOCK = "IN_STOCK";
    public static final String SOLD = "SOLD";
    /// Most serials in one receipt or one assignment.
    static final int MAX_SERIALS = 500;

    private final DataSource db;
    private final StoreAccess access;

    public Tracking(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// A variant's mode; NONE when it has no row.
    @Transactional(readOnly = true)
    public String mode(String variantId) throws IOException {
        Map r = variantId == null ? null : db.queryOne(
                "SELECT mode FROM variant_tracking WHERE variant_id = ?", new Object[] {variantId});
        return r == null ? NONE : Rows.text(r.get("mode"));
    }

    @Transactional(readOnly = true)
    public List<TrackingDto> tracked(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.ANYONE);
        List rows = db.query("SELECT t.variant_id AS variant_id, t.mode AS mode, v.title AS vt, "
                + "v.sku AS sku, p.title AS pt FROM variant_tracking t JOIN variant v ON "
                + "v.id = t.variant_id JOIN product p ON p.id = v.product_id WHERE t.store_id = ? "
                + "ORDER BY p.title, v.position", new Object[] {storeId});
        List<TrackingDto> out = new ArrayList<TrackingDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new TrackingDto(Rows.text(r.get("variant_id")), Rows.text(r.get("pt")),
                    Rows.text(r.get("vt")), Rows.text(r.get("sku")), Rows.text(r.get("mode"))));
        }
        return out;
    }

    /// Sets a variant's mode. A variant with serials or lots on hand keeps
    /// its mode: switching would orphan them.
    @Transactional(rollbackFor = IOException.class)
    public TrackingDto setMode(String username, String storeId, TrackingDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Name the variant");
        }
        String mode = wanted.mode() == null ? "" : wanted.mode();
        if (!NONE.equals(mode) && !SERIAL.equals(mode) && !LOT.equals(mode)) {
            throw new ResponseStatusException(400, "Tracking is NONE, SERIAL or LOT");
        }
        Map v = variant(storeId, wanted.variantId());
        String was = mode(wanted.variantId());
        if (!was.equals(mode) && inStock(wanted.variantId()) > 0) {
            throw new ResponseStatusException(409, "The variant has tracked units in stock; "
                    + "sell or remove them first");
        }
        db.execute("DELETE FROM variant_tracking WHERE variant_id = ?",
                new Object[] {wanted.variantId()});
        if (!NONE.equals(mode)) {
            db.execute("INSERT INTO variant_tracking (variant_id, store_id, mode, updated_at) "
                    + "VALUES (?, ?, ?, ?)", new Object[] {wanted.variantId(), storeId, mode,
                        Long.valueOf(System.currentTimeMillis())});
        }
        return new TrackingDto(wanted.variantId(), Rows.text(v.get("pt")), Rows.text(v.get("vt")),
                Rows.text(v.get("sku")), mode);
    }

    /// Records serials or a lot received at a location. Serials are unique in
    /// the store; a SERIAL receipt names one per unit, a LOT receipt a code,
    /// a quantity and an expiry (0 for none).
    @Transactional(rollbackFor = IOException.class)
    public List<SerialDto> receive(String username, String storeId, TrackedReceiptDto receipt)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        if (receipt == null) {
            throw new ResponseStatusException(400, "Say what arrived");
        }
        variant(storeId, receipt.variantId());
        location(storeId, receipt.locationId());
        String mode = mode(receipt.variantId());
        String reference = cut(receipt.reference(), 64);
        long now = System.currentTimeMillis();
        List<SerialDto> out = new ArrayList<SerialDto>();
        if (SERIAL.equals(mode)) {
            List<String> serials = clean(receipt.serials());
            if (serials.isEmpty()) {
                throw new ResponseStatusException(400, "Scan a serial for every unit");
            }
            if (receipt.quantity() > 0 && receipt.quantity() != serials.size()) {
                throw new ResponseStatusException(400, receipt.quantity() + " units need "
                        + receipt.quantity() + " serials, not " + serials.size());
            }
            for (int iter = 0; iter < serials.size(); iter++) {
                String serial = serials.get(iter);
                if (db.queryOne("SELECT id FROM tracked_unit WHERE store_id = ? AND serial = ?",
                        new Object[] {storeId, serial}) != null) {
                    throw new ResponseStatusException(409, "Serial " + serial
                            + " was already received");
                }
                db.execute("INSERT INTO tracked_unit (id, store_id, variant_id, location_id, "
                        + "serial, status, order_id, order_line_id, reference, received_at, "
                        + "sold_at) VALUES (?, ?, ?, ?, ?, ?, '', '', ?, ?, 0)", new Object[] {
                            Ids.next(), storeId, receipt.variantId(), receipt.locationId(), serial,
                            IN_STOCK, reference, Long.valueOf(now)});
            }
            for (int iter = 0; iter < serials.size(); iter++) {
                out.add(serial(storeId, serials.get(iter)));
            }
            return out;
        }
        if (LOT.equals(mode)) {
            String code = cut(receipt.lotCode(), 64);
            if (code.length() == 0 || receipt.quantity() <= 0 || receipt.quantity() > 1000000) {
                throw new ResponseStatusException(400, "A lot has a code and a quantity");
            }
            if (receipt.expiresAt() < 0) {
                throw new ResponseStatusException(400, "That is not an expiry");
            }
            db.execute("INSERT INTO stock_lot (id, store_id, variant_id, location_id, lot_code, "
                    + "expires_at, received_qty, remaining, reference, received_at) VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId,
                        receipt.variantId(), receipt.locationId(), code,
                        Long.valueOf(receipt.expiresAt()), Integer.valueOf(receipt.quantity()),
                        Integer.valueOf(receipt.quantity()), reference, Long.valueOf(now)});
            return out;
        }
        throw new ResponseStatusException(409, "The variant is not tracked by serial or lot");
    }

    /// Serials of the store equal to, or starting with, `q`.
    @Transactional(readOnly = true)
    public List<SerialDto> find(String username, String storeId, String q) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.ANYONE);
        String text = q == null ? "" : q.trim();
        if (text.length() == 0 || text.length() > 64) {
            throw new ResponseStatusException(400, "Type or scan a serial");
        }
        List rows = db.query(UNIT + " WHERE u.store_id = ? AND (u.serial = ? OR u.serial LIKE ?) "
                + "ORDER BY u.serial", new Object[] {storeId, text, like(text) + "%"});
        List<SerialDto> out = new ArrayList<SerialDto>();
        for (int iter = 0; iter < rows.size() && iter < 50; iter++) {
            out.add(unit((Map) rows.get(iter)));
        }
        return out;
    }

    /// A variant's lots, the soonest to expire first (lots with no expiry last).
    @Transactional(readOnly = true)
    public List<LotDto> lots(String username, String storeId, String variantId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.ANYONE);
        variant(storeId, variantId);
        List rows = db.query("SELECT * FROM stock_lot WHERE store_id = ? AND variant_id = ?",
                new Object[] {storeId, variantId});
        List<LotDto> out = new ArrayList<LotDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new LotDto(Rows.text(r.get("id")), Rows.text(r.get("variant_id")),
                    Rows.text(r.get("location_id")), Rows.text(r.get("lot_code")),
                    Rows.number(r.get("expires_at")), (int) Rows.number(r.get("received_qty")),
                    (int) Rows.number(r.get("remaining")), Rows.text(r.get("reference")),
                    Rows.number(r.get("received_at"))));
        }
        sortFefo(out);
        return out;
    }

    /// Assigns scanned serials to a line of an order: each must be the line's
    /// variant, in stock, and the line cannot take more serials than units.
    @Transactional(rollbackFor = IOException.class)
    public List<SerialDto> assign(String username, String storeId, String orderId,
            SerialAssignDto wanted) throws IOException {
        access.requireArea(username, storeId, Perms.ORDERS, true, StoreAccess.ANYONE);
        Map order = orderId == null || orderId.length() > 32 ? null : db.queryOne(
                "SELECT store_id FROM store_order WHERE id = ?", new Object[] {orderId});
        if (order == null || !storeId.equals(Rows.text(order.get("store_id")))) {
            throw new ResponseStatusException(404, "No such order");
        }
        if (wanted == null || wanted.orderLineId() == null) {
            throw new ResponseStatusException(400, "Name the order line");
        }
        Map line = db.queryOne("SELECT variant_id, quantity, refunded_qty FROM order_line "
                + "WHERE id = ? AND order_id = ?", new Object[] {wanted.orderLineId(), orderId});
        if (line == null) {
            throw new ResponseStatusException(404, "That line is not on the order");
        }
        String variantId = Rows.text(line.get("variant_id"));
        if (!SERIAL.equals(mode(variantId))) {
            throw new ResponseStatusException(409, "That item is not tracked by serial");
        }
        List<String> serials = clean(wanted.serials());
        if (serials.isEmpty()) {
            throw new ResponseStatusException(400, "Scan at least one serial");
        }
        long already = Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM tracked_unit WHERE "
                + "order_line_id = ?", new Object[] {wanted.orderLineId()}).get("n"));
        long room = Rows.number(line.get("quantity")) - Rows.number(line.get("refunded_qty"))
                - already;
        if (serials.size() > room) {
            throw new ResponseStatusException(409, "The line needs " + Math.max(0, room)
                    + " more serials");
        }
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < serials.size(); iter++) {
            int done = db.execute("UPDATE tracked_unit SET status = ?, order_id = ?, "
                    + "order_line_id = ?, sold_at = ? WHERE store_id = ? AND serial = ? AND "
                    + "variant_id = ? AND status = ?", new Object[] {SOLD, orderId,
                        wanted.orderLineId(), Long.valueOf(now), storeId, serials.get(iter),
                        variantId, IN_STOCK});
            if (done != 1) {
                throw new ResponseStatusException(409, "Serial " + serials.get(iter)
                        + " is not in stock for that item");
            }
        }
        List<SerialDto> out = new ArrayList<SerialDto>();
        for (int iter = 0; iter < serials.size(); iter++) {
            out.add(serial(storeId, serials.get(iter)));
        }
        return out;
    }

    /// Draws `qty` units of a LOT variant at a location from its lots, the
    /// soonest to expire first, and records each draw against `reference`
    /// (the order or the shipment). Units beyond what the lots hold are drawn from no lot: on
    /// hand is the ledger's, and a lot not recorded at receiving cannot be
    /// made up. Does nothing for a variant not tracked by lot. Each draw is a
    /// conditional update, so two shipments never take the same unit.
    @Transactional(rollbackFor = IOException.class)
    public void pickLots(String storeId, String variantId, String locationId, int qty,
            String reference) throws IOException {
        if (qty <= 0 || !LOT.equals(mode(variantId))) {
            return;
        }
        int left = qty;
        for (int attempt = 0; attempt < 20 && left > 0; attempt++) {
            List rows = db.query("SELECT id, lot_code, expires_at, remaining, received_at, "
                    + "variant_id, location_id, received_qty, reference FROM stock_lot WHERE "
                    + "store_id = ? AND variant_id = ? AND location_id = ? AND remaining > 0",
                    new Object[] {storeId, variantId, locationId});
            List<LotDto> lots = new ArrayList<LotDto>();
            for (int iter = 0; iter < rows.size(); iter++) {
                Map r = (Map) rows.get(iter);
                lots.add(new LotDto(Rows.text(r.get("id")), "", "", "",
                        Rows.number(r.get("expires_at")), 0, (int) Rows.number(r.get("remaining")),
                        "", Rows.number(r.get("received_at"))));
            }
            if (lots.isEmpty()) {
                return;
            }
            sortFefo(lots);
            boolean raced = false;
            for (int iter = 0; iter < lots.size() && left > 0; iter++) {
                LotDto lot = lots.get(iter);
                int take = Math.min(left, lot.remaining());
                if (db.execute("UPDATE stock_lot SET remaining = remaining - ? WHERE id = ? AND "
                        + "remaining >= ?", new Object[] {Integer.valueOf(take), lot.id(),
                            Integer.valueOf(take)}) != 1) {
                    raced = true;
                    break;
                }
                db.execute("INSERT INTO lot_pick (id, store_id, lot_id, reference, quantity, "
                        + "created_at) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                            storeId, lot.id(), cut(reference, 64), Integer.valueOf(take),
                            Long.valueOf(System.currentTimeMillis())});
                left -= take;
            }
            if (!raced) {
                return;
            }
        }
    }

    /// The lots drawn for an order -- at the register, or by any of its
    /// shipments -- as `lot code x quantity`.
    @Transactional(readOnly = true)
    public List<String> picks(String orderId) throws IOException {
        List rows = db.query("SELECT l.lot_code AS lot_code, l.expires_at AS expires_at, "
                + "p.quantity AS quantity FROM lot_pick p JOIN stock_lot l ON l.id = p.lot_id "
                + "WHERE p.reference = ? OR p.reference IN (SELECT f.id FROM fulfillment f WHERE "
                + "f.order_id = ?)", new Object[] {orderId, orderId});
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(Rows.text(r.get("lot_code")) + " x " + Rows.number(r.get("quantity")));
        }
        return out;
    }

    /// The serials sold on an order.
    @Transactional(readOnly = true)
    public List<SerialDto> ofOrder(String orderId) throws IOException {
        List rows = db.query(UNIT + " WHERE u.order_id = ? ORDER BY u.serial",
                new Object[] {orderId});
        List<SerialDto> out = new ArrayList<SerialDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(unit((Map) rows.get(iter)));
        }
        return out;
    }

    /// Soonest expiry first, lots with no expiry (0) last, then the oldest
    /// received first. An insertion sort: a variant has a handful of lots.
    static void sortFefo(List<LotDto> lots) {
        for (int iter = 1; iter < lots.size(); iter++) {
            LotDto key = lots.get(iter);
            int at = iter - 1;
            while (at >= 0 && before(key, lots.get(at))) {
                lots.set(at + 1, lots.get(at));
                at--;
            }
            lots.set(at + 1, key);
        }
    }

    private static boolean before(LotDto a, LotDto b) {
        long ea = a.expiresAt() <= 0 ? Long.MAX_VALUE : a.expiresAt();
        long eb = b.expiresAt() <= 0 ? Long.MAX_VALUE : b.expiresAt();
        if (ea != eb) {
            return ea < eb;
        }
        return a.receivedAt() < b.receivedAt();
    }

    private long inStock(String variantId) throws IOException {
        Map units = db.queryOne("SELECT COUNT(*) AS n FROM tracked_unit WHERE variant_id = ? AND "
                + "status = ?", new Object[] {variantId, IN_STOCK});
        Map lots = db.queryOne("SELECT COUNT(*) AS n FROM stock_lot WHERE variant_id = ? AND "
                + "remaining > 0", new Object[] {variantId});
        return Rows.number(units.get("n")) + Rows.number(lots.get("n"));
    }

    private SerialDto serial(String storeId, String serial) throws IOException {
        Map r = db.queryOne(UNIT + " WHERE u.store_id = ? AND u.serial = ?",
                new Object[] {storeId, serial});
        return unit(r);
    }

    private static final String UNIT = "SELECT u.serial AS serial, u.variant_id AS variant_id, "
            + "u.location_id AS location_id, u.status AS status, u.order_id AS order_id, "
            + "u.received_at AS received_at, u.sold_at AS sold_at, v.title AS vt, v.sku AS sku, "
            + "p.title AS pt, o.number AS number FROM tracked_unit u JOIN variant v ON "
            + "v.id = u.variant_id JOIN product p ON p.id = v.product_id LEFT JOIN store_order o "
            + "ON o.id = u.order_id";

    private static SerialDto unit(Map r) {
        return new SerialDto(Rows.text(r.get("serial")), Rows.text(r.get("variant_id")),
                Rows.text(r.get("pt")), Rows.text(r.get("vt")), Rows.text(r.get("sku")),
                Rows.text(r.get("location_id")), Rows.text(r.get("status")),
                Rows.text(r.get("order_id")), (int) Rows.number(r.get("number")),
                Rows.number(r.get("received_at")), Rows.number(r.get("sold_at")));
    }

    private Map variant(String storeId, String variantId) throws IOException {
        Map r = variantId == null || variantId.length() == 0 || variantId.length() > 32 ? null
                : db.queryOne("SELECT v.store_id AS store_id, v.title AS vt, v.sku AS sku, "
                + "p.title AS pt FROM variant v JOIN product p ON p.id = v.product_id WHERE "
                + "v.id = ?", new Object[] {variantId});
        if (r == null || !storeId.equals(Rows.text(r.get("store_id")))) {
            throw new ResponseStatusException(404, "No such variant");
        }
        return r;
    }

    private void location(String storeId, String locationId) throws IOException {
        Map r = locationId == null || locationId.length() == 0 || locationId.length() > 32 ? null
                : db.queryOne("SELECT store_id FROM location WHERE id = ?",
                new Object[] {locationId});
        if (r == null || !storeId.equals(Rows.text(r.get("store_id")))) {
            throw new ResponseStatusException(404, "No such location");
        }
    }

    /// Serials trimmed, blanks and repeats dropped; 400 past [#MAX_SERIALS].
    static List<String> clean(List<String> serials) {
        List<String> out = new ArrayList<String>();
        if (serials == null) {
            return out;
        }
        for (int iter = 0; iter < serials.size(); iter++) {
            String s = serials.get(iter) == null ? "" : serials.get(iter).trim();
            if (s.length() > 64) {
                throw new ResponseStatusException(400, "A serial is at most 64 characters");
            }
            if (s.length() > 0 && !out.contains(s)) {
                out.add(s);
            }
        }
        if (out.size() > MAX_SERIALS) {
            throw new ResponseStatusException(400, "At most " + MAX_SERIALS + " serials at once");
        }
        return out;
    }

    /// `text` with the LIKE wildcards taken literally (escaped as nothing:
    /// they are dropped, since a serial never holds them).
    private static String like(String text) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c != '%' && c != '_' && c != '\\') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String cut(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
