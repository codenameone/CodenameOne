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
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.InventoryStateDto;
import com.codenameone.examples.bazaar.api.UnavailableDto;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.repository.InventoryMoveRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The states a unit of stock can be in, beyond the on hand and available
/// [Inventory] keeps: committed to an open order, incoming from a purchase
/// order or transfer, and unavailable -- on the premises but not for sale --
/// in one of three buckets: damaged, held for quality control, or kept back
/// as safety stock.
///
/// A unit moved into a bucket leaves the level's `on_hand` (an
/// `UNAVAILABLE` move in the ledger) and is counted in the bucket instead, so
/// every place that works out `available = on_hand - committed` stays right
/// without knowing the buckets exist, and the ledger still sums to on hand.
/// The total on the premises is `on_hand` plus the buckets. A stock count
/// counts sellable units: the buckets are counted by moving units in and out.
@Component
public class InventoryStates {
    public static final String DAMAGED = "DAMAGED";
    public static final String QUALITY_CONTROL = "QUALITY_CONTROL";
    public static final String SAFETY_STOCK = "SAFETY_STOCK";
    /// The ledger reason of a move into or out of a bucket.
    public static final String UNAVAILABLE = "UNAVAILABLE";

    private final DataSource db;
    private final StoreAccess access;
    private final Inventory inventory;
    private final InventoryMoveRepository moves;

    public InventoryStates(DataSource db, StoreAccess access, Inventory inventory,
            InventoryMoveRepository moves) {
        this.db = db;
        this.access = access;
        this.inventory = inventory;
        this.moves = moves;
    }

    /// Every variant at every location of the store, or at `locationId` only.
    @Transactional(readOnly = true)
    public List<InventoryStateDto> states(String username, String storeId, String locationId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.ANYONE);
        boolean one = locationId != null && locationId.length() > 0;
        List rows = db.query(SELECT + " WHERE l.store_id = ?" + (one ? " AND l.location_id = ?" : "")
                + " ORDER BY p.title, v.position, loc.name",
                one ? new Object[] {storeId, locationId} : new Object[] {storeId});
        List<InventoryStateDto> out = new ArrayList<InventoryStateDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto((Map) rows.get(iter)));
        }
        return out;
    }

    /// One variant at one location, 404 unless both are the store's.
    @Transactional(readOnly = true)
    public InventoryStateDto state(String storeId, String variantId, String locationId)
            throws IOException {
        Map r = db.queryOne(SELECT + " WHERE l.store_id = ? AND l.variant_id = ? AND "
                + "l.location_id = ?", new Object[] {storeId, variantId, locationId});
        if (r == null) {
            throw new ResponseStatusException(404, "No stock of that variant there");
        }
        return dto(r);
    }

    /// Moves units into a bucket (`quantity` above zero), taking them from
    /// what is available, or back out of it. Never takes units promised to
    /// an order, and never empties a bucket below zero; either is a 409.
    @Transactional(rollbackFor = IOException.class)
    public InventoryStateDto move(String username, String storeId, UnavailableDto move)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        if (move == null || move.quantity() == 0 || Math.abs(move.quantity()) > 1000000) {
            throw new ResponseStatusException(400, "Move at least one unit");
        }
        String column = column(move.reason());
        mine("variant", move.variantId(), storeId);
        mine("location", move.locationId(), storeId);
        InventoryLevel level = inventory.level(storeId, move.variantId(), move.locationId());
        int n = Math.abs(move.quantity());
        int changed;
        if (move.quantity() > 0) {
            changed = db.execute("UPDATE inventory_level SET on_hand = on_hand - ?, " + column
                    + " = " + column + " + ?, version = version + 1, updated_at = ? WHERE id = ? "
                    + "AND on_hand - committed >= ?", new Object[] {Integer.valueOf(n),
                        Integer.valueOf(n), Long.valueOf(System.currentTimeMillis()), level.getId(),
                        Integer.valueOf(n)});
            if (changed != 1) {
                throw new ResponseStatusException(409, "Only units that are available can be "
                        + "set aside");
            }
        } else {
            changed = db.execute("UPDATE inventory_level SET on_hand = on_hand + ?, " + column
                    + " = " + column + " - ?, version = version + 1, updated_at = ? WHERE id = ? "
                    + "AND " + column + " >= ?", new Object[] {Integer.valueOf(n),
                        Integer.valueOf(n), Long.valueOf(System.currentTimeMillis()), level.getId(),
                        Integer.valueOf(n)});
            if (changed != 1) {
                throw new ResponseStatusException(409, "There are not that many units in the "
                        + "bucket");
            }
        }
        moves.insert(Inventory.newMove(storeId, move.variantId(), move.locationId(),
                move.quantity() > 0 ? -n : n, 0, UNAVAILABLE, move.reason(), username));
        return state(storeId, move.variantId(), move.locationId());
    }

    /// The column of a bucket; 400 for anything else, so no text from the
    /// request reaches the statement.
    static String column(String reason) {
        if (DAMAGED.equals(reason)) {
            return "damaged";
        }
        if (QUALITY_CONTROL.equals(reason)) {
            return "quality_control";
        }
        if (SAFETY_STOCK.equals(reason)) {
            return "safety_stock";
        }
        throw new ResponseStatusException(400, "A bucket is DAMAGED, QUALITY_CONTROL or "
                + "SAFETY_STOCK");
    }

    private void mine(String table, String id, String storeId) throws IOException {
        Map r = id == null || id.length() == 0 || id.length() > 32 ? null
                : db.queryOne("SELECT store_id FROM " + table + " WHERE id = ?", new Object[] {id});
        if (r == null || !storeId.equals(Rows.text(r.get("store_id")))) {
            throw new ResponseStatusException(404, "No such " + table);
        }
    }

    private static final String SELECT = "SELECT l.variant_id AS variant_id, l.location_id AS "
            + "location_id, l.on_hand AS on_hand, l.committed AS committed, l.incoming AS incoming, "
            + "l.damaged AS damaged, l.quality_control AS quality_control, l.safety_stock AS "
            + "safety_stock, loc.name AS location_name, v.title AS variant_title, v.sku AS sku, "
            + "v.allow_backorder AS allow_backorder, p.title AS product_title FROM inventory_level l "
            + "JOIN location loc ON loc.id = l.location_id JOIN variant v ON v.id = l.variant_id "
            + "JOIN product p ON p.id = v.product_id";

    private static InventoryStateDto dto(Map r) {
        int onHand = (int) Rows.number(r.get("on_hand"));
        int committed = (int) Rows.number(r.get("committed"));
        int damaged = (int) Rows.number(r.get("damaged"));
        int qc = (int) Rows.number(r.get("quality_control"));
        int safety = (int) Rows.number(r.get("safety_stock"));
        return new InventoryStateDto(Rows.text(r.get("variant_id")), Rows.text(r.get("location_id")),
                Rows.text(r.get("location_name")), Rows.text(r.get("product_title")),
                Rows.text(r.get("variant_title")), Rows.text(r.get("sku")),
                onHand + damaged + qc + safety, onHand - committed, committed,
                (int) Rows.number(r.get("incoming")), damaged, qc, safety,
                Rows.number(r.get("allow_backorder")) != 0);
    }
}
