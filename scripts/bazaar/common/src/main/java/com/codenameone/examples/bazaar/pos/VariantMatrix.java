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
package com.codenameone.examples.bazaar.pos;

import com.codenameone.examples.bazaar.api.PosItemDto;
import java.util.ArrayList;
import java.util.List;

/// A product's variants as a grid of options, for the variant picker.
///
/// The register's catalog names each variant by its title, the option values
/// joined with " / " ("M / Charcoal"), as the store's own admin writes it. The
/// picker needs them the other way round: one row of chips per option (every
/// size, every colour), and for each chip whether the other choices leave a
/// variant to sell. No UI here, so the tests can hold the rules.
final class VariantMatrix {
    /// What an option's chip shows next to its value.
    static final int AVAILABLE = 0;
    /// The combination exists and is out of stock.
    static final int SOLD_OUT = 1;
    /// The store sells no variant with this value and the other choices.
    static final int MISSING = 2;

    private final List<PosItemDto> variants;
    private final List<String[]> values = new ArrayList<String[]>();
    private final List<List<String>> options = new ArrayList<List<String>>();

    VariantMatrix(List<PosItemDto> variants) {
        this.variants = variants;
        int groups = 0;
        for (int iter = 0; iter < variants.size(); iter++) {
            String[] v = parts(variants.get(iter).variantTitle());
            values.add(v);
            groups = Math.max(groups, v.length);
        }
        for (int g = 0; g < groups; g++) {
            List<String> seen = new ArrayList<String>();
            for (int iter = 0; iter < values.size(); iter++) {
                String[] v = values.get(iter);
                if (g < v.length && !seen.contains(v[g])) {
                    seen.add(v[g]);
                }
            }
            options.add(seen);
        }
    }

    /// The values of `title` split on " / ", trimmed; one value for a title
    /// without the separator.
    static String[] parts(String title) {
        String t = title == null ? "" : title;
        List<String> out = new ArrayList<String>();
        int from = 0;
        while (true) {
            int at = t.indexOf(" / ", from);
            if (at < 0) {
                out.add(t.substring(from).trim());
                break;
            }
            out.add(t.substring(from, at).trim());
            from = at + 3;
        }
        return out.toArray(new String[out.size()]);
    }

    /// How many options (rows of chips) the product has.
    int groups() {
        return options.size();
    }

    /// The values of option `group`, in the order the catalog lists them.
    List<String> values(int group) {
        return options.get(group);
    }

    /// What to call option `group`: "Size" when its values read as sizes,
    /// "Colour" when they read as colours, else "Style". The register's
    /// catalog carries the values but not the option names.
    String name(int group) {
        List<String> v = options.get(group);
        if (all(v, SIZES) || numbers(v)) {
            return "Size";
        }
        if (any(v, COLOURS)) {
            return "Colour";
        }
        return groups() == 1 ? "Option" : "Style";
    }

    /// The variant `chosen` names (one value per group, null for a group not
    /// chosen yet), or null while it names none or more than one.
    PosItemDto find(String[] chosen) {
        PosItemDto found = null;
        for (int iter = 0; iter < variants.size(); iter++) {
            if (matches(values.get(iter), chosen, -1)) {
                if (found != null) {
                    return null;
                }
                found = variants.get(iter);
            }
        }
        return found;
    }

    /// The state of `value` in `group` given the other groups' choices.
    int state(int group, String value, String[] chosen) {
        boolean exists = false;
        for (int iter = 0; iter < variants.size(); iter++) {
            String[] v = values.get(iter);
            if (group < v.length && v[group].equals(value) && matches(v, chosen, group)) {
                exists = true;
                if (inStock(variants.get(iter))) {
                    return AVAILABLE;
                }
            }
        }
        return exists ? SOLD_OUT : MISSING;
    }

    /// The first choice a cashier sees: the first variant in stock, so the
    /// Add button is live straight away, or the first variant at all.
    String[] initial() {
        int pick = 0;
        for (int iter = 0; iter < variants.size(); iter++) {
            if (inStock(variants.get(iter))) {
                pick = iter;
                break;
            }
        }
        String[] v = values.get(pick);
        String[] out = new String[groups()];
        for (int g = 0; g < out.length && g < v.length; g++) {
            out[g] = v[g];
        }
        return out;
    }

    /// `chosen` with `group` set to `value`. When the new value leaves no
    /// variant with the other choices, the other groups move to the nearest
    /// variant that has it, so one tap always lands on something sellable.
    String[] choose(String[] chosen, int group, String value) {
        String[] out = new String[chosen.length];
        for (int g = 0; g < out.length; g++) {
            out[g] = chosen[g];
        }
        out[group] = value;
        if (find(out) != null) {
            return out;
        }
        int best = -1;
        int bestScore = -1;
        for (int iter = 0; iter < variants.size(); iter++) {
            String[] v = values.get(iter);
            if (group >= v.length || !v[group].equals(value)) {
                continue;
            }
            int score = inStock(variants.get(iter)) ? 100 : 0;
            for (int g = 0; g < v.length && g < out.length; g++) {
                if (v[g].equals(out[g])) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = iter;
            }
        }
        if (best >= 0) {
            String[] v = values.get(best);
            for (int g = 0; g < out.length && g < v.length; g++) {
                out[g] = v[g];
            }
        }
        return out;
    }

    static boolean inStock(PosItemDto v) {
        return !v.trackInventory() || v.available() > 0;
    }

    private static boolean matches(String[] v, String[] chosen, int skip) {
        for (int g = 0; g < chosen.length; g++) {
            if (g == skip || chosen[g] == null) {
                continue;
            }
            if (g >= v.length || !v[g].equals(chosen[g])) {
                return false;
            }
        }
        return true;
    }

    private static final String[] SIZES = {"XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "2XL",
        "3XL", "OS", "ONE SIZE", "SMALL", "MEDIUM", "LARGE", "PETITE", "REGULAR", "TALL"};
    private static final String[] COLOURS = {"BLACK", "WHITE", "GREY", "GRAY", "RED", "BLUE",
        "NAVY", "GREEN", "OLIVE", "SAGE", "YELLOW", "ORANGE", "PINK", "PURPLE", "BROWN", "TAN",
        "BEIGE", "CREAM", "IVORY", "CHARCOAL", "SAND", "STONE", "RUST", "TEAL", "INDIGO", "OAT",
        "NATURAL", "CLAY", "TERRACOTTA", "SKY", "MINT", "COCOA", "ESPRESSO", "SLATE", "BONE",
        "MUSTARD", "BURGUNDY", "MAROON", "CORAL", "LILAC", "GOLD", "SILVER", "COPPER", "WALNUT",
        "OAK", "ASH", "CHALK", "INK", "MOSS", "FOREST", "OCEAN", "HEATHER"};

    private static boolean all(List<String> values, String[] words) {
        for (int iter = 0; iter < values.size(); iter++) {
            if (!listed(values.get(iter), words)) {
                return false;
            }
        }
        return !values.isEmpty();
    }

    private static boolean any(List<String> values, String[] words) {
        for (int iter = 0; iter < values.size(); iter++) {
            String v = values.get(iter);
            // "Heather grey" is a colour as much as "Grey" is.
            int space = v.lastIndexOf(' ');
            if (listed(v, words) || space > 0 && listed(v.substring(space + 1), words)) {
                return true;
            }
        }
        return false;
    }

    private static boolean listed(String value, String[] words) {
        for (int iter = 0; iter < words.length; iter++) {
            if (words[iter].equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean numbers(List<String> values) {
        for (int iter = 0; iter < values.size(); iter++) {
            String v = values.get(iter);
            if (v.length() == 0) {
                return false;
            }
            for (int c = 0; c < v.length(); c++) {
                char ch = v.charAt(c);
                if ((ch < '0' || ch > '9') && ch != '.') {
                    return false;
                }
            }
        }
        return !values.isEmpty();
    }
}
