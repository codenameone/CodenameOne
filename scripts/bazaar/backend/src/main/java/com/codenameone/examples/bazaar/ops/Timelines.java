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
import com.codenameone.examples.bazaar.api.TimelineEventDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The life of a stock transfer or purchase order as a list of events:
/// CREATED, SENT, PARTIALLY_RECEIVED, RECEIVED and CLOSED (received in full,
/// or cancelled -- the message says which).
///
/// `Stock` writes them as it moves a transfer or order on; there is no
/// CLOSED status, so closing is an event of the terminal statuses, not a
/// state of its own. A transfer or order older than the timeline has no
/// events, and its creation is read from its own row instead.
@Component
public class Timelines {
    public static final String TRANSFER = "TRANSFER";
    public static final String PURCHASE = "PURCHASE";
    public static final String CREATED = "CREATED";
    public static final String SENT = "SENT";
    public static final String PARTIAL = "PARTIALLY_RECEIVED";
    public static final String RECEIVED = "RECEIVED";
    public static final String CLOSED = "CLOSED";

    private final DataSource db;
    private final StoreAccess access;

    public Timelines(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// Records an event; `username` may be empty for the system.
    public void record(String storeId, String subjectKind, String subjectId, String kind,
            String message, String username) throws IOException {
        db.execute("INSERT INTO ops_event (id, store_id, subject_kind, subject_id, kind, message, "
                + "username, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                    storeId, subjectKind, subjectId, kind, Codes.cut(message == null ? "" : message, 300),
                    username == null ? "" : Codes.cut(username, 200),
                    Long.valueOf(System.currentTimeMillis())});
    }

    /// The events a move to `status` amounts to (Stock's statuses).
    public void status(String storeId, String subjectKind, String subjectId, String status,
            String username) throws IOException {
        String what = TRANSFER.equals(subjectKind) ? "Transfer" : "Purchase order";
        if ("IN_TRANSIT".equals(status)) {
            record(storeId, subjectKind, subjectId, SENT, what + " sent", username);
        } else if ("ORDERED".equals(status)) {
            record(storeId, subjectKind, subjectId, SENT, what + " sent to the supplier", username);
        } else if ("PARTIAL".equals(status)) {
            record(storeId, subjectKind, subjectId, PARTIAL, "Partly received", username);
        } else if ("RECEIVED".equals(status)) {
            record(storeId, subjectKind, subjectId, RECEIVED, "Received in full", username);
            record(storeId, subjectKind, subjectId, CLOSED, what + " closed", username);
        } else if ("CANCELLED".equals(status)) {
            record(storeId, subjectKind, subjectId, CLOSED, what + " cancelled", username);
        }
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDto> transfer(String username, String storeId, String transferId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        return events(storeId, TRANSFER, transferId, "stock_transfer", "Transfer created");
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDto> purchase(String username, String storeId, String orderId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        return events(storeId, PURCHASE, orderId, "purchase_order", "Purchase order created");
    }

    private List<TimelineEventDto> events(String storeId, String kind, String id, String table,
            String created) throws IOException {
        // The table name is one of two constants, never input.
        Map row = id == null || id.length() > 32 ? null : db.queryOne("SELECT store_id, created_at FROM "
                + table + " WHERE id = ?", new Object[] {id});
        if (row == null || !storeId.equals(Rows.text(row.get("store_id")))) {
            throw new ResponseStatusException(404, "No such " + (TRANSFER.equals(kind)
                    ? "transfer" : "purchase order"));
        }
        List rows = db.query("SELECT kind, message, username, created_at FROM ops_event WHERE "
                + "subject_id = ? AND store_id = ? AND subject_kind = ? ORDER BY created_at, id",
                new Object[] {id, storeId, kind});
        List<TimelineEventDto> out = new ArrayList<TimelineEventDto>();
        boolean hasCreated = false;
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String k = Rows.text(r.get("kind"));
            hasCreated = hasCreated || CREATED.equals(k);
            out.add(new TimelineEventDto(k, Rows.text(r.get("message")),
                    Rows.text(r.get("username")), Rows.number(r.get("created_at"))));
        }
        if (!hasCreated) {
            out.add(0, new TimelineEventDto(CREATED, created, "", createdAt(row.get("created_at"))));
        }
        return out;
    }

    private static long createdAt(Object v) {
        long m = Rows.millis(v);
        return m != 0 ? m : Rows.number(v);
    }
}
