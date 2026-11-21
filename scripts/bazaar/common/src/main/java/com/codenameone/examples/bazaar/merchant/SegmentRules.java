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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.io.JSONParser;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A segment's rules as the query builder edits them: rows of
/// `field op value` joined by AND or OR.
///
/// The server's grammar (`AudienceSql`) is a tree, `{"op":"AND"|"OR",
/// "rules":[...]}`, whose rules are conditions `{"field","op","value"}` or
/// further groups, and it is typed: spend and order counts are JSON numbers,
/// day windows numbers, `acceptsMarketing` a JSON boolean, everything else a
/// string. The builder edits one level of rows; a nested group it loads (from
/// the web back office, say) is kept as it came and written back unchanged,
/// never flattened into the rows.
final class SegmentRules {
    static final String AND = "AND";
    static final String OR = "OR";

    /// Value kinds, each with its operators.
    static final String NUMBER = "NUMBER";
    static final String DAYS = "DAYS";
    static final String TAG = "TAG";
    static final String COUNTRY = "COUNTRY";
    static final String ID = "ID";
    static final String BOOL = "BOOL";

    /// The fields a row can test, in the order the builder offers them.
    static final String[] FIELDS = {"totalSpent", "ordersCount", "lastOrderAt", "createdAt",
        "tags", "country", "tier", "club", "productPurchased", "acceptsMarketing"};

    private static final String[] NUMBER_OPS = {"=", "!=", ">", ">=", "<", "<="};
    private static final String[] DAYS_OPS = {"withinDays", "notWithinDays"};
    private static final String[] TAG_OPS = {"has", "lacks"};
    private static final String[] IS_OPS = {"is", "isNot"};
    private static final String[] BOOL_OPS = {"is"};

    /// One row, or a nested group carried through untouched.
    static final class Rule {
        String field;
        String op;
        String value;
        /// A nested group as parsed, written back as it came; null for a row.
        final Object group;

        Rule(String field, String op, String value) {
            this.field = field;
            this.op = op;
            this.value = value;
            this.group = null;
        }

        Rule(Object group) {
            this.group = group;
        }

        boolean isGroup() {
            return group != null;
        }
    }

    String op = AND;
    final List<Rule> rules = new ArrayList<Rule>();

    static String kindOf(String field) {
        if ("totalSpent".equals(field) || "ordersCount".equals(field)) {
            return NUMBER;
        }
        if ("lastOrderAt".equals(field) || "createdAt".equals(field)) {
            return DAYS;
        }
        if ("tags".equals(field)) {
            return TAG;
        }
        if ("country".equals(field)) {
            return COUNTRY;
        }
        if ("acceptsMarketing".equals(field)) {
            return BOOL;
        }
        return ID;
    }

    static String[] opsFor(String field) {
        String k = kindOf(field);
        if (NUMBER.equals(k)) {
            return NUMBER_OPS;
        }
        if (DAYS.equals(k)) {
            return DAYS_OPS;
        }
        if (TAG.equals(k)) {
            return TAG_OPS;
        }
        if (BOOL.equals(k)) {
            return BOOL_OPS;
        }
        return IS_OPS;
    }

    /// A new row for `field`, with its first operator and a usable value.
    static Rule row(String field) {
        String k = kindOf(field);
        String value = BOOL.equals(k) ? "true" : NUMBER.equals(k) || DAYS.equals(k) ? "30" : "";
        return new Rule(field, opsFor(field)[0], value);
    }

    // ---------------------------------------------------------------- reading
    /// Reads stored rules; empty or unreadable text is an empty AND.
    static SegmentRules parse(String json) {
        SegmentRules out = new SegmentRules();
        if (json == null || json.trim().length() == 0) {
            return out;
        }
        Map<String, Object> tree;
        try {
            tree = new JSONParser().parseJSON(new StringReader(json));
        } catch (IOException broken) {
            return out;
        }
        if (tree == null) {
            return out;
        }
        Object op = tree.get("op");
        out.op = OR.equals(op) ? OR : AND;
        Object list = tree.get("rules");
        if (list instanceof List) {
            List<?> items = (List<?>) list;
            for (int iter = 0; iter < items.size(); iter++) {
                Object item = items.get(iter);
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> m = (Map<?, ?>) item;
                if (m.get("rules") instanceof List) {
                    out.rules.add(new Rule(m));
                } else if (m.get("field") instanceof String && m.get("op") instanceof String) {
                    out.rules.add(new Rule((String) m.get("field"), (String) m.get("op"),
                            text(m.get("value"))));
                }
            }
        }
        return out;
    }

    private static String text(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString((long) d);
            }
        }
        return v.toString();
    }

    // ---------------------------------------------------------------- writing
    /// The rules as the server reads them, each value in its JSON type.
    String toJson() {
        StringBuilder b = new StringBuilder();
        b.append("{\"op\":\"").append(OR.equals(op) ? OR : AND).append("\",\"rules\":[");
        for (int iter = 0; iter < rules.size(); iter++) {
            if (iter > 0) {
                b.append(',');
            }
            Rule r = rules.get(iter);
            if (r.isGroup()) {
                value(b, r.group);
                continue;
            }
            b.append("{\"field\":");
            string(b, r.field);
            b.append(",\"op\":");
            string(b, r.op);
            b.append(",\"value\":");
            String k = kindOf(r.field);
            String v = r.value == null ? "" : r.value.trim();
            if ((NUMBER.equals(k) || DAYS.equals(k)) && whole(v)) {
                b.append(v);
            } else if (BOOL.equals(k)) {
                b.append("false".equalsIgnoreCase(v) ? "false" : "true");
            } else {
                string(b, v);
            }
            b.append('}');
        }
        b.append("]}");
        return b.toString();
    }

    /// What is wrong with the rows, in words, or null: the server answers 400
    /// for the same things, so the builder says so before asking.
    String problem() {
        for (int iter = 0; iter < rules.size(); iter++) {
            Rule r = rules.get(iter);
            if (r.isGroup()) {
                continue;
            }
            String k = kindOf(r.field);
            String v = r.value == null ? "" : r.value.trim();
            if ((NUMBER.equals(k) || DAYS.equals(k)) && !whole(v)) {
                return "Rule " + (iter + 1) + " needs a whole number";
            }
            if (DAYS.equals(k) && Long.parseLong(v) > 3650) {
                return "Rule " + (iter + 1) + " looks back at most 3650 days";
            }
            if (COUNTRY.equals(k) && !letters(v, 2)) {
                return "Rule " + (iter + 1) + " needs a two-letter country";
            }
            if ((TAG.equals(k) || ID.equals(k)) && v.length() == 0) {
                return "Rule " + (iter + 1) + " needs a value";
            }
        }
        return null;
    }

    static boolean whole(String v) {
        if (v == null || v.length() == 0 || v.length() > 15) {
            return false;
        }
        for (int iter = 0; iter < v.length(); iter++) {
            char c = v.charAt(iter);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean letters(String v, int n) {
        if (v.length() != n) {
            return false;
        }
        for (int iter = 0; iter < n; iter++) {
            char c = v.charAt(iter);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')) {
                return false;
            }
        }
        return true;
    }

    private static void value(StringBuilder b, Object v) {
        if (v instanceof Map) {
            b.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!first) {
                    b.append(',');
                }
                first = false;
                string(b, String.valueOf(e.getKey()));
                b.append(':');
                value(b, e.getValue());
            }
            b.append('}');
        } else if (v instanceof List) {
            b.append('[');
            List<?> l = (List<?>) v;
            for (int iter = 0; iter < l.size(); iter++) {
                if (iter > 0) {
                    b.append(',');
                }
                value(b, l.get(iter));
            }
            b.append(']');
        } else if (v instanceof Number) {
            b.append(text(v));
        } else if (v instanceof Boolean) {
            b.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v == null) {
            b.append("null");
        } else {
            string(b, v.toString());
        }
    }

    static void string(StringBuilder b, String s) {
        b.append('"');
        for (int iter = 0; s != null && iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else if (c == '\t') {
                b.append("\\t");
            } else if (c < 0x20) {
                String hex = Integer.toHexString(c);
                b.append("\\u");
                for (int pad = hex.length(); pad < 4; pad++) {
                    b.append('0');
                }
                b.append(hex);
            } else {
                b.append(c);
            }
        }
        b.append('"');
    }
}
