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
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.ForecastDto;
import com.codenameone.examples.bazaar.api.PurchaseLineDto;
import com.codenameone.examples.bazaar.api.PurchaseOrderDto;
import com.codenameone.examples.bazaar.api.SuggestionDto;
import com.codenameone.examples.bazaar.api.TransferDto;
import com.codenameone.examples.bazaar.api.TransferLineDto;
import com.codenameone.examples.bazaar.inventory.Stock;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Demand forecasts per variant per location from the last
/// [Forecast#WINDOW] days of sales, and the drafts they suggest: purchase
/// orders for locations running short, transfers from locations holding
/// more than they sell. The arithmetic is [Forecast]'s; this reads the rows.
///
/// A sale counts at the location its line shipped from, else its order's,
/// else the store's first location. Cancelled orders do not count.
/// Drafts are made through [Stock], with its own checks and timeline.
@Component
public class Forecasts {
    static final int MAX_LINES = 20000;

    private final DataSource db;
    private final StoreAccess access;
    private final Stock stock;

    public Forecasts(DataSource db, StoreAccess access, Stock stock) {
        this.db = db;
        this.access = access;
        this.stock = stock;
    }

    /// The forecast rows with the state the suggestions need.
    static final class Cell {
        String variantId;
        String sku = "";
        String title = "";
        String locationId;
        String locationName = "";
        int available;
        int incoming;
        int[] units = new int[Forecast.WINDOW];
        long daily;
    }

    List<Cell> cells(String storeId, long now) throws IOException {
        long today = Days.number(now);
        long firstDay = today - Forecast.WINDOW + 1;
        Map<String, Cell> byKey = new HashMap<String, Cell>();
        List<Cell> out = new ArrayList<Cell>();
        Map<String, String> names = new HashMap<String, String>();
        List locs = db.query("SELECT id, name FROM location WHERE store_id = ? ORDER BY position, id",
                new Object[] {storeId});
        String fallback = "";
        for (int iter = 0; iter < locs.size(); iter++) {
            Map r = (Map) locs.get(iter);
            names.put(Rows.text(r.get("id")), Rows.text(r.get("name")));
            if (iter == 0) {
                fallback = Rows.text(r.get("id"));
            }
        }
        List levels = db.query("SELECT l.variant_id, l.location_id, l.on_hand, l.committed, "
                + "l.incoming, v.sku AS sku, v.title AS vtitle, p.title AS ptitle FROM inventory_level l "
                + "JOIN variant v ON v.id = l.variant_id JOIN product p ON p.id = v.product_id "
                + "WHERE l.store_id = ?", new Object[] {storeId});
        for (int iter = 0; iter < levels.size(); iter++) {
            Map r = (Map) levels.get(iter);
            Cell c = new Cell();
            c.variantId = Rows.text(r.get("variant_id"));
            c.locationId = Rows.text(r.get("location_id"));
            c.locationName = names.containsKey(c.locationId) ? names.get(c.locationId) : "";
            c.available = (int) (Rows.number(r.get("on_hand")) - Rows.number(r.get("committed")));
            c.incoming = (int) Rows.number(r.get("incoming"));
            c.sku = Rows.text(r.get("sku"));
            String vt = Rows.text(r.get("vtitle"));
            c.title = Rows.text(r.get("ptitle")) + (vt.length() == 0 || "Default".equals(vt) ? ""
                    : " / " + vt);
            byKey.put(c.variantId + "/" + c.locationId, c);
            out.add(c);
        }
        List sold = db.query("SELECT l.variant_id, l.location_id AS lloc, o.location_id AS oloc, "
                + "l.quantity, o.created_at FROM order_line l JOIN store_order o ON o.id = l.order_id "
                + "WHERE o.store_id = ? AND o.created_at >= ? AND o.status <> 'CANCELLED' "
                + "AND o.status <> 'DRAFT'", new Object[] {storeId, Long.valueOf(firstDay * Days.MILLIS)});
        for (int iter = 0; iter < sold.size() && iter < MAX_LINES; iter++) {
            Map r = (Map) sold.get(iter);
            String loc = Rows.text(r.get("lloc"));
            if (loc.length() == 0) {
                loc = Rows.text(r.get("oloc"));
            }
            if (loc.length() == 0) {
                loc = fallback;
            }
            Cell c = byKey.get(Rows.text(r.get("variant_id")) + "/" + loc);
            long at = Rows.millis(r.get("created_at"));
            if (at == 0) {
                at = Rows.number(r.get("created_at"));
            }
            int index = (int) (Days.number(at) - firstDay);
            if (c != null && index >= 0 && index < Forecast.WINDOW) {
                c.units[index] += (int) Rows.number(r.get("quantity"));
            }
        }
        for (int iter = 0; iter < out.size(); iter++) {
            out.get(iter).daily = Forecast.baseX100(out.get(iter).units);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ForecastDto> forecast(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.STOCK);
        long now = System.currentTimeMillis();
        long today = Days.number(now);
        List<Cell> cells = cells(storeId, now);
        List<ForecastDto> out = new ArrayList<ForecastDto>();
        for (int iter = 0; iter < cells.size(); iter++) {
            Cell c = cells.get(iter);
            out.add(new ForecastDto(c.variantId, c.sku, c.title, c.locationId, c.locationName,
                    c.available, c.incoming, c.daily, Forecast.forecastX100(c.units, today, 7),
                    Forecast.daysOfCover(c.available, c.daily),
                    Forecast.reorderQuantity(c.available, c.incoming, c.daily)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<SuggestionDto> suggestions(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.STOCK);
        return suggest(cells(storeId, System.currentTimeMillis()));
    }

    static List<SuggestionDto> suggest(List<Cell> cells) {
        List<SuggestionDto> out = new ArrayList<SuggestionDto>();
        Map<String, List<Cell>> byVariant = new HashMap<String, List<Cell>>();
        List<String> order = new ArrayList<String>();
        for (int iter = 0; iter < cells.size(); iter++) {
            Cell c = cells.get(iter);
            List<Cell> l = byVariant.get(c.variantId);
            if (l == null) {
                l = new ArrayList<Cell>();
                byVariant.put(c.variantId, l);
                order.add(c.variantId);
            }
            l.add(c);
        }
        for (int v = 0; v < order.size(); v++) {
            List<Cell> l = byVariant.get(order.get(v));
            long[] avail = new long[l.size()];
            long[] daily = new long[l.size()];
            for (int iter = 0; iter < l.size(); iter++) {
                avail[iter] = l.get(iter).available;
                daily[iter] = l.get(iter).daily;
            }
            List<Forecast.Move> moves = Forecast.rebalance(avail, daily);
            int[] got = new int[l.size()];
            for (int iter = 0; iter < moves.size(); iter++) {
                Forecast.Move m = moves.get(iter);
                Cell from = l.get(m.from);
                Cell to = l.get(m.to);
                got[m.to] += m.quantity;
                out.add(new SuggestionDto("REBALANCE", to.variantId, to.sku, to.title,
                        from.locationId, to.locationId, m.quantity, from.locationName + " has "
                        + Forecast.daysOfCover(from.available, from.daily) + " days of cover, "
                        + to.locationName + " " + Forecast.daysOfCover(to.available, to.daily)));
            }
            for (int iter = 0; iter < l.size(); iter++) {
                Cell c = l.get(iter);
                int q = Forecast.reorderQuantity(c.available + got[iter], c.incoming, c.daily);
                if (q > 0) {
                    out.add(new SuggestionDto("REORDER", c.variantId, c.sku, c.title, "",
                            c.locationId, q, Forecast.daysOfCover(c.available, c.daily)
                            + " days of cover at " + c.locationName));
                }
            }
        }
        return out;
    }

    /// One draft purchase order per location short of stock, from the
    /// store's first supplier.
    @Transactional(rollbackFor = IOException.class)
    public List<PurchaseOrderDto> draftReorders(String username, String storeId, String locationId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        List<SuggestionDto> all = suggest(cells(storeId, System.currentTimeMillis()));
        Map supplier = db.queryOne("SELECT id FROM supplier WHERE store_id = ? ORDER BY name",
                new Object[] {storeId});
        Map<String, List<PurchaseLineDto>> byLocation = new HashMap<String, List<PurchaseLineDto>>();
        List<String> order = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            SuggestionDto s = all.get(iter);
            if (!"REORDER".equals(s.kind()) || locationId != null && locationId.length() > 0
                    && !locationId.equals(s.toLocationId())) {
                continue;
            }
            List<PurchaseLineDto> l = byLocation.get(s.toLocationId());
            if (l == null) {
                l = new ArrayList<PurchaseLineDto>();
                byLocation.put(s.toLocationId(), l);
                order.add(s.toLocationId());
            }
            l.add(new PurchaseLineDto(null, s.variantId(), s.sku(), s.title(), s.quantity(), 0, 0));
        }
        List<PurchaseOrderDto> out = new ArrayList<PurchaseOrderDto>();
        if (order.isEmpty()) {
            return out;
        }
        if (supplier == null) {
            throw new ResponseStatusException(409, "Add a supplier before drafting purchase orders");
        }
        for (int iter = 0; iter < order.size(); iter++) {
            out.add(stock.createPurchaseOrder(username, storeId, new PurchaseOrderDto(null, 0,
                    Rows.text(supplier.get("id")), null, order.get(iter), null, 0,
                    "Suggested by the demand forecast", 0, byLocation.get(order.get(iter)), 0)));
        }
        return out;
    }

    /// One draft transfer per pair of locations the suggestions move stock between.
    @Transactional(rollbackFor = IOException.class)
    public List<TransferDto> draftTransfers(String username, String storeId, String locationId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        List<SuggestionDto> all = suggest(cells(storeId, System.currentTimeMillis()));
        Map<String, List<TransferLineDto>> byPair = new HashMap<String, List<TransferLineDto>>();
        List<String> order = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            SuggestionDto s = all.get(iter);
            if (!"REBALANCE".equals(s.kind()) || locationId != null && locationId.length() > 0
                    && !locationId.equals(s.toLocationId()) && !locationId.equals(s.fromLocationId())) {
                continue;
            }
            String key = s.fromLocationId() + "/" + s.toLocationId();
            List<TransferLineDto> l = byPair.get(key);
            if (l == null) {
                l = new ArrayList<TransferLineDto>();
                byPair.put(key, l);
                order.add(key);
            }
            l.add(new TransferLineDto(null, s.variantId(), s.sku(), s.title(), s.quantity(), 0));
        }
        List<TransferDto> out = new ArrayList<TransferDto>();
        for (int iter = 0; iter < order.size(); iter++) {
            String key = order.get(iter);
            int slash = key.indexOf('/');
            out.add(stock.createTransfer(username, storeId, new TransferDto(null,
                    key.substring(0, slash), key.substring(slash + 1), null,
                    "Suggested by the demand forecast", byPair.get(key), 0)));
        }
        return out;
    }
}
