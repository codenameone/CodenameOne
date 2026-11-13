/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.search;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.sql.Dialect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Full-text search in the application's own SQL database, for a project that
/// does not run a search cluster.
///
/// Documents go into one table, `cn1_search_documents`, created when the engine
/// opens, with every string of a document joined into a `body` column:
///
/// - **PostgreSQL** matches with `websearch_to_tsquery('simple', ...)` over a GIN
///   index and ranks with `ts_rank`. The `simple` configuration lowercases but
///   does not stem, so "shoe" does not find "shoes"; it is the one configuration
///   that behaves the same for every language.
/// - **MySQL and MariaDB** match every word with a `FULLTEXT` index in boolean
///   mode. InnoDB ignores words shorter than `innodb_ft_min_token_size` (3) and
///   its stop words, which a query cannot override.
/// - **SQLite** scans with `LIKE`, one condition per word, and scores by how
///   often the words occur. Correct, and linear in the size of the index;
///   SQLite's `LIKE` folds case for ASCII letters only.
///
/// Matching, as on a search cluster, requires every word. Filters, field
/// restrictions, sorting, facets and paging are then applied in memory over at
/// most [#MAX_CANDIDATES] matches; past that [SearchResult#isTotalExact()] is
/// false. Highlights are not produced.
public final class SqlSearchEngine implements SearchEngine {
    /// The most matches read from the database for one query.
    public static final int MAX_CANDIDATES = 5000;
    private static final int MAX_TERMS = 16;
    private static final String TABLE = "cn1_search_documents";

    private final DataSource data;
    private final boolean ownsData;
    private final int kind;

    private static final int SQLITE = 0;
    private static final int POSTGRES = 1;
    private static final int MYSQL = 2;

    /// An engine over `data`, creating its table if needed. `ownsData` closes the
    /// data source along with the engine.
    public SqlSearchEngine(DataSource data, boolean ownsData) throws IOException {
        this.data = data;
        this.ownsData = ownsData;
        Dialect d = data.dialect();
        if (d == Dialect.POSTGRES) {
            kind = POSTGRES;
        } else if (d == Dialect.MYSQL || d == Dialect.MARIADB) {
            kind = MYSQL;
        } else {
            kind = SQLITE;
        }
        createSchema();
    }

    private void createSchema() throws IOException {
        if (kind == POSTGRES) {
            data.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (idx VARCHAR(200) NOT NULL, "
                    + "id VARCHAR(512) NOT NULL, body TEXT NOT NULL, doc TEXT NOT NULL, "
                    + "PRIMARY KEY (idx, id))", null);
            data.execute("CREATE INDEX IF NOT EXISTS " + TABLE + "_fts ON " + TABLE
                    + " USING GIN (to_tsvector('simple', body))", null);
        } else if (kind == MYSQL) {
            // utf8mb4 caps an indexed VARCHAR at 191 characters under the
            // 767-byte key limit older row formats still have.
            data.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (idx VARCHAR(191) NOT NULL, "
                    + "id VARCHAR(191) NOT NULL, body LONGTEXT NOT NULL, doc LONGTEXT NOT NULL, "
                    + "PRIMARY KEY (idx, id), FULLTEXT KEY " + TABLE + "_fts (body)) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", null);
        } else {
            data.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (idx TEXT NOT NULL, "
                    + "id TEXT NOT NULL, body TEXT NOT NULL, doc TEXT NOT NULL, "
                    + "PRIMARY KEY (idx, id))", null);
        }
    }

    private static void requireId(String id) {
        if (id == null || id.length() == 0 || id.length() > 191) {
            throw new IllegalArgumentException("A search document id is 1 to 191 characters");
        }
    }

    @Override
    public void index(String index, String id, Map document) throws IOException {
        ElasticsearchEngine.requireIndexName(index);
        requireId(id);
        StringBuilder body = new StringBuilder();
        collectText(document, body, 0);
        String sql = "INSERT INTO " + TABLE + " (idx, id, body, doc) VALUES (?, ?, ?, ?) "
                + (kind == MYSQL
                ? "ON DUPLICATE KEY UPDATE body = VALUES(body), doc = VALUES(doc)"
                : "ON CONFLICT (idx, id) DO UPDATE SET body = excluded.body, doc = excluded.doc");
        data.execute(sql, new Object[] {index, id, body.toString(), Json.write(document)});
    }

    private static void collectText(Object value, StringBuilder out, int depth) {
        if (depth > 32 || value == null) {
            return;
        }
        if (value instanceof String) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append((String) value);
        } else if (value instanceof Map) {
            Iterator it = ((Map) value).values().iterator();
            while (it.hasNext()) {
                collectText(it.next(), out, depth + 1);
            }
        } else if (value instanceof List) {
            List l = (List) value;
            for (int i = 0; i < l.size(); i++) {
                collectText(l.get(i), out, depth + 1);
            }
        }
    }

    @Override
    public boolean delete(String index, String id) throws IOException {
        ElasticsearchEngine.requireIndexName(index);
        return data.execute("DELETE FROM " + TABLE + " WHERE idx = ? AND id = ?",
                new Object[] {index, id}) > 0;
    }

    @Override
    public Map get(String index, String id) throws IOException {
        ElasticsearchEngine.requireIndexName(index);
        Map row = data.queryOne("SELECT doc FROM " + TABLE + " WHERE idx = ? AND id = ?",
                new Object[] {index, id});
        return row == null ? null : Json.parseObject(String.valueOf(row.get("doc")));
    }

    /// The words of a query: runs of letters and digits, at most [#MAX_TERMS].
    static List terms(String text) {
        List out = new ArrayList();
        int start = -1;
        for (int i = 0; i <= text.length(); i++) {
            boolean word = i < text.length() && Character.isLetterOrDigit(text.charAt(i));
            if (word && start < 0) {
                start = i;
            } else if (!word && start >= 0) {
                if (out.size() < MAX_TERMS) {
                    out.add(fold(text.substring(start, i)));
                }
                start = -1;
            }
        }
        return out;
    }

    /// Lowercases character by character. Character.toLowerCase(char) is not
    /// locale sensitive, unlike String.toLowerCase, so this folds the same on a
    /// server set to Turkish.
    static String fold(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append(Character.toLowerCase(s.charAt(i)));
        }
        return sb.toString();
    }

    private static String likeEscape(String term) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '%' || c == '_' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private List candidates(String index, List terms) throws IOException {
        int cap = MAX_CANDIDATES + 1;
        if (terms.isEmpty()) {
            return data.query("SELECT id, doc, body FROM " + TABLE + " WHERE idx = ? "
                    + "ORDER BY id LIMIT " + cap, new Object[] {index});
        }
        if (kind == POSTGRES) {
            StringBuilder q = new StringBuilder();
            for (int i = 0; i < terms.size(); i++) {
                q.append(i == 0 ? "" : " ").append('"').append(terms.get(i)).append('"');
            }
            String tsq = "websearch_to_tsquery('simple', ?)";
            return data.query("SELECT id, doc, body, ts_rank(to_tsvector('simple', body), "
                    + tsq + ") AS score FROM " + TABLE + " WHERE idx = ? AND "
                    + "to_tsvector('simple', body) @@ " + tsq
                    + " ORDER BY score DESC, id LIMIT " + cap,
                    new Object[] {q.toString(), index, q.toString()});
        }
        if (kind == MYSQL) {
            StringBuilder q = new StringBuilder();
            for (int i = 0; i < terms.size(); i++) {
                // Each word is letters and digits only (see terms), so none of the
                // boolean-mode operators can reach the server from user input.
                q.append(i == 0 ? "+" : " +").append(terms.get(i));
            }
            String match = "MATCH(body) AGAINST(? IN BOOLEAN MODE)";
            return data.query("SELECT id, doc, body, " + match + " AS score FROM " + TABLE
                    + " WHERE idx = ? AND " + match + " ORDER BY score DESC, id LIMIT " + cap,
                    new Object[] {q.toString(), index, q.toString()});
        }
        StringBuilder sql = new StringBuilder("SELECT id, doc, body FROM " + TABLE
                + " WHERE idx = ?");
        Object[] params = new Object[terms.size() + 1];
        params[0] = index;
        for (int i = 0; i < terms.size(); i++) {
            sql.append(" AND body LIKE ? ESCAPE '\\'");
            params[i + 1] = "%" + likeEscape((String) terms.get(i)) + "%";
        }
        sql.append(" ORDER BY id LIMIT ").append(cap);
        return data.query(sql.toString(), params);
    }

    @Override
    public SearchResult search(String index, SearchQuery q) throws IOException {
        ElasticsearchEngine.requireIndexName(index);
        List terms = terms(q.getText());
        List rows = candidates(index, terms);
        boolean exact = rows.size() <= MAX_CANDIDATES;
        List matched = new ArrayList();
        for (int i = 0; i < rows.size() && i < MAX_CANDIDATES; i++) {
            Map row = (Map) rows.get(i);
            Map doc = Json.parseObject(String.valueOf(row.get("doc")));
            if (!q.getFields().isEmpty() && !termsInFields(doc, q.getFields(), terms)) {
                continue;
            }
            if (!filtersHold(doc, q.getFilters())) {
                continue;
            }
            Object s = row.get("score");
            double score = s instanceof Number ? ((Number) s).doubleValue()
                    : occurrences(fold(String.valueOf(row.get("body"))), terms);
            matched.add(new SearchHit(String.valueOf(row.get("id")), score, doc, null));
        }
        if (q.getSortField() != null) {
            sortHits(matched, q.getSortField(), q.isSortAscending());
        } else if (kind == SQLITE && !terms.isEmpty()) {
            sortByScore(matched);
        }
        Map facets = new LinkedHashMap();
        for (int f = 0; f < q.getFacets().size(); f++) {
            String field = (String) q.getFacets().get(f);
            facets.put(field, facet(matched, field));
        }
        List page = new ArrayList();
        for (int i = q.getFrom(); i < matched.size() && page.size() < q.getSize(); i++) {
            page.add(matched.get(i));
        }
        return new SearchResult(page, matched.size(), exact, facets);
    }

    private static double occurrences(String body, List terms) {
        int n = 0;
        for (int i = 0; i < terms.size(); i++) {
            String t = (String) terms.get(i);
            int at = body.indexOf(t);
            while (at >= 0) {
                n++;
                at = body.indexOf(t, at + t.length());
            }
        }
        return n;
    }

    private static boolean termsInFields(Map doc, List fields, List terms) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            collectText(value(doc, (String) fields.get(i)), text, 0);
        }
        String folded = fold(text.toString());
        for (int i = 0; i < terms.size(); i++) {
            if (folded.indexOf((String) terms.get(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /// A field by name, following dots into nested maps.
    static Object value(Map doc, String field) {
        Object v = doc.get(field);
        if (v != null || field.indexOf('.') < 0) {
            return v;
        }
        Object cur = doc;
        int start = 0;
        while (cur instanceof Map) {
            int dot = field.indexOf('.', start);
            String part = dot < 0 ? field.substring(start) : field.substring(start, dot);
            cur = ((Map) cur).get(part);
            if (dot < 0) {
                return cur;
            }
            start = dot + 1;
        }
        return null;
    }

    private static boolean filtersHold(Map doc, Map filters) {
        Iterator it = filters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            Object actual = value(doc, (String) e.getKey());
            if (actual instanceof List) {
                List l = (List) actual;
                boolean any = false;
                for (int i = 0; i < l.size() && !any; i++) {
                    any = same(l.get(i), e.getValue());
                }
                if (!any) {
                    return false;
                }
            } else if (!same(actual, e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a == null ? b == null : a.equals(b);
    }

    static int compare(Object a, Object b) {
        if (a == null || b == null) {
            return a == null ? (b == null ? 0 : 1) : -1;
        }
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            return x < y ? -1 : (x > y ? 1 : 0);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private static void sortHits(List hits, final String field, final boolean ascending) {
        Collections.sort(hits, new Comparator() {
            @Override
            public int compare(Object x, Object y) {
                Object a = value(((SearchHit) x).getDocument(), field);
                Object b = value(((SearchHit) y).getDocument(), field);
                if (a == null || b == null) {
                    // Missing values last in either direction, as the clusters do.
                    return SqlSearchEngine.compare(a, b);
                }
                int c = SqlSearchEngine.compare(a, b);
                return ascending ? c : -c;
            }
        });
    }

    private static void sortByScore(List hits) {
        Collections.sort(hits, new Comparator() {
            @Override
            public int compare(Object x, Object y) {
                double a = ((SearchHit) x).getScore();
                double b = ((SearchHit) y).getScore();
                return a > b ? -1 : (a < b ? 1 : 0);
            }
        });
    }

    private static Map facet(List hits, String field) {
        final Map counts = new LinkedHashMap();
        for (int i = 0; i < hits.size(); i++) {
            Object v = value(((SearchHit) hits.get(i)).getDocument(), field);
            if (v instanceof List) {
                List l = (List) v;
                for (int j = 0; j < l.size(); j++) {
                    count(counts, l.get(j));
                }
            } else if (v != null) {
                count(counts, v);
            }
        }
        List keys = new ArrayList(counts.keySet());
        Collections.sort(keys, new Comparator() {
            @Override
            public int compare(Object x, Object y) {
                long a = ((Long) counts.get(x)).longValue();
                long b = ((Long) counts.get(y)).longValue();
                return a > b ? -1 : (a < b ? 1 : ((String) x).compareTo((String) y));
            }
        });
        Map out = new LinkedHashMap();
        for (int i = 0; i < keys.size() && i < 50; i++) {
            out.put(keys.get(i), counts.get(keys.get(i)));
        }
        return out;
    }

    private static void count(Map counts, Object v) {
        String key = v instanceof Number && ((Number) v).doubleValue()
                == Math.floor(((Number) v).doubleValue())
                ? Long.toString(((Number) v).longValue()) : String.valueOf(v);
        Long c = (Long) counts.get(key);
        counts.put(key, Long.valueOf(c == null ? 1 : c.longValue() + 1));
    }

    @Override
    public void refresh(String index) {
        // A committed row is visible to the next query; nothing to publish.
    }

    @Override
    public void deleteIndex(String index) throws IOException {
        ElasticsearchEngine.requireIndexName(index);
        data.execute("DELETE FROM " + TABLE + " WHERE idx = ?", new Object[] {index});
    }

    @Override
    public void close() {
        if (ownsData) {
            data.close();
        }
    }
}
