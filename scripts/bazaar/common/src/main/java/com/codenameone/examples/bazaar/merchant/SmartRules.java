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

import java.util.ArrayList;
import java.util.List;

/// The rules of a smart collection, as the rule builder edits them.
///
/// The server stores rules as text: terms joined by ` AND `, each one of
/// `tag:x`, `type:x`, `vendor:x`, `price<N` or `price>N` with N in cents (see
/// the backend's `catalog.Rules`). The builder shows one row per term -- a
/// field, a comparison and a value -- and this class turns rows into text and
/// back. Prices are typed in the store's currency and kept in cents. Pure logic.
public final class SmartRules {
    public static final String TAG = "tag";
    public static final String TYPE = "type";
    public static final String VENDOR = "vendor";
    public static final String PRICE_BELOW = "price<";
    public static final String PRICE_ABOVE = "price>";

    /// Every field the builder offers, in the order of its picker.
    public static final String[] FIELDS = {TAG, TYPE, VENDOR, PRICE_BELOW, PRICE_ABOVE};

    /// One row of the builder.
    public static final class Rule {
        public final String field;
        public final String value;

        public Rule(String field, String value) {
            this.field = field;
            this.value = value == null ? "" : value.trim();
        }

        /// Whether the value is a price in cents.
        public boolean isPrice() {
            return PRICE_BELOW.equals(field) || PRICE_ABOVE.equals(field);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Rule)) {
                return false;
            }
            Rule r = (Rule) o;
            return field.equals(r.field) && value.equals(r.value);
        }

        @Override
        public int hashCode() {
            return field.hashCode() * 31 + value.hashCode();
        }

        @Override
        public String toString() {
            return isPrice() ? field + value : field + ":" + value;
        }
    }

    private SmartRules() {
    }

    /// The rows of rule text; null when a term cannot be read, so the editor
    /// can say the rules were written somewhere it does not understand.
    public static List<Rule> parse(String text) {
        List<Rule> out = new ArrayList<Rule>();
        String rest = text == null ? "" : text.trim();
        while (rest.length() > 0) {
            int and = indexOfAnd(rest);
            String term = (and < 0 ? rest : rest.substring(0, and)).trim();
            rest = and < 0 ? "" : rest.substring(and + 5).trim();
            Rule rule = term(term);
            if (rule == null) {
                return null;
            }
            out.add(rule);
        }
        return out;
    }

    /// Rule text for rows, skipping rows with no value (the row the user has
    /// added and not filled in yet) and price rows that are not whole cents.
    public static String format(List<Rule> rules) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; rules != null && iter < rules.size(); iter++) {
            Rule r = rules.get(iter);
            if (r.value.length() == 0 || (r.isPrice() && !digits(r.value))) {
                continue;
            }
            if (b.length() > 0) {
                b.append(" AND ");
            }
            b.append(r.toString());
        }
        return b.toString();
    }

    /// A readable sentence for a list of collections: "Tag is sale and price
    /// under 2000".
    public static String describe(List<Rule> rules) {
        if (rules == null || rules.isEmpty()) {
            return "Manual";
        }
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < rules.size(); iter++) {
            Rule r = rules.get(iter);
            if (iter > 0) {
                b.append(", ");
            }
            b.append(label(r.field)).append(' ').append(r.value);
        }
        return b.toString();
    }

    /// What the picker shows for a field.
    public static String label(String field) {
        if (TAG.equals(field)) {
            return "Tag is";
        }
        if (TYPE.equals(field)) {
            return "Type is";
        }
        if (VENDOR.equals(field)) {
            return "Vendor is";
        }
        if (PRICE_BELOW.equals(field)) {
            return "Price below";
        }
        return "Price above";
    }

    private static Rule term(String term) {
        if (term.regionMatches(true, 0, PRICE_BELOW, 0, 6)
                || term.regionMatches(true, 0, PRICE_ABOVE, 0, 6)) {
            String number = term.substring(6).trim();
            if (number.length() == 0 || number.length() > 15 || !digits(number)) {
                return null;
            }
            return new Rule(term.charAt(5) == '<' ? PRICE_BELOW : PRICE_ABOVE, number);
        }
        String[] kinds = {TAG, TYPE, VENDOR};
        for (int iter = 0; iter < kinds.length; iter++) {
            String prefix = kinds[iter] + ":";
            if (term.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String value = term.substring(prefix.length()).trim();
                return value.length() == 0 ? null : new Rule(kinds[iter], value);
            }
        }
        return null;
    }

    private static boolean digits(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int iter = 0; iter < s.length(); iter++) {
            if (s.charAt(iter) < '0' || s.charAt(iter) > '9') {
                return false;
            }
        }
        return true;
    }

    private static int indexOfAnd(String text) {
        for (int iter = 0; iter + 5 <= text.length(); iter++) {
            if (text.regionMatches(true, iter, " AND ", 0, 5)) {
                return iter;
            }
        }
        return -1;
    }
}
