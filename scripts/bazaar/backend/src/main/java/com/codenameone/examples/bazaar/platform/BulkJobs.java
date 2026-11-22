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
package com.codenameone.examples.bazaar.platform;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.BulkJobDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Bulk operations: an export of a store's products or orders as JSON Lines,
/// or an import of products from them, run in the background.
///
/// Starting one only writes a `QUEUED` row; the scheduled [#runQueued] picks
/// queued jobs up, runs each to `DONE` or `FAILED`, and keeps the result in
/// the row for `result.jsonl` to answer. Both staff (the merchant API) and
/// API keys (the key API) start them, through [#start].
///
/// - **Exports** write one JSON object per line: the same product or order
///   the merchant API answers, at most [#MAX_LINES] of them.
/// - **Imports** read one product per line. A line with an `id` of one of the
///   store's products updates it; a line without one creates a product; an id
///   the store does not have is refused, never looked up elsewhere. Every line
///   stands alone: a refused line is reported as `line N: reason` and the rest
///   go on. At most [#MAX_LINES] lines and [#MAX_INPUT] characters.
///
/// The catalog and order services check staff, and a key is nobody's account:
/// work a key asked for runs with the store owner's authority, which is what
/// the owner gave the key when choosing its scopes.
@Component
public class BulkJobs {
    public static final String EXPORT_PRODUCTS = "EXPORT_PRODUCTS";
    public static final String EXPORT_ORDERS = "EXPORT_ORDERS";
    public static final String IMPORT_PRODUCTS = "IMPORT_PRODUCTS";
    public static final String QUEUED = "QUEUED";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";
    static final int MAX_LINES = 5000;
    static final int MAX_INPUT = 2000000;
    static final int MAX_ERRORS = 50;
    /// How long a job may run before it is taken to have been abandoned.
    static final long STALE = 15L * 60000L;

    private final DataSource db;
    private final StoreAccess access;
    private final StoreRepository stores;
    private final Catalog catalog;
    private final Orders orders;

    public BulkJobs(DataSource db, StoreAccess access, StoreRepository stores, Catalog catalog,
            Orders orders) {
        this.db = db;
        this.access = access;
        this.stores = stores;
        this.catalog = catalog;
        this.orders = orders;
    }

    /// The scope a key needs to start a job of `kind`.
    public static String scopeOf(String kind) {
        if (EXPORT_ORDERS.equals(kind)) {
            return StoreApiKeys.READ_ORDERS;
        }
        if (IMPORT_PRODUCTS.equals(kind)) {
            return StoreApiKeys.WRITE_PRODUCTS;
        }
        if (EXPORT_PRODUCTS.equals(kind)) {
            return StoreApiKeys.READ_PRODUCTS;
        }
        throw new ResponseStatusException(400, "There is no such bulk operation");
    }

    // ------------------------------------------------------------- staff

    @Transactional(readOnly = true)
    public List<BulkJobDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return jobs(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public BulkJobDto startAsStaff(String username, String storeId, BulkJobDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return start(storeId, username, wanted);
    }

    @Transactional(readOnly = true)
    public BulkJobDto job(String username, String storeId, String jobId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return dto(row(storeId, jobId));
    }

    /// The job's output, for `result.jsonl`.
    @Transactional(readOnly = true)
    public String result(String username, String storeId, String jobId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return output(storeId, jobId);
    }

    // ----------------------------------------------------- staff and keys

    /// Queues a job for the store, once the caller has been judged.
    @Transactional(rollbackFor = IOException.class)
    public BulkJobDto start(String storeId, String requestedBy, BulkJobDto wanted)
            throws IOException {
        String kind = wanted == null || wanted.kind() == null ? "" : wanted.kind().trim();
        scopeOf(kind);
        String input = wanted.input() == null ? "" : wanted.input();
        if (IMPORT_PRODUCTS.equals(kind)) {
            if (input.trim().length() == 0) {
                throw new ResponseStatusException(400, "Give the products to import, one per line");
            }
            if (input.length() > MAX_INPUT) {
                throw new ResponseStatusException(413, "Import at most " + MAX_INPUT
                        + " characters at a time");
            }
        } else {
            input = "";
        }
        Map busy = db.queryOne("SELECT COUNT(*) AS n FROM bulk_job WHERE store_id = ? AND "
                + "status IN ('QUEUED', 'RUNNING')", new Object[] {storeId});
        if (busy != null && Rows.number(busy.get("n")) >= 5) {
            throw new ResponseStatusException(429, "Five bulk operations are already waiting");
        }
        String id = Ids.next();
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO bulk_job (id, store_id, kind, status, requested_by, input, output, "
                + "errors, line_count, error_count, created_at, finished_at) VALUES "
                + "(?, ?, ?, ?, ?, ?, '', '', 0, 0, ?, 0)", new Object[] {id, storeId, kind, QUEUED,
                    requestedBy, input, Long.valueOf(now)});
        return dto(row(storeId, id));
    }

    @Transactional(readOnly = true)
    public BulkJobDto jobOf(String storeId, String jobId) throws IOException {
        return dto(row(storeId, jobId));
    }

    @Transactional(readOnly = true)
    public String output(String storeId, String jobId) throws IOException {
        Map r = row(storeId, jobId);
        if (!DONE.equals(Rows.text(r.get("status")))) {
            throw new ResponseStatusException(409, "The job has not finished");
        }
        return Rows.text(r.get("output"));
    }

    // ------------------------------------------------------------ running

    @Scheduled(fixedDelay = 5000, initialDelay = 15000, lock = "bazaar-bulk-jobs")
    public void run() throws IOException {
        runQueued();
    }

    /// Runs every queued job; answers how many ran. Public so tests need not
    /// wait for the schedule.
    public int runQueued() throws IOException {
        // A job left RUNNING by a server that stopped mid-way goes back in
        // the queue; `finished_at` holds when a running job was claimed.
        long now = System.currentTimeMillis();
        db.execute("UPDATE bulk_job SET status = ?, finished_at = 0 WHERE status = ? AND "
                + "finished_at < ?", new Object[] {QUEUED, RUNNING, Long.valueOf(now - STALE)});
        List rows = db.query("SELECT id AS id, store_id AS store_id FROM bulk_job WHERE status = ? "
                + "ORDER BY created_at", new Object[] {QUEUED});
        int ran = 0;
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String id = Rows.text(r.get("id"));
            // Claimed by a conditional update, so two runners never share one.
            int claimed = db.execute("UPDATE bulk_job SET status = ?, finished_at = ? WHERE id = ? "
                    + "AND status = ?", new Object[] {RUNNING, Long.valueOf(now), id, QUEUED});
            if (claimed == 1) {
                runOne(Rows.text(r.get("store_id")), id);
                ran++;
            }
        }
        return ran;
    }

    private void runOne(String storeId, String id) throws IOException {
        Map r = db.queryOne("SELECT kind AS kind, input AS input FROM bulk_job WHERE id = ?",
                new Object[] {id});
        String kind = Rows.text(r.get("kind"));
        StringBuilder out = new StringBuilder();
        List<String> errors = new ArrayList<String>();
        int[] lines = {0, 0};
        String status = DONE;
        // Looked up outside the try below: no cast inside a handler's reach.
        Store store = stores.findById(storeId);
        String owner = store == null ? null : store.getOwnerUsername();
        try {
            if (owner == null) {
                throw new ResponseStatusException(404, "No such store");
            }
            if (EXPORT_PRODUCTS.equals(kind)) {
                exportProducts(owner, storeId, out, lines);
            } else if (EXPORT_ORDERS.equals(kind)) {
                exportOrders(owner, storeId, out, lines);
            } else {
                importProducts(owner, storeId, Rows.text(r.get("input")), out, errors, lines);
            }
        } catch (IOException | RuntimeException failed) {
            status = FAILED;
            String reason = failed instanceof ResponseStatusException
                    ? ((ResponseStatusException) failed).getReason() : "The operation failed";
            errors.add(reason == null ? "The operation failed" : reason);
        }
        StringBuilder err = new StringBuilder();
        for (int iter = 0; iter < errors.size() && iter < MAX_ERRORS; iter++) {
            err.append(errors.get(iter)).append('\n');
        }
        db.execute("UPDATE bulk_job SET status = ?, output = ?, errors = ?, line_count = ?, "
                + "error_count = ?, input = '', finished_at = ? WHERE id = ?", new Object[] {status,
                    out.toString(), err.toString(), Integer.valueOf(lines[0]),
                    Integer.valueOf(Math.max(lines[1], FAILED.equals(status) ? 1 : 0)),
                    Long.valueOf(System.currentTimeMillis()), id});
    }

    private void exportProducts(String owner, String storeId, StringBuilder out, int[] lines)
            throws IOException {
        List rows = db.query("SELECT id AS id FROM product WHERE store_id = ? ORDER BY created_at",
                new Object[] {storeId});
        for (int iter = 0; rows != null && iter < rows.size() && lines[0] < MAX_LINES; iter++) {
            String productId = Rows.text(((Map) rows.get(iter)).get("id"));
            out.append(Json.write(catalog.merchantProduct(owner, storeId, productId))).append('\n');
            lines[0]++;
        }
    }

    private void exportOrders(String owner, String storeId, StringBuilder out, int[] lines)
            throws IOException {
        List rows = db.query("SELECT id AS id FROM store_order WHERE store_id = ? AND status <> ? "
                + "ORDER BY created_at", new Object[] {storeId, Orders.DRAFT});
        for (int iter = 0; rows != null && iter < rows.size() && lines[0] < MAX_LINES; iter++) {
            String orderId = Rows.text(((Map) rows.get(iter)).get("id"));
            out.append(Json.write(orders.detail(owner, storeId, orderId))).append('\n');
            lines[0]++;
        }
    }

    private void importProducts(String owner, String storeId, String input, StringBuilder out,
            List<String> errors, int[] lines) throws IOException {
        int start = 0;
        int number = 0;
        while (start < input.length()) {
            int nl = input.indexOf('\n', start);
            int end = nl < 0 ? input.length() : nl;
            String line = input.substring(start, end).trim();
            start = end + 1;
            number++;
            if (line.length() == 0) {
                continue;
            }
            if (number > MAX_LINES) {
                errors.add("line " + number + ": past the limit of " + MAX_LINES + " lines");
                lines[1]++;
                break;
            }
            try {
                ProductDto p = product(line);
                if (p == null) {
                    throw new ResponseStatusException(400, "not a product");
                }
                ProductDto saved;
                if (p.id() != null && p.id().length() > 0) {
                    Map mine = db.queryOne("SELECT id AS id FROM product WHERE id = ? AND store_id = ?",
                            new Object[] {p.id(), storeId});
                    if (mine == null) {
                        throw new ResponseStatusException(404, "no such product in this store");
                    }
                    saved = catalog.update(owner, storeId, p.id(), p);
                } else {
                    saved = catalog.create(owner, storeId, p);
                }
                out.append("{\"line\":").append(number).append(",\"id\":")
                        .append(Json.write(saved.id())).append("}\n");
                lines[0]++;
            } catch (ResponseStatusException refused) {
                errors.add("line " + number + ": " + refused.getReason());
                lines[1]++;
            } catch (IOException | RuntimeException malformed) {
                errors.add("line " + number + ": not a product as JSON");
                lines[1]++;
            }
        }
    }

    /// One import line as a product. Its own method, so the cast `Json.read`
    /// implies is not inside the caller's try.
    private static ProductDto product(String line) throws IOException {
        return Json.read(line, ProductDto.class);
    }

    // ------------------------------------------------------------- reading

    private List<BulkJobDto> jobs(String storeId) throws IOException {
        List rows = db.query("SELECT id AS id FROM bulk_job WHERE store_id = ? "
                + "ORDER BY created_at DESC", new Object[] {storeId});
        List<BulkJobDto> out = new ArrayList<BulkJobDto>();
        for (int iter = 0; rows != null && iter < rows.size() && iter < 50; iter++) {
            out.add(dto(row(storeId, Rows.text(((Map) rows.get(iter)).get("id")))));
        }
        return out;
    }

    private Map row(String storeId, String jobId) throws IOException {
        Map r = jobId == null ? null : db.queryOne("SELECT id AS id, store_id AS store_id, "
                + "kind AS kind, status AS status, errors AS errors, output AS output, "
                + "line_count AS line_count, error_count AS error_count, created_at AS created_at, "
                + "finished_at AS finished_at FROM bulk_job WHERE id = ?", new Object[] {jobId});
        StoreAccess.requireIn(r == null ? null : Rows.text(r.get("store_id")), storeId,
                "bulk operation");
        return r;
    }

    private static BulkJobDto dto(Map r) {
        List<String> errors = new ArrayList<String>();
        String all = Rows.text(r.get("errors"));
        int start = 0;
        while (start < all.length()) {
            int nl = all.indexOf('\n', start);
            int end = nl < 0 ? all.length() : nl;
            if (end > start) {
                errors.add(all.substring(start, end));
            }
            start = end + 1;
        }
        return new BulkJobDto(Rows.text(r.get("id")), Rows.text(r.get("kind")),
                Rows.text(r.get("status")), "", (int) Rows.number(r.get("line_count")),
                (int) Rows.number(r.get("error_count")), errors,
                Rows.number(r.get("created_at")),
                DONE.equals(Rows.text(r.get("status"))) || FAILED.equals(Rows.text(r.get("status")))
                        ? Rows.number(r.get("finished_at")) : 0);
    }
}
