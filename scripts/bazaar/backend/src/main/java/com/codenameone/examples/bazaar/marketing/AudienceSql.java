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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Compiles a segment's rule tree to SQL over the `customer` table.
///
/// A tree is a group, `{"op":"AND"|"OR","rules":[...]}`, whose rules are
/// groups or conditions, `{"field":..., "op":..., "value":...}`. Nothing
/// the caller sends reaches the SQL text: a field picks a fragment from a
/// fixed table, an operator picks one from a fixed list for that field's
/// kind, and every value is a bound parameter, checked for its kind first.
/// Anything else -- an unknown field or operator, a value of the wrong kind,
/// a tree too deep or too big -- is a 400. The store is fixed outside the
/// tree, so no rule can reach another store's customers.
///
/// | field | operators | value |
/// |---|---|---|
/// | totalSpent, ordersCount | = != > >= < <= | whole number (cents for spend) |
/// | lastOrderAt, createdAt | withinDays notWithinDays | days, 0..3650 |
/// | tags | has lacks | a tag: letters, digits, space, - and _ |
/// | country | is isNot | two letters |
/// | tier, club, productPurchased | is isNot | an id |
/// | acceptsMarketing | is | true or false |
public final class AudienceSql {
    static final int MAX_DEPTH = 4;
    static final int MAX_CONDITIONS = 30;
    static final long DAY = 24L * 3600 * 1000;
    static final String[] NUMERIC_OPS = {"=", "!=", ">", ">=", "<", "<="};
    static final String[] SQL_OPS = {"=", "<>", ">", ">=", "<", "<="};

    /// The compiled statement: SQL with `?` placeholders and their values.
    public static final class Query {
        public final String where;
        public final List<Object> params;

        Query(String where, List<Object> params) {
            this.where = where;
            this.params = params;
        }
    }

    private final long now;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<Object>();
    private int conditions;

    private AudienceSql(long now) {
        this.now = now;
    }

    /// Compiles `rules` (JSON) for one store, with `now` as the reference
    /// for day-based conditions. An empty tree matches every customer.
    public static Query compile(String rules, String storeId, long now) {
        Map<String, Object> tree;
        if (rules == null || rules.trim().length() == 0) {
            tree = null;
        } else {
            try {
                tree = Json.parseObject(rules);
            } catch (IOException | RuntimeException bad) {
                throw new ResponseStatusException(400, "The segment rules are not valid JSON");
            }
        }
        AudienceSql c = new AudienceSql(now);
        c.sql.append("c.store_id = ?");
        c.params.add(storeId);
        boolean empty = tree != null && tree.get("rules") instanceof List && ((List<?>) tree.get("rules")).isEmpty();
        if (empty && !"AND".equals(tree.get("op")) && !"OR".equals(tree.get("op"))) {
            throw bad("A group is AND or OR");
        }
        if (tree != null && !empty) {
            c.sql.append(" AND ");
            c.node(tree, 1);
        }
        return new Query(c.sql.toString(), c.params);
    }

    /// Checks `rules` compile, answering the error message or null.
    public static String problem(String rules) {
        try {
            compile(rules, "x", 0);
            return null;
        } catch (ResponseStatusException bad) {
            return bad.getMessage();
        }
    }

    private static ResponseStatusException bad(String why) {
        return new ResponseStatusException(400, why);
    }

    private void node(Object n, int depth) {
        if (!(n instanceof Map)) {
            throw bad("A rule must be an object");
        }
        Map<?, ?> m = (Map<?, ?>) n;
        if (m.containsKey("rules")) {
            group(m, depth);
        } else {
            condition(m);
        }
    }

    private void group(Map<?, ?> m, int depth) {
        if (depth > MAX_DEPTH) {
            throw bad("Groups nest at most " + MAX_DEPTH + " deep");
        }
        Object op = m.get("op");
        String joiner;
        if ("AND".equals(op)) {
            joiner = " AND ";
        } else if ("OR".equals(op)) {
            joiner = " OR ";
        } else {
            throw bad("A group is AND or OR");
        }
        Object rules = m.get("rules");
        if (!(rules instanceof List) || ((List<?>) rules).isEmpty()) {
            throw bad("A group needs rules");
        }
        List<?> list = (List<?>) rules;
        sql.append('(');
        for (int iter = 0; iter < list.size(); iter++) {
            if (iter > 0) {
                sql.append(joiner);
            }
            node(list.get(iter), depth + 1);
        }
        sql.append(')');
    }

    private void condition(Map<?, ?> m) {
        conditions++;
        if (conditions > MAX_CONDITIONS) {
            throw bad("A segment has at most " + MAX_CONDITIONS + " conditions");
        }
        Object f = m.get("field");
        Object o = m.get("op");
        Object v = m.get("value");
        if (!(f instanceof String) || !(o instanceof String)) {
            throw bad("A condition needs a field and an operator");
        }
        String field = (String) f;
        String op = (String) o;
        if ("totalSpent".equals(field)) {
            numeric("c.spent_cents", op, v);
        } else if ("ordersCount".equals(field)) {
            numeric("c.orders_count", op, v);
        } else if ("lastOrderAt".equals(field)) {
            boolean within = within(op);
            long cutoff = cutoff(v);
            sql.append(within ? "EXISTS" : "NOT EXISTS").append(
                    " (SELECT 1 FROM store_order o WHERE o.customer_id = c.id AND o.created_at >= ?)");
            params.add(Long.valueOf(cutoff));
        } else if ("createdAt".equals(field)) {
            boolean within = within(op);
            sql.append(within ? "c.created_at >= ?" : "c.created_at < ?");
            params.add(Long.valueOf(cutoff(v)));
        } else if ("tags".equals(field)) {
            boolean has = "has".equals(op);
            if (!has && !"lacks".equals(op)) {
                throw bad("tags takes has or lacks");
            }
            String tag = tag(v);
            // Tags are kept joined by ", "; the tag's characters cannot be
            // LIKE wildcards (see tag), so each pattern means what it says.
            sql.append(has ? "" : "NOT ").append(
                    "(c.tags = ? OR c.tags LIKE ? OR c.tags LIKE ? OR c.tags LIKE ?)");
            params.add(tag);
            params.add(tag + ", %");
            params.add("%, " + tag);
            params.add("%, " + tag + ", %");
        } else if ("country".equals(field)) {
            boolean is = is(op);
            String cc = country(v);
            sql.append(is ? "" : "NOT ").append("(EXISTS (SELECT 1 FROM customer_address a "
                    + "WHERE a.customer_id = c.id AND a.country = ?) OR EXISTS (SELECT 1 FROM "
                    + "store_order o WHERE o.customer_id = c.id AND o.ship_country = ?))");
            params.add(cc);
            params.add(cc);
        } else if ("tier".equals(field)) {
            boolean is = is(op);
            sql.append(is ? "EXISTS" : "NOT EXISTS").append(" (SELECT 1 FROM loyalty_member l "
                    + "WHERE l.customer_id = c.id AND l.tier_id = ?)");
            params.add(id(v));
        } else if ("club".equals(field)) {
            boolean is = is(op);
            sql.append(is ? "EXISTS" : "NOT EXISTS").append(" (SELECT 1 FROM club_member k "
                    + "WHERE k.customer_id = c.id AND k.club_id = ? AND k.status <> 'LAPSED' "
                    + "AND k.renews_at > ?)");
            params.add(id(v));
            params.add(Long.valueOf(now));
        } else if ("productPurchased".equals(field)) {
            boolean is = is(op);
            sql.append(is ? "EXISTS" : "NOT EXISTS").append(" (SELECT 1 FROM order_line ol, "
                    + "store_order o WHERE ol.order_id = o.id AND o.customer_id = c.id "
                    + "AND ol.product_id = ?)");
            params.add(id(v));
        } else if ("acceptsMarketing".equals(field)) {
            if (!"is".equals(op) || !(v instanceof Boolean)) {
                throw bad("acceptsMarketing is true or false");
            }
            sql.append("c.accepts_marketing = ?");
            params.add(Integer.valueOf(((Boolean) v).booleanValue() ? 1 : 0));
        } else {
            throw bad("Unknown field: " + Tokens.fold(field.length() > 40 ? field.substring(0, 40) : field));
        }
    }

    private void numeric(String column, String op, Object v) {
        int which = -1;
        for (int iter = 0; iter < NUMERIC_OPS.length; iter++) {
            if (NUMERIC_OPS[iter].equals(op)) {
                which = iter;
            }
        }
        if (which < 0) {
            throw bad("Numbers compare with = != > >= < <=");
        }
        sql.append(column).append(' ').append(SQL_OPS[which]).append(" ?");
        params.add(Long.valueOf(whole(v, 0, 1000000000000L)));
    }

    private static boolean within(String op) {
        if ("withinDays".equals(op)) {
            return true;
        }
        if ("notWithinDays".equals(op)) {
            return false;
        }
        throw bad("Dates take withinDays or notWithinDays");
    }

    private static boolean is(String op) {
        if ("is".equals(op)) {
            return true;
        }
        if ("isNot".equals(op)) {
            return false;
        }
        throw bad("Use is or isNot");
    }

    private long cutoff(Object v) {
        return now - whole(v, 0, 3650) * DAY;
    }

    /// A whole number in range. JSON numbers arrive as Long or Double; a
    /// Double must have no fraction. Strings are not numbers.
    static long whole(Object v, long min, long max) {
        long n;
        if (v instanceof Long || v instanceof Integer) {
            n = ((Number) v).longValue();
        } else if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            if (d != Math.floor(d) || d > max || d < min) {
                throw bad("Use a whole number from " + min + " to " + max);
            }
            n = (long) d;
        } else {
            throw bad("Use a whole number from " + min + " to " + max);
        }
        if (n < min || n > max) {
            throw bad("Use a whole number from " + min + " to " + max);
        }
        return n;
    }

    static String tag(Object v) {
        if (!(v instanceof String)) {
            throw bad("A tag is text");
        }
        String s = ((String) v).trim();
        if (s.length() == 0 || s.length() > 40) {
            throw bad("A tag is 1 to 40 characters");
        }
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == ' ' || c == '-';
            if (!ok) {
                throw bad("A tag has letters, digits, spaces and dashes only");
            }
        }
        return s;
    }

    static String country(Object v) {
        if (!(v instanceof String) || ((String) v).length() != 2) {
            throw bad("A country is two letters");
        }
        String s = (String) v;
        StringBuilder out = new StringBuilder(2);
        for (int iter = 0; iter < 2; iter++) {
            char c = s.charAt(iter);
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 'a' + 'A');
            }
            if (c < 'A' || c > 'Z') {
                throw bad("A country is two letters");
            }
            out.append(c);
        }
        return out.toString();
    }

    static String id(Object v) {
        if (!(v instanceof String)) {
            throw bad("Choose one");
        }
        String s = (String) v;
        if (s.length() == 0 || s.length() > 64) {
            throw bad("Choose one");
        }
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-';
            if (!ok) {
                throw bad("That is not an id");
            }
        }
        return s;
    }
}
