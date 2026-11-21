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
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AttributionDto;
import com.codenameone.examples.bazaar.api.CohortRowDto;
import com.codenameone.examples.bazaar.api.ReportDto;
import com.codenameone.examples.bazaar.api.ReportRowDto;
import com.codenameone.examples.bazaar.api.ReportSpecDto;
import com.codenameone.examples.bazaar.api.SavedReportDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.Money;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The report builder, saved reports, the cohort retention grid and sales
/// by where customers came from.
///
/// **The builder.** A report is up to two dimensions ([#DIMENSIONS]) and
/// any of the metrics ([#METRICS]) over a day range, optionally held to one
/// channel and one source. Every id is matched against those lists, never
/// written into SQL; the queries are fixed and filter on the store first.
/// Days are bucketed in Java, and the range is held to [#MAX_DAYS].
///
/// **Sources** are the last touch recorded on the order ([Attribution]);
/// an order with none is "direct".
@Component
public class Reports {
    public static final String[][] DIMENSIONS = {{"day", "Day"}, {"week", "Week"},
        {"month", "Month"}, {"channel", "Channel"}, {"source", "Source"}, {"medium", "Medium"},
        {"campaign", "Campaign"}, {"product", "Product"}, {"location", "Location"}};
    public static final String[][] METRICS = {{"sales", "Sales"}, {"orders", "Orders"},
        {"units", "Units"}, {"aov", "Average order"}, {"customers", "Customers"}};
    static final int MAX_DAYS = 400;
    static final int MAX_ORDERS = 20000;
    static final int MAX_ROWS = 500;
    static final int COHORT_MONTHS = 12;

    private final DataSource db;
    private final StoreAccess access;

    public Reports(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    static String label(String[][] table, String id) {
        for (int iter = 0; iter < table.length; iter++) {
            if (table[iter][0].equals(id)) {
                return table[iter][1];
            }
        }
        return null;
    }

    /// `yyyy-MM-dd` as the first millisecond of that day (UTC); -1 if not a date.
    static long millisOf(String iso) {
        if (!Days.isIso(iso)) {
            return -1;
        }
        long y = Long.parseLong(iso.substring(0, 4));
        long m = Long.parseLong(iso.substring(5, 7));
        long d = Long.parseLong(iso.substring(8, 10));
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return (era * 146097 + doe - 719468) * Days.MILLIS;
    }

    /// The month index (`year * 12 + month - 1`) of a moment.
    static int month(long millis) {
        String iso = Days.iso(millis);
        return Integer.parseInt(iso.substring(0, 4)) * 12 + Integer.parseInt(iso.substring(5, 7)) - 1;
    }

    /// A spec cleaned and checked: known ids, a range of at most [#MAX_DAYS].
    static ReportSpecDto clean(ReportSpecDto s, long now) {
        if (s == null) {
            throw new ResponseStatusException(400, "Nothing to report");
        }
        String d1 = s.dimension() == null || s.dimension().length() == 0 ? "day" : s.dimension();
        String d2 = s.dimension2() == null ? "" : s.dimension2();
        if (label(DIMENSIONS, d1) == null || d2.length() > 0 && (label(DIMENSIONS, d2) == null
                || d2.equals(d1))) {
            throw new ResponseStatusException(400, "Unknown dimension");
        }
        StringBuilder metrics = new StringBuilder();
        String m = s.metrics() == null || s.metrics().length() == 0 ? "sales" : s.metrics();
        int start = 0;
        while (start <= m.length()) {
            int comma = m.indexOf(',', start);
            String one = m.substring(start, comma < 0 ? m.length() : comma).trim();
            if (one.length() > 0) {
                if (label(METRICS, one) == null) {
                    throw new ResponseStatusException(400, "Unknown metric " + Codes.cut(one, 20));
                }
                if (metrics.indexOf(one) < 0) {
                    metrics.append(metrics.length() == 0 ? "" : ",").append(one);
                }
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        if (metrics.length() == 0) {
            metrics.append("sales");
        }
        long to = s.to() == null || s.to().length() == 0 ? Days.start(now) + Days.MILLIS
                : millisOf(s.to()) + Days.MILLIS;
        long from = s.from() == null || s.from().length() == 0 ? to - 30 * Days.MILLIS
                : millisOf(s.from());
        if (from < 0 || to < Days.MILLIS) {
            throw new ResponseStatusException(400, "Dates are written yyyy-MM-dd");
        }
        if (to <= from) {
            throw new ResponseStatusException(400, "The range ends before it starts");
        }
        if (to - from > MAX_DAYS * Days.MILLIS) {
            throw new ResponseStatusException(400, "A report covers at most " + MAX_DAYS + " days");
        }
        String name = s.name() == null ? "" : Codes.cut(s.name().trim(), 100);
        return new ReportSpecDto(name, d1, d2, metrics.toString(),
                s.channel() == null ? "" : Codes.cut(s.channel().trim(), 16),
                s.source() == null ? "" : Codes.cut(s.source().trim(), 100),
                Days.iso(from), Days.iso(to - 1));
    }

    private static final class Acc {
        final List<String> keys = new ArrayList<String>();
        final Map<String, Boolean> orders = new HashMap<String, Boolean>();
        final Map<String, Boolean> customers = new HashMap<String, Boolean>();
        long sales;
        long units;
    }

    private String key(String dim, Map o, Map attr, String product, Map<String, String> locs) {
        long at = created(o.get("created_at"));
        if ("day".equals(dim)) {
            return Days.iso(at);
        }
        if ("week".equals(dim)) {
            long day = Days.number(at);
            return Days.iso((day - Forecast.weekday(day)) * Days.MILLIS);
        }
        if ("month".equals(dim)) {
            return Days.iso(at).substring(0, 7);
        }
        if ("channel".equals(dim)) {
            return Rows.text(o.get("channel"));
        }
        if ("source".equals(dim) || "medium".equals(dim) || "campaign".equals(dim)) {
            String v = attr == null ? "" : Rows.text(attr.get("last_" + dim));
            return v.length() == 0 ? ("source".equals(dim) ? "direct" : "(none)") : v;
        }
        if ("product".equals(dim)) {
            return product;
        }
        String loc = Rows.text(o.get("location_id"));
        String name = locs.get(loc);
        return name == null ? "Online" : name;
    }

    static long created(Object v) {
        long m = Rows.millis(v);
        return m != 0 ? m : Rows.number(v);
    }

    /// Runs a report.
    @Transactional(readOnly = true)
    public ReportDto run(String username, String storeId, ReportSpecDto wanted) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        ReportSpecDto s = clean(wanted, System.currentTimeMillis());
        return report(storeId, s);
    }

    ReportDto report(String storeId, ReportSpecDto s) throws IOException {
        long from = millisOf(s.from());
        long to = millisOf(s.to()) + Days.MILLIS;
        Map currency = db.queryOne("SELECT currency FROM store WHERE id = ?", new Object[] {storeId});
        String cur = currency == null ? "USD" : Rows.text(currency.get("currency"));
        List orders = db.query("SELECT id, created_at, channel, location_id, subtotal_cents, "
                + "discount_cents, refunded_cents, customer_id, email FROM store_order WHERE store_id = ? "
                + "AND created_at >= ? AND created_at < ? AND status <> 'CANCELLED' AND status <> 'DRAFT' "
                + "ORDER BY created_at", new Object[] {storeId, Long.valueOf(from), Long.valueOf(to)});
        Map<String, Map> attrs = new HashMap<String, Map>();
        List a = db.query("SELECT a.subject_id, a.last_source, a.last_medium, a.last_campaign FROM "
                + "attribution a JOIN store_order o ON o.id = a.subject_id WHERE a.store_id = ? AND "
                + "a.subject_kind = 'ORDER' AND o.created_at >= ? AND o.created_at < ?",
                new Object[] {storeId, Long.valueOf(from), Long.valueOf(to)});
        for (int iter = 0; iter < a.size(); iter++) {
            Map r = (Map) a.get(iter);
            attrs.put(Rows.text(r.get("subject_id")), r);
        }
        Map<String, String> locs = new HashMap<String, String>();
        List l = db.query("SELECT id, name FROM location WHERE store_id = ?", new Object[] {storeId});
        for (int iter = 0; iter < l.size(); iter++) {
            Map r = (Map) l.get(iter);
            locs.put(Rows.text(r.get("id")), Rows.text(r.get("name")));
        }
        boolean byLine = "product".equals(s.dimension()) || "product".equals(s.dimension2())
                || s.metrics().indexOf("units") >= 0;
        Map<String, List<Map>> lines = new HashMap<String, List<Map>>();
        if (byLine) {
            List ls = db.query("SELECT l.order_id, l.title, l.quantity, l.price_cents, l.discount_cents "
                    + "FROM order_line l JOIN store_order o ON o.id = l.order_id WHERE o.store_id = ? "
                    + "AND o.created_at >= ? AND o.created_at < ?",
                    new Object[] {storeId, Long.valueOf(from), Long.valueOf(to)});
            for (int iter = 0; iter < ls.size(); iter++) {
                Map r = (Map) ls.get(iter);
                String id = Rows.text(r.get("order_id"));
                List<Map> mine = lines.get(id);
                if (mine == null) {
                    mine = new ArrayList<Map>();
                    lines.put(id, mine);
                }
                mine.add(r);
            }
        }
        Map<String, Acc> accs = new HashMap<String, Acc>();
        List<String> order = new ArrayList<String>();
        boolean productDim = "product".equals(s.dimension()) || "product".equals(s.dimension2());
        for (int iter = 0; iter < orders.size() && iter < MAX_ORDERS; iter++) {
            Map o = (Map) orders.get(iter);
            String id = Rows.text(o.get("id"));
            Map attr = attrs.get(id);
            if (s.channel().length() > 0 && !s.channel().equals(Rows.text(o.get("channel")))) {
                continue;
            }
            if (s.source().length() > 0 && !s.source().equals(key("source", o, attr, "", locs))) {
                continue;
            }
            String customer = Rows.text(o.get("customer_id"));
            if (customer.length() == 0) {
                customer = Rows.text(o.get("email"));
            }
            List<Map> mine = lines.get(id);
            if (productDim) {
                for (int li = 0; mine != null && li < mine.size(); li++) {
                    Map line = mine.get(li);
                    long q = Rows.number(line.get("quantity"));
                    long sales = Rows.number(line.get("price_cents")) * q
                            - Rows.number(line.get("discount_cents"));
                    add(accs, order, s, o, attr, Rows.text(line.get("title")), locs, id, customer,
                            sales, q);
                }
            } else {
                long units = 0;
                for (int li = 0; mine != null && li < mine.size(); li++) {
                    units += Rows.number(mine.get(li).get("quantity"));
                }
                long sales = Rows.number(o.get("subtotal_cents")) - Rows.number(o.get("discount_cents"))
                        - Rows.number(o.get("refunded_cents"));
                add(accs, order, s, o, attr, "", locs, id, customer, sales, units);
            }
        }
        List<String> metrics = new ArrayList<String>();
        int start = 0;
        while (true) {
            int comma = s.metrics().indexOf(',', start);
            metrics.add(s.metrics().substring(start, comma < 0 ? s.metrics().length() : comma));
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        List<String> columns = new ArrayList<String>();
        columns.add(label(DIMENSIONS, s.dimension()));
        if (s.dimension2().length() > 0) {
            columns.add(label(DIMENSIONS, s.dimension2()));
        }
        for (int iter = 0; iter < metrics.size(); iter++) {
            columns.add(label(METRICS, metrics.get(iter)));
        }
        boolean time = "day".equals(s.dimension()) || "week".equals(s.dimension())
                || "month".equals(s.dimension());
        List<ReportRowDto> rows = new ArrayList<ReportRowDto>();
        long total = 0;
        for (int iter = 0; iter < order.size(); iter++) {
            Acc acc = accs.get(order.get(iter));
            List<String> cells = new ArrayList<String>(acc.keys);
            long first = 0;
            for (int m = 0; m < metrics.size(); m++) {
                long v = value(metrics.get(m), acc);
                if (m == 0) {
                    first = v;
                }
                String mm = metrics.get(m);
                cells.add("sales".equals(mm) || "aov".equals(mm) ? Money.format(v, cur)
                        : String.valueOf(v));
            }
            total += first;
            rows.add(new ReportRowDto(cells, first));
        }
        sort(rows, time);
        while (rows.size() > MAX_ROWS) {
            rows.remove(rows.size() - 1);
        }
        return new ReportDto(columns, rows, total);
    }

    private void add(Map<String, Acc> accs, List<String> order, ReportSpecDto s, Map o, Map attr,
            String product, Map<String, String> locs, String orderId, String customer, long sales,
            long units) {
        String k1 = key(s.dimension(), o, attr, product, locs);
        String k2 = s.dimension2().length() == 0 ? null : key(s.dimension2(), o, attr, product, locs);
        String k = k2 == null ? k1 : k1 + "\t" + k2;
        Acc acc = accs.get(k);
        if (acc == null) {
            acc = new Acc();
            acc.keys.add(k1);
            if (k2 != null) {
                acc.keys.add(k2);
            }
            accs.put(k, acc);
            order.add(k);
        }
        acc.orders.put(orderId, Boolean.TRUE);
        if (customer.length() > 0) {
            acc.customers.put(customer, Boolean.TRUE);
        }
        acc.sales += sales;
        acc.units += units;
    }

    private static long value(String metric, Acc acc) {
        if ("orders".equals(metric)) {
            return acc.orders.size();
        }
        if ("units".equals(metric)) {
            return acc.units;
        }
        if ("customers".equals(metric)) {
            return acc.customers.size();
        }
        if ("aov".equals(metric)) {
            int n = acc.orders.size();
            return n == 0 ? 0 : acc.sales / n;
        }
        return acc.sales;
    }

    /// Time reads oldest first; anything else largest first. Stable.
    private static void sort(List<ReportRowDto> rows, boolean time) {
        for (int i = 1; i < rows.size(); i++) {
            ReportRowDto r = rows.get(i);
            int j = i - 1;
            while (j >= 0 && (time ? rows.get(j).cells().get(0).compareTo(r.cells().get(0)) > 0
                    : rows.get(j).value() < r.value())) {
                rows.set(j + 1, rows.get(j));
                j--;
            }
            rows.set(j + 1, r);
        }
    }

    /// The report as CSV: a header and a row per row, fields quoted.
    public static String csv(ReportDto r) {
        StringBuilder sb = new StringBuilder();
        line(sb, r.columns());
        for (int iter = 0; iter < r.rows().size(); iter++) {
            line(sb, r.rows().get(iter).cells());
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> cells) {
        for (int iter = 0; iter < cells.size(); iter++) {
            String c = cells.get(iter) == null ? "" : cells.get(iter);
            // A leading = + - @ would be read as a formula by a spreadsheet.
            if (c.length() > 0 && "=+-@".indexOf(c.charAt(0)) >= 0) {
                c = "'" + c;
            }
            sb.append(iter == 0 ? "" : ",").append('"');
            for (int k = 0; k < c.length(); k++) {
                char ch = c.charAt(k);
                sb.append(ch == '"' ? "\"\"" : String.valueOf(ch));
            }
            sb.append('"');
        }
        sb.append("\r\n");
    }

    // ---------------------------------------------------------------- saved

    static String encode(ReportSpecDto s) {
        return "dimension=" + s.dimension() + "\ndimension2=" + s.dimension2() + "\nmetrics="
                + s.metrics() + "\nchannel=" + flat(s.channel()) + "\nsource=" + flat(s.source())
                + "\nfrom=" + s.from() + "\nto=" + s.to();
    }

    private static String flat(String v) {
        return v.replace('\n', ' ').replace('\r', ' ');
    }

    static ReportSpecDto decode(String name, String spec) {
        Map<String, String> m = new HashMap<String, String>();
        int start = 0;
        while (start < spec.length()) {
            int nl = spec.indexOf('\n', start);
            String line = spec.substring(start, nl < 0 ? spec.length() : nl);
            int eq = line.indexOf('=');
            if (eq > 0) {
                m.put(line.substring(0, eq), line.substring(eq + 1));
            }
            start = nl < 0 ? spec.length() : nl + 1;
        }
        return new ReportSpecDto(name, get(m, "dimension"), get(m, "dimension2"), get(m, "metrics"),
                get(m, "channel"), get(m, "source"), get(m, "from"), get(m, "to"));
    }

    private static String get(Map<String, String> m, String k) {
        String v = m.get(k);
        return v == null ? "" : v;
    }

    @Transactional(readOnly = true)
    public List<SavedReportDto> saved(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        List rows = db.query("SELECT id, name, spec, created_at FROM saved_report WHERE store_id = ? "
                + "ORDER BY name", new Object[] {storeId});
        List<SavedReportDto> out = new ArrayList<SavedReportDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new SavedReportDto(Rows.text(r.get("id")), Rows.text(r.get("name")),
                    decode(Rows.text(r.get("name")), Rows.text(r.get("spec"))),
                    Rows.number(r.get("created_at"))));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public SavedReportDto save(String username, String storeId, ReportSpecDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, true, StoreAccess.MANAGERS);
        ReportSpecDto s = clean(wanted, System.currentTimeMillis());
        if (s.name().length() == 0) {
            throw new ResponseStatusException(400, "A saved report has a name");
        }
        String id = Ids.next();
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO saved_report (id, store_id, name, spec, created_by, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)", new Object[] {id, storeId, s.name(), encode(s),
                    Codes.cut(username, 200), Long.valueOf(now)});
        return new SavedReportDto(id, s.name(), s, now);
    }

    /// A saved report's spec, 404 unless it is the store's.
    @Transactional(readOnly = true)
    public ReportSpecDto spec(String username, String storeId, String reportId) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        Map r = reportId == null || reportId.length() > 32 ? null : db.queryOne(
                "SELECT store_id, name, spec FROM saved_report WHERE id = ?", new Object[] {reportId});
        StoreAccess.requireIn(r == null ? null : Rows.text(r.get("store_id")), storeId, "report");
        return decode(Rows.text(r.get("name")), Rows.text(r.get("spec")));
    }

    @Transactional(readOnly = true)
    public ReportDto runSaved(String username, String storeId, String reportId) throws IOException {
        return report(storeId, spec(username, storeId, reportId));
    }

    // -------------------------------------------------------------- cohorts

    @Transactional(readOnly = true)
    public List<CohortRowDto> cohorts(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        long now = System.currentTimeMillis();
        int nowMonth = month(now);
        // Every order of the last two years: older cohorts are not shown.
        long since = now - 2 * 366 * Days.MILLIS;
        List rows = db.query("SELECT customer_id, email, created_at FROM store_order WHERE store_id = ? "
                + "AND created_at >= ? AND status <> 'CANCELLED' AND status <> 'DRAFT'",
                new Object[] {storeId, Long.valueOf(since)});
        int n = Math.min(rows.size(), MAX_ORDERS * 5);
        String[] customers = new String[n];
        int[] months = new int[n];
        for (int iter = 0; iter < n; iter++) {
            Map r = (Map) rows.get(iter);
            String c = Rows.text(r.get("customer_id"));
            customers[iter] = c.length() > 0 ? c : Rows.text(r.get("email"));
            months[iter] = month(created(r.get("created_at")));
        }
        List<Cohorts.Row> grid = Cohorts.grid(customers, months, nowMonth, COHORT_MONTHS);
        List<CohortRowDto> out = new ArrayList<CohortRowDto>();
        for (int iter = Math.max(0, grid.size() - COHORT_MONTHS - 1); iter < grid.size(); iter++) {
            Cohorts.Row r = grid.get(iter);
            List<Integer> bp = new ArrayList<Integer>();
            for (int k = 0; k < r.retainedBp.length; k++) {
                bp.add(Integer.valueOf(r.retainedBp[k]));
            }
            out.add(new CohortRowDto(Cohorts.label(r.month), r.size, bp));
        }
        return out;
    }

    // ---------------------------------------------------------- attribution

    /// Orders and sales by first touch and by last touch, largest first.
    @Transactional(readOnly = true)
    public List<AttributionDto> attribution(String username, String storeId, String fromIso,
            String toIso) throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        ReportSpecDto s = clean(new ReportSpecDto("", "source", "", "sales", "", "", fromIso, toIso),
                System.currentTimeMillis());
        long from = millisOf(s.from());
        long to = millisOf(s.to()) + Days.MILLIS;
        List rows = db.query("SELECT o.id, o.subtotal_cents, o.discount_cents, o.refunded_cents, "
                + "a.first_source, a.first_medium, a.last_source, a.last_medium FROM store_order o "
                + "LEFT JOIN attribution a ON a.subject_id = o.id AND a.subject_kind = 'ORDER' "
                + "WHERE o.store_id = ? AND o.created_at >= ? AND o.created_at < ? AND o.status <> "
                + "'CANCELLED' AND o.status <> 'DRAFT'",
                new Object[] {storeId, Long.valueOf(from), Long.valueOf(to)});
        Map<String, long[]> sums = new HashMap<String, long[]>();
        List<String> keys = new ArrayList<String>();
        for (int iter = 0; iter < rows.size() && iter < MAX_ORDERS; iter++) {
            Map r = (Map) rows.get(iter);
            long sales = Rows.number(r.get("subtotal_cents")) - Rows.number(r.get("discount_cents"))
                    - Rows.number(r.get("refunded_cents"));
            String[] touches = {"FIRST", "LAST"};
            for (int t = 0; t < 2; t++) {
                String pre = t == 0 ? "first_" : "last_";
                String src = Rows.text(r.get(pre + "source"));
                String med = Rows.text(r.get(pre + "medium"));
                String k = touches[t] + "\t" + (src.length() == 0 ? "direct" : src) + "\t"
                        + (med.length() == 0 ? "(none)" : med);
                long[] v = sums.get(k);
                if (v == null) {
                    v = new long[2];
                    sums.put(k, v);
                    keys.add(k);
                }
                v[0]++;
                v[1] += sales;
            }
        }
        List<AttributionDto> out = new ArrayList<AttributionDto>();
        for (int iter = 0; iter < keys.size(); iter++) {
            String k = keys.get(iter);
            int a = k.indexOf('\t');
            int b = k.indexOf('\t', a + 1);
            long[] v = sums.get(k);
            AttributionDto d = new AttributionDto(k.substring(0, a), k.substring(a + 1, b),
                    k.substring(b + 1), (int) v[0], v[1]);
            int j = out.size();
            while (j > 0 && out.get(j - 1).touch().equals(d.touch())
                    && out.get(j - 1).salesCents() < d.salesCents()) {
                j--;
            }
            out.add(j, d);
        }
        return out;
    }
}
