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
package com.codenameone.examples.bazaar.ops;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.BinDto;
import com.codenameone.examples.bazaar.api.PickLineDto;
import com.codenameone.examples.bazaar.api.WarehouseMoveDto;
import com.codenameone.examples.bazaar.api.WarehouseTaskDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The warehouse board of a location: purchase orders arriving
/// (RECEIVING -> PUTAWAY -> DONE) and orders leaving (PICK -> PACK ->
/// DISPATCH -> DONE), the bins inside it and the pick list that walks them.
///
/// [#sync] puts on the board every placed purchase order bound for the
/// location and every open, unfulfilled order shipping from it (or from no
/// location in particular) that is not on it yet; a task is never made twice.
/// A move is checked by [WarehouseFlow#legal] and made by a conditional
/// update on the stage it starts from, so two people dragging the same card
/// cannot both win.
///
/// Permissions: reading is the inventory area's read, changing its write;
/// with no set assigned, the legacy STOCK roles.
@Component
public class Warehouse {
    static final int MAX_TASKS = 500;

    private final DataSource db;
    private final StoreAccess access;

    public Warehouse(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    private void location(String storeId, String locationId) throws IOException {
        Map row = locationId == null || locationId.length() > 32 ? null
                : db.queryOne("SELECT store_id FROM location WHERE id = ?", new Object[] {locationId});
        StoreAccess.requireIn(row == null ? null : Rows.text(row.get("store_id")), storeId, "location");
    }

    @Transactional(readOnly = true)
    public List<WarehouseTaskDto> board(String username, String storeId, String locationId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, locationId, StoreAccess.STOCK);
        location(storeId, locationId);
        List rows = db.query("SELECT id, location_id, kind, ref_id, label, stage, updated_at FROM "
                + "warehouse_task WHERE store_id = ? AND location_id = ? ORDER BY created_at, id",
                new Object[] {storeId, locationId});
        List<WarehouseTaskDto> out = new ArrayList<WarehouseTaskDto>();
        for (int iter = 0; iter < rows.size() && iter < MAX_TASKS; iter++) {
            out.add(task((Map) rows.get(iter)));
        }
        return out;
    }

    private static WarehouseTaskDto task(Map r) {
        String kind = Rows.text(r.get("kind"));
        return new WarehouseTaskDto(Rows.text(r.get("id")), Rows.text(r.get("location_id")), kind,
                Rows.text(r.get("ref_id")), Rows.text(r.get("label")), Rows.text(r.get("stage")),
                WarehouseFlow.INBOUND.equals(kind) ? "Inbound" : "Outbound",
                Rows.number(r.get("updated_at")));
    }

    @Transactional(rollbackFor = IOException.class)
    public List<WarehouseTaskDto> sync(String username, String storeId, String locationId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, locationId, StoreAccess.STOCK);
        location(storeId, locationId);
        long now = System.currentTimeMillis();
        List pos = db.query("SELECT id, number FROM purchase_order WHERE store_id = ? AND "
                + "location_id = ? AND status IN ('ORDERED', 'PARTIAL')",
                new Object[] {storeId, locationId});
        for (int iter = 0; iter < pos.size(); iter++) {
            Map r = (Map) pos.get(iter);
            add(storeId, locationId, WarehouseFlow.INBOUND, Rows.text(r.get("id")),
                    "PO #" + Rows.number(r.get("number")), now);
        }
        List open = db.query("SELECT id, number FROM store_order WHERE store_id = ? AND "
                + "(location_id = ? OR location_id = '' OR location_id IS NULL) AND status = 'OPEN' "
                + "AND fulfilment <> 'FULFILLED' AND channel <> 'POS' ORDER BY created_at",
                new Object[] {storeId, locationId});
        for (int iter = 0; iter < open.size() && iter < MAX_TASKS; iter++) {
            Map r = (Map) open.get(iter);
            add(storeId, locationId, WarehouseFlow.OUTBOUND, Rows.text(r.get("id")),
                    "Order #" + Rows.number(r.get("number")), now);
        }
        return board(username, storeId, locationId);
    }

    private void add(String storeId, String locationId, String kind, String refId, String label,
            long now) throws IOException {
        Map have = db.queryOne("SELECT id FROM warehouse_task WHERE location_id = ? AND kind = ? "
                + "AND ref_id = ?", new Object[] {locationId, kind, refId});
        if (have != null) {
            return;
        }
        db.execute("INSERT INTO warehouse_task (id, store_id, location_id, kind, ref_id, label, stage, "
                + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                    storeId, locationId, kind, refId, label, WarehouseFlow.first(kind),
                    Long.valueOf(now), Long.valueOf(now)});
    }

    @Transactional(rollbackFor = IOException.class)
    public WarehouseTaskDto move(String username, String storeId, WarehouseMoveDto move)
            throws IOException {
        access.requireStaff(username, storeId);
        Map r = move == null || move.taskId() == null || move.taskId().length() > 32 ? null
                : db.queryOne("SELECT id, store_id, location_id, kind, ref_id, label, stage, updated_at "
                + "FROM warehouse_task WHERE id = ?", new Object[] {move.taskId()});
        StoreAccess.requireIn(r == null ? null : Rows.text(r.get("store_id")), storeId, "task");
        String locationId = Rows.text(r.get("location_id"));
        access.requireArea(username, storeId, Perms.INVENTORY, true, locationId, StoreAccess.STOCK);
        String kind = Rows.text(r.get("kind"));
        String from = Rows.text(r.get("stage"));
        if (!WarehouseFlow.legal(kind, from, move.stage())) {
            throw new ResponseStatusException(409, "A task in " + from + " cannot go to "
                    + Codes.cut(move.stage() == null ? "" : move.stage(), 16));
        }
        int n = db.execute("UPDATE warehouse_task SET stage = ?, updated_at = ? WHERE id = ? AND "
                + "stage = ?", new Object[] {move.stage(), Long.valueOf(System.currentTimeMillis()),
                    move.taskId(), from});
        if (n != 1) {
            throw new ResponseStatusException(409, "Someone moved that task meanwhile; look again");
        }
        return task(db.queryOne("SELECT id, location_id, kind, ref_id, label, stage, updated_at FROM "
                + "warehouse_task WHERE id = ?", new Object[] {move.taskId()}));
    }

    // ----------------------------------------------------------------- bins

    @Transactional(readOnly = true)
    public List<BinDto> bins(String username, String storeId, String locationId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, locationId, StoreAccess.STOCK);
        location(storeId, locationId);
        List rows = db.query("SELECT b.id, b.location_id, b.code, b.aisle, b.shelf, b.position, "
                + "b.variant_id, v.sku AS sku FROM warehouse_bin b LEFT JOIN variant v ON v.id = "
                + "b.variant_id WHERE b.store_id = ? AND b.location_id = ?",
                new Object[] {storeId, locationId});
        int[][] keys = new int[rows.size()][];
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            keys[iter] = new int[] {(int) Rows.number(r.get("aisle")),
                (int) Rows.number(r.get("shelf")), (int) Rows.number(r.get("position"))};
        }
        int[] order = WarehouseFlow.pathOrder(keys);
        List<BinDto> out = new ArrayList<BinDto>();
        for (int iter = 0; iter < order.length; iter++) {
            Map r = (Map) rows.get(order[iter]);
            out.add(new BinDto(Rows.text(r.get("id")), Rows.text(r.get("location_id")),
                    Rows.text(r.get("code")), (int) Rows.number(r.get("aisle")),
                    (int) Rows.number(r.get("shelf")), (int) Rows.number(r.get("position")),
                    Rows.text(r.get("variant_id")), Rows.text(r.get("sku"))));
        }
        return out;
    }

    /// Adds a bin, or (same code) changes what it holds and where it is.
    @Transactional(rollbackFor = IOException.class)
    public List<BinDto> saveBin(String username, String storeId, String locationId, BinDto bin)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, locationId, StoreAccess.STOCK);
        location(storeId, locationId);
        String code = bin == null || bin.code() == null ? "" : bin.code().trim();
        if (code.length() == 0 || code.length() > 32) {
            throw new ResponseStatusException(400, "A bin has a code of up to 32 characters");
        }
        if (bin.aisle() < 0 || bin.shelf() < 0 || bin.position() < 0 || bin.aisle() > 9999
                || bin.shelf() > 9999 || bin.position() > 9999) {
            throw new ResponseStatusException(400, "Aisle, shelf and position are 0 to 9999");
        }
        String variant = bin.variantId() == null ? "" : bin.variantId();
        if (variant.length() > 0) {
            Map v = variant.length() > 32 ? null
                    : db.queryOne("SELECT store_id FROM variant WHERE id = ?", new Object[] {variant});
            if (v == null || !storeId.equals(Rows.text(v.get("store_id")))) {
                throw new ResponseStatusException(400, "No such variant");
            }
        }
        Map have = db.queryOne("SELECT id FROM warehouse_bin WHERE location_id = ? AND code = ?",
                new Object[] {locationId, code});
        if (have == null) {
            db.execute("INSERT INTO warehouse_bin (id, store_id, location_id, code, aisle, shelf, "
                    + "position, variant_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[] {Ids.next(), storeId, locationId, code, Integer.valueOf(bin.aisle()),
                        Integer.valueOf(bin.shelf()), Integer.valueOf(bin.position()), variant,
                        Long.valueOf(System.currentTimeMillis())});
        } else {
            db.execute("UPDATE warehouse_bin SET aisle = ?, shelf = ?, position = ?, variant_id = ? "
                    + "WHERE id = ? AND store_id = ?", new Object[] {Integer.valueOf(bin.aisle()),
                        Integer.valueOf(bin.shelf()), Integer.valueOf(bin.position()), variant,
                        Rows.text(have.get("id")), storeId});
        }
        return bins(username, storeId, locationId);
    }

    /// What the orders in PICK need, one line per variant, in the order the
    /// bins are walked; a variant in no bin comes last, under "-".
    @Transactional(readOnly = true)
    public List<PickLineDto> pickList(String username, String storeId, String locationId)
            throws IOException {
        List<BinDto> bins = bins(username, storeId, locationId);
        List rows = db.query("SELECT l.variant_id, l.title, l.sku, l.quantity, l.fulfilled_qty, "
                + "l.order_id FROM order_line l JOIN warehouse_task t ON t.ref_id = l.order_id WHERE "
                + "t.store_id = ? AND t.location_id = ? AND t.kind = 'OUTBOUND' AND t.stage = 'PICK'",
                new Object[] {storeId, locationId});
        Map<String, int[]> need = new HashMap<String, int[]>();
        Map<String, String[]> names = new HashMap<String, String[]>();
        Map<String, List<String>> orders = new HashMap<String, List<String>>();
        List<String> seen = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String v = Rows.text(r.get("variant_id"));
            int left = (int) (Rows.number(r.get("quantity")) - Rows.number(r.get("fulfilled_qty")));
            if (left <= 0) {
                continue;
            }
            int[] n = need.get(v);
            if (n == null) {
                n = new int[1];
                need.put(v, n);
                names.put(v, new String[] {Rows.text(r.get("sku")), Rows.text(r.get("title"))});
                orders.put(v, new ArrayList<String>());
                seen.add(v);
            }
            n[0] += left;
            String o = Rows.text(r.get("order_id"));
            if (!orders.get(v).contains(o)) {
                orders.get(v).add(o);
            }
        }
        List<PickLineDto> out = new ArrayList<PickLineDto>();
        for (int iter = 0; iter < bins.size(); iter++) {
            BinDto b = bins.get(iter);
            int[] n = need.get(b.variantId());
            if (n != null && n[0] > 0) {
                String[] nm = names.get(b.variantId());
                out.add(new PickLineDto(b.code(), b.variantId(), nm[0], nm[1], n[0],
                        orders.get(b.variantId()).size()));
                n[0] = 0;
            }
        }
        for (int iter = 0; iter < seen.size(); iter++) {
            String v = seen.get(iter);
            if (need.get(v)[0] > 0) {
                String[] nm = names.get(v);
                out.add(new PickLineDto("-", v, nm[0], nm[1], need.get(v)[0], orders.get(v).size()));
            }
        }
        return out;
    }
}
