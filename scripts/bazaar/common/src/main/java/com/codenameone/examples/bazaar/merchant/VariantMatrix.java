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

import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import java.util.ArrayList;
import java.util.List;

/// The variants a product's options make: one per combination of values.
///
/// Editing the options never throws away what the merchant typed into a
/// variant. A combination that already had a variant keeps it -- its id, SKU,
/// barcode, prices and weight -- and a new combination starts from the first
/// existing variant's prices, so adding a colour does not reset every price to
/// zero. Pure logic: no display, no network.
public final class VariantMatrix {
    /// At most three options, as on the server and in every storefront.
    public static final int MAX_OPTIONS = 3;

    /// A store does not need more variants than this on one product, and the
    /// grid that edits them stays usable.
    public static final int MAX_VARIANTS = 100;

    private VariantMatrix() {
    }

    /// The combinations of `options`' values, in order: the first option
    /// changes slowest. An option with no values is skipped. No options at
    /// all is one combination with no values, the single default variant.
    public static List<String[]> combinations(List<OptionDto> options) {
        List<String[]> out = new ArrayList<String[]>();
        out.add(new String[0]);
        int used = 0;
        for (int o = 0; options != null && o < options.size() && used < MAX_OPTIONS; o++) {
            List<String> values = clean(options.get(o).values());
            if (values.isEmpty()) {
                continue;
            }
            used++;
            List<String[]> next = new ArrayList<String[]>();
            for (int c = 0; c < out.size(); c++) {
                String[] prefix = out.get(c);
                for (int v = 0; v < values.size(); v++) {
                    String[] longer = new String[prefix.length + 1];
                    System.arraycopy(prefix, 0, longer, 0, prefix.length);
                    longer[prefix.length] = values.get(v);
                    next.add(longer);
                    if (next.size() > MAX_VARIANTS) {
                        throw new IllegalArgumentException("More than " + MAX_VARIANTS
                                + " variants");
                    }
                }
            }
            out = next;
        }
        return out;
    }

    /// The variants for `options`, reusing `existing` where a combination
    /// matches one (ignoring ASCII case and surrounding space).
    public static List<VariantDto> generate(String productId, List<OptionDto> options,
            List<VariantDto> existing) {
        List<String[]> combos = combinations(options);
        List<VariantDto> out = new ArrayList<VariantDto>();
        VariantDto template = existing == null || existing.isEmpty() ? null : existing.get(0);
        for (int iter = 0; iter < combos.size(); iter++) {
            String[] values = combos.get(iter);
            String o1 = value(values, 0);
            String o2 = value(values, 1);
            String o3 = value(values, 2);
            VariantDto match = find(existing, o1, o2, o3, combos.size() == 1);
            if (match != null) {
                out.add(new VariantDto(match.id(), productId, title(values), o1, o2, o3,
                        match.sku(), match.barcode(), match.priceCents(), match.compareAtCents(),
                        match.costCents(), match.weightGrams(), match.taxable(),
                        match.trackInventory(), match.allowBackorder(), iter, match.available(),
                        match.stock()));
            } else {
                out.add(new VariantDto(null, productId, title(values), o1, o2, o3, "", "",
                        template == null ? 0 : template.priceCents(),
                        template == null ? 0 : template.compareAtCents(),
                        template == null ? 0 : template.costCents(),
                        template == null ? 0 : template.weightGrams(),
                        template == null || template.taxable(),
                        template == null || template.trackInventory(),
                        template != null && template.allowBackorder(), iter, 0,
                        new ArrayList<StockDto>()));
            }
        }
        return out;
    }

    /// A variant's title: its values joined by " / ", or "Default" for none.
    public static String title(String[] values) {
        if (values.length == 0) {
            return "Default";
        }
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < values.length; iter++) {
            if (iter > 0) {
                b.append(" / ");
            }
            b.append(values[iter]);
        }
        return b.toString();
    }

    /// Splits a comma separated list of values typed into an option, dropping
    /// blanks and repeats (ignoring ASCII case).
    public static List<String> split(String typed) {
        List<String> out = new ArrayList<String>();
        if (typed == null) {
            return out;
        }
        int start = 0;
        while (start <= typed.length()) {
            int comma = typed.indexOf(',', start);
            int end = comma < 0 ? typed.length() : comma;
            add(out, typed.substring(start, end));
            start = end + 1;
        }
        return out;
    }

    /// The values joined back for editing.
    public static String join(List<String> values) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; values != null && iter < values.size(); iter++) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(values.get(iter));
        }
        return b.toString();
    }

    private static List<String> clean(List<String> values) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; values != null && iter < values.size(); iter++) {
            add(out, values.get(iter));
        }
        return out;
    }

    private static void add(List<String> out, String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.length() == 0) {
            return;
        }
        for (int iter = 0; iter < out.size(); iter++) {
            if (out.get(iter).equalsIgnoreCase(v)) {
                return;
            }
        }
        out.add(v);
    }

    private static String value(String[] values, int index) {
        return index < values.length ? values[index] : null;
    }

    private static VariantDto find(List<VariantDto> existing, String o1, String o2, String o3,
            boolean single) {
        for (int iter = 0; existing != null && iter < existing.size(); iter++) {
            VariantDto v = existing.get(iter);
            if (same(v.option1(), o1) && same(v.option2(), o2) && same(v.option3(), o3)) {
                return v;
            }
        }
        // A product going from no options to none keeps its one variant.
        if (single && o1 == null && existing != null && existing.size() == 1) {
            return existing.get(0);
        }
        return null;
    }

    private static boolean same(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.equalsIgnoreCase(y);
    }
}
