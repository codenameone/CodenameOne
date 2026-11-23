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
package com.codenameone.examples.bazaar.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.PlatformStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatusDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The platform admin's view across every store: how many, what they sold,
/// and switching one off. A suspended store vanishes from the public
/// directory and its shop; its staff still sign in and see it.
///
/// GMV is the total of the counted orders (not cancelled, not an
/// unconverted draft) less what was refunded on them, in each store's own
/// currency, so the platform total adds amounts in mixed currencies -- fine
/// for a demo where every store sells in one.
@Component
public class Platform {
    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDED = "SUSPENDED";

    private final StoreRepository stores;
    private final StoreOrderRepository orders;
    private final AuditEventRepository audits;

    public Platform(StoreRepository stores, StoreOrderRepository orders,
            AuditEventRepository audits) {
        this.stores = stores;
        this.orders = orders;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto stats() {
        List<Store> all = stores.findAll();
        List<StoreOrder> rows = orders.findByCreatedAtGreaterThanEqual(Instant.EPOCH);
        Map<String, long[]> per = new HashMap<String, long[]>();
        long today = Days.start(System.currentTimeMillis());
        int count = 0;
        int ordersToday = 0;
        long gmv = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreOrder o = rows.get(iter);
            if ("CANCELLED".equals(o.getStatus()) || Orders.isUnconvertedDraft(o)) {
                continue;
            }
            long[] s = per.get(o.getStoreId());
            if (s == null) {
                s = new long[2];
                per.put(o.getStoreId(), s);
            }
            long value = o.getTotalCents() - o.getRefundedCents();
            s[0]++;
            s[1] += value;
            count++;
            gmv += value;
            if (o.getCreatedAt().toEpochMilli() >= today) {
                ordersToday++;
            }
        }
        int active = 0;
        int suspended = 0;
        List<StoreStatsDto> table = new ArrayList<StoreStatsDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Store st = all.get(iter);
            if (ACTIVE.equals(st.getStatus())) {
                active++;
            } else if (SUSPENDED.equals(st.getStatus())) {
                suspended++;
            }
            table.add(dto(st, per.get(st.getId())));
        }
        return new PlatformStatsDto(all.size(), active, suspended, count, ordersToday, gmv, table);
    }

    @Transactional(rollbackFor = IOException.class)
    public StoreStatsDto setStatus(String admin, String storeId, StoreStatusDto wanted)
            throws IOException {
        String status = wanted == null || wanted.status() == null ? "" : wanted.status().trim();
        String next = ACTIVE.equalsIgnoreCase(status) ? ACTIVE
                : SUSPENDED.equalsIgnoreCase(status) ? SUSPENDED : "";
        if (next.length() == 0) {
            throw new ResponseStatusException(400, "A store is ACTIVE or SUSPENDED");
        }
        Store st = storeId == null ? null : stores.findById(storeId);
        if (st == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        st.setStatus(next);
        st.setUpdatedAt(Instant.now());
        stores.save(st);
        AuditEvent e = new AuditEvent();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setUsername(admin);
        e.setAction(SUSPENDED.equals(next) ? "STORE_SUSPENDED" : "STORE_ACTIVATED");
        e.setSubject(storeId);
        e.setDetail(Codes.cut(wanted.reason() == null ? "" : wanted.reason(), 500));
        e.setCreatedAt(Instant.now());
        audits.insert(e);
        long[] s = new long[2];
        List<StoreOrder> mine = orders.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.EPOCH);
        for (int iter = 0; iter < mine.size(); iter++) {
            StoreOrder o = mine.get(iter);
            if (!"CANCELLED".equals(o.getStatus()) && !Orders.isUnconvertedDraft(o)) {
                s[0]++;
                s[1] += o.getTotalCents() - o.getRefundedCents();
            }
        }
        return dto(st, s);
    }

    /// The organization overview: every store `owner` owns, by name, with its counted
    /// orders and sales less refunds, each in that store's own currency.
    @Transactional(readOnly = true)
    public List<StoreStatsDto> organization(String owner) {
        List<Store> owned = stores.findByOwnerUsernameOrderByNameAsc(owner);
        List<StoreStatsDto> out = new ArrayList<StoreStatsDto>();
        for (int iter = 0; iter < owned.size(); iter++) {
            Store st = owned.get(iter);
            long[] s = new long[2];
            List<StoreOrder> rows = orders.findByStoreIdAndCreatedAtGreaterThanEqual(st.getId(),
                    Instant.EPOCH);
            for (int o = 0; o < rows.size(); o++) {
                StoreOrder order = rows.get(o);
                if (!"CANCELLED".equals(order.getStatus()) && !Orders.isUnconvertedDraft(order)) {
                    s[0]++;
                    s[1] += order.getTotalCents() - order.getRefundedCents();
                }
            }
            out.add(dto(st, s));
        }
        return out;
    }

    private static StoreStatsDto dto(Store st, long[] s) {
        return new StoreStatsDto(st.getId(), st.getHandle(), st.getName(), st.getStatus(),
                st.getOwnerUsername(), s == null ? 0 : (int) s[0], s == null ? 0 : s[1],
                st.getCurrency(), st.getCreatedAt().toEpochMilli());
    }
}
