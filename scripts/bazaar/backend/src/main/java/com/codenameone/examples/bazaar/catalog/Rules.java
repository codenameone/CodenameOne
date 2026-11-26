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
package com.codenameone.examples.bazaar.catalog;

import com.codename1.backend.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/// The rules of a smart collection: terms joined by ` AND `, each one of
/// `tag:x`, `type:x`, `vendor:x`, `price<N` and `price>N` (N in cents).
/// Names compare ignoring ASCII case. `price<N` holds when the cheapest
/// variant costs less than N, `price>N` when the dearest costs more.
public final class Rules {
    private final List<String[]> terms;

    private Rules(List<String[]> terms) {
        this.terms = terms;
    }

    /// Parses rule text; answers 400 naming the first term it cannot read.
    public static Rules parse(String text) {
        List<String[]> terms = new ArrayList<String[]>();
        String rest = text == null ? "" : text.trim();
        while (rest.length() > 0) {
            int and = indexOfAnd(rest);
            String term = (and < 0 ? rest : rest.substring(0, and)).trim();
            rest = and < 0 ? "" : rest.substring(and + 5).trim();
            terms.add(term(term));
        }
        return new Rules(terms);
    }

    /// Whether rule text parses, without answering 400.
    public static boolean valid(String text) {
        try {
            parse(text);
            return true;
        } catch (ResponseStatusException bad) {
            return false;
        }
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    /// Whether a product is in the collection.
    ///
    /// @param tags the product's tags
    /// @param minPrice its cheapest variant, in cents
    /// @param maxPrice its dearest variant, in cents
    public boolean matches(List<String> tags, String type, String vendor, long minPrice,
            long maxPrice) {
        if (terms.isEmpty()) {
            return false;
        }
        for (int iter = 0; iter < terms.size(); iter++) {
            String[] term = terms.get(iter);
            String kind = term[0];
            String value = term[1];
            boolean ok;
            if ("tag".equals(kind)) {
                ok = false;
                for (int t = 0; t < tags.size() && !ok; t++) {
                    ok = tags.get(t).equalsIgnoreCase(value);
                }
            } else if ("type".equals(kind)) {
                ok = type != null && type.equalsIgnoreCase(value);
            } else if ("vendor".equals(kind)) {
                ok = vendor != null && vendor.equalsIgnoreCase(value);
            } else if ("price<".equals(kind)) {
                ok = minPrice < Long.parseLong(value);
            } else {
                ok = maxPrice > Long.parseLong(value);
            }
            if (!ok) {
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

    private static String[] term(String term) {
        if (term.regionMatches(true, 0, "price<", 0, 6)
                || term.regionMatches(true, 0, "price>", 0, 6)) {
            String number = term.substring(6).trim();
            if (number.length() == 0 || number.length() > 15) {
                throw bad(term);
            }
            for (int iter = 0; iter < number.length(); iter++) {
                if (number.charAt(iter) < '0' || number.charAt(iter) > '9') {
                    throw bad(term);
                }
            }
            return new String[] {term.charAt(5) == '<' ? "price<" : "price>", number};
        }
        String[] kinds = {"tag", "type", "vendor"};
        for (int iter = 0; iter < kinds.length; iter++) {
            String prefix = kinds[iter] + ":";
            if (term.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String value = term.substring(prefix.length()).trim();
                if (value.length() == 0) {
                    throw bad(term);
                }
                return new String[] {kinds[iter], value};
            }
        }
        throw bad(term);
    }

    private static ResponseStatusException bad(String term) {
        return new ResponseStatusException(400, "A rule cannot read \"" + term
                + "\"; use tag:x, type:x, vendor:x, price<N or price>N joined by AND");
    }
}
