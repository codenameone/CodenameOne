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
package com.codenameone.examples.bazaar.web.table;

import com.codename1.backend.HttpServer;
import com.codenameone.examples.bazaar.web.NavItem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// The one data-table engine behind every grid: columns declared once, then
/// the request's `q`, `sort`, `dir` and `page` applied on the server, and the
/// result drawn as a [DataTable] or written as CSV.
///
/// ```java
/// Grid<OrderDto> g = new Grid<OrderDto>("orders")
///         .col("number", "Order", o -> "#" + o.number()).sortBy(o -> o.number())
///         .col("total", "Total", o -> Money.format(...)).num().sortBy(o -> o.totalCents());
/// DataTable t = g.table(rows, Grid.Query.of(request), "/admin/s1/orders");
/// ```
///
/// The rows given are the page's filtered set from one query (the service
/// that loads them is where store scoping and the database's own filters
/// are); search, sort and paging over that set happen here, so there is one
/// query per page and never one per row.
public final class Grid<T> {
    /// A cell's text, link or sort key from a row.
    public interface Fn<T, R> {
        R of(T row);
    }

    static final class Col<T> {
        String key;
        String label;
        Fn<T, String> text;
        Fn<T, ? extends Comparable> sort;
        Fn<T, String> href;
        Fn<T, String> tone;
        Fn<T, String> edit;
        boolean num;
        boolean pill;
        boolean csvOnly;
    }

    public static final int PAGE_SIZE = 25;

    private final String id;
    private final List<Col<T>> cols = new ArrayList<Col<T>>();
    private Fn<T, String> rowId;
    private Fn<T, String> rowHref;
    private int pageSize = PAGE_SIZE;
    private String defaultSort;
    private boolean defaultDesc;

    public Grid(String id) {
        this.id = id;
    }

    public Grid<T> col(String key, String label, Fn<T, String> text) {
        Col<T> c = new Col<T>();
        c.key = key;
        c.label = label;
        c.text = text;
        cols.add(c);
        return this;
    }

    private Col<T> lastCol() {
        return cols.get(cols.size() - 1);
    }

    public Grid<T> sortBy(Fn<T, ? extends Comparable> key) {
        lastCol().sort = key;
        return this;
    }

    public Grid<T> num() {
        lastCol().num = true;
        return this;
    }

    public Grid<T> link(Fn<T, String> href) {
        lastCol().href = href;
        return this;
    }

    public Grid<T> pill(Fn<T, String> tone) {
        lastCol().pill = true;
        lastCol().tone = tone;
        return this;
    }

    /// The last column is edited in place, posting `value` to the address given.
    public Grid<T> edit(Fn<T, String> url) {
        lastCol().edit = url;
        return this;
    }

    /// The last column is in the CSV only.
    public Grid<T> csvOnly() {
        lastCol().csvOnly = true;
        return this;
    }

    public Grid<T> rows(Fn<T, String> id, Fn<T, String> href) {
        this.rowId = id;
        this.rowHref = href;
        return this;
    }

    public Grid<T> pageSize(int size) {
        this.pageSize = size;
        return this;
    }

    public Grid<T> defaultSort(String key, boolean descending) {
        this.defaultSort = key;
        this.defaultDesc = descending;
        return this;
    }

    /// The request's table parameters.
    public static final class Query {
        public String q = "";
        public String sort = "";
        public boolean desc;
        public int page;

        public static Query of(HttpServer.Request request) {
            Query out = new Query();
            out.q = param(request, "q", 100);
            out.sort = param(request, "sort", 40);
            out.desc = "desc".equals(param(request, "dir", 4));
            String p = param(request, "page", 6);
            try {
                out.page = p.length() == 0 ? 0 : Math.max(0, Integer.parseInt(p));
            } catch (NumberFormatException bad) {
                out.page = 0;
            }
            return out;
        }

        static String param(HttpServer.Request request, String name, int max) {
            String v;
            try {
                v = request.queryParam(name);
            } catch (RuntimeException malformed) {
                v = null;
            }
            if (v == null) {
                return "";
            }
            v = v.trim();
            return v.length() > max ? v.substring(0, max) : v;
        }
    }

    /// The filtered and sorted rows, all of them (for CSV) -- `q` matched
    /// against every text column, ignoring ASCII case.
    public List<T> apply(List<T> rows, Query query) {
        List<T> out = new ArrayList<T>();
        String needle = fold(query.q);
        for (int iter = 0; iter < rows.size(); iter++) {
            T row = rows.get(iter);
            if (needle.length() == 0 || matches(row, needle)) {
                out.add(row);
            }
        }
        String sortKey = query.sort.length() > 0 ? query.sort : defaultSort;
        final boolean desc = query.sort.length() > 0 ? query.desc : defaultDesc;
        final Col<T> by = column(sortKey);
        if (by != null && by.sort != null) {
            Collections.sort(out, new Comparator<T>() {
                @Override
                @SuppressWarnings("unchecked")
                public int compare(T a, T b) {
                    Comparable x = by.sort.of(a);
                    Comparable y = by.sort.of(b);
                    int c = x == null ? (y == null ? 0 : -1) : y == null ? 1 : x.compareTo(y);
                    return desc ? -c : c;
                }
            });
        }
        return out;
    }

    private Col<T> column(String key) {
        if (key == null) {
            return null;
        }
        for (int iter = 0; iter < cols.size(); iter++) {
            if (cols.get(iter).key.equals(key)) {
                return cols.get(iter);
            }
        }
        return null;
    }

    private boolean matches(T row, String needle) {
        for (int iter = 0; iter < cols.size(); iter++) {
            String t = cols.get(iter).text.of(row);
            if (t != null && fold(t).indexOf(needle) >= 0) {
                return true;
            }
        }
        return false;
    }

    static String fold(String s) {
        return com.codenameone.examples.bazaar.web.security.Ascii.lower(s == null ? "" : s.trim());
    }

    /// The table for one page of `rows`. `keep` are the page's own filters,
    /// carried through every sort and page link.
    public DataTable table(List<T> rows, Query query, String base, DataTable.Param... keep) {
        List<T> all = apply(rows, query);
        DataTable t = new DataTable();
        t.id = id;
        t.base = base;
        t.q = query.q;
        for (int iter = 0; iter < keep.length; iter++) {
            if (keep[iter].value != null && keep[iter].value.length() > 0) {
                t.keep.add(keep[iter]);
            }
        }
        String activeSort = query.sort.length() > 0 ? query.sort : defaultSort;
        boolean activeDesc = query.sort.length() > 0 ? query.desc : defaultDesc;
        for (int iter = 0; iter < cols.size(); iter++) {
            Col<T> c = cols.get(iter);
            if (c.csvOnly) {
                continue;
            }
            DataTable.Column col = new DataTable.Column();
            col.key = c.key;
            col.label = c.label;
            col.num = c.num;
            col.sortable = c.sort != null;
            boolean on = c.key.equals(activeSort);
            col.sort = !on ? "none" : activeDesc ? "descending" : "ascending";
            col.sortUrl = url(base, t.keep, query.q, c.key, on && !activeDesc, 0);
            t.columns.add(col);
        }
        t.total = all.size();
        t.pages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        t.page = Math.min(query.page, t.pages - 1);
        int start = t.page * pageSize;
        int end = Math.min(all.size(), start + pageSize);
        t.from = all.isEmpty() ? 0 : start + 1;
        t.to = end;
        String sortParam = query.sort;
        if (t.page > 0) {
            t.prevUrl = url(base, t.keep, query.q, sortParam, query.desc, t.page - 1);
        }
        if (t.page < t.pages - 1) {
            t.nextUrl = url(base, t.keep, query.q, sortParam, query.desc, t.page + 1);
        }
        t.csvUrl = url(base, t.keep, query.q, sortParam, query.desc, 0)
                + (url(base, t.keep, query.q, sortParam, query.desc, 0).indexOf('?') < 0
                    ? "?" : "&") + "format=csv";
        for (int iter = start; iter < end; iter++) {
            T row = all.get(iter);
            DataTable.Row r = new DataTable.Row();
            r.id = rowId == null ? Integer.toString(iter) : rowId.of(row);
            r.href = rowHref == null ? "" : nz(rowHref.of(row));
            for (int c = 0; c < cols.size(); c++) {
                Col<T> col = cols.get(c);
                if (col.csvOnly) {
                    continue;
                }
                DataTable.Cell cell = new DataTable.Cell();
                cell.col = col.key;
                cell.text = nz(col.text.of(row));
                cell.num = col.num;
                cell.href = col.href == null ? "" : nz(col.href.of(row));
                cell.pill = col.pill;
                cell.tone = col.tone == null ? "" : nz(col.tone.of(row));
                if (col.edit != null) {
                    cell.edit = true;
                    cell.editUrl = nz(col.edit.of(row));
                    cell.editLabel = col.label + " for " + nz(cols.get(0).text.of(row));
                }
                r.cells.add(cell);
            }
            t.rows.add(r);
        }
        return t;
    }

    /// Bulk actions: label and the `action` value each posts.
    public static void bulk(DataTable t, String url, String... labelsAndActions) {
        t.selectable = true;
        t.bulkUrl = url;
        for (int iter = 0; iter + 1 < labelsAndActions.length; iter += 2) {
            t.actions.add(new NavItem(labelsAndActions[iter], labelsAndActions[iter + 1], ""));
        }
    }

    /// Every filtered row, sorted, as CSV: a header, then one line per row.
    /// A cell that starts like a formula is prefixed with `'`, so a
    /// spreadsheet shows it as text rather than running it.
    public byte[] csv(List<T> rows, Query query) {
        List<T> all = apply(rows, query);
        StringBuilder b = new StringBuilder();
        for (int c = 0; c < cols.size(); c++) {
            b.append(c == 0 ? "" : ",").append(csvCell(cols.get(c).label));
        }
        b.append("\r\n");
        for (int iter = 0; iter < all.size(); iter++) {
            for (int c = 0; c < cols.size(); c++) {
                b.append(c == 0 ? "" : ",").append(csvCell(cols.get(c).text.of(all.get(iter))));
            }
            b.append("\r\n");
        }
        try {
            return b.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /// The CSV response for [#csv].
    public HttpServer.Response csvResponse(List<T> rows, Query query, String fileName) {
        return new HttpServer.Response(200, "text/csv; charset=utf-8", csv(rows, query))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + ".csv\"")
                .header("Cache-Control", "private, no-store");
    }

    static String csvCell(String value) {
        String v = value == null ? "" : value;
        if (v.length() > 0) {
            char first = v.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t'
                    || first == '\r') {
                v = "'" + v;
            }
        }
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0
                || v.indexOf('\r') >= 0) {
            StringBuilder q = new StringBuilder("\"");
            for (int iter = 0; iter < v.length(); iter++) {
                char ch = v.charAt(iter);
                if (ch == '"') {
                    q.append('"');
                }
                q.append(ch);
            }
            return q.append('"').toString();
        }
        return v;
    }

    static String url(String base, List<DataTable.Param> keep, String q, String sort,
            boolean desc, int page) {
        StringBuilder b = new StringBuilder(base);
        char sep = '?';
        for (int iter = 0; iter < keep.size(); iter++) {
            b.append(sep).append(enc(keep.get(iter).name)).append('=')
                    .append(enc(keep.get(iter).value));
            sep = '&';
        }
        if (q != null && q.length() > 0) {
            b.append(sep).append("q=").append(enc(q));
            sep = '&';
        }
        if (sort != null && sort.length() > 0) {
            b.append(sep).append("sort=").append(enc(sort));
            sep = '&';
            if (desc) {
                b.append("&dir=desc");
            }
        }
        if (page > 0) {
            b.append(sep).append("page=").append(page);
        }
        return b.toString();
    }

    static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
