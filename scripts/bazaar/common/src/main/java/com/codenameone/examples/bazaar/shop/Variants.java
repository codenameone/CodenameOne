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
package com.codenameone.examples.bazaar.shop;

import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import java.util.List;

/// Which variant the chips on a product page pick out.
///
/// A product has up to three options (size, colour, material) and a variant
/// per combination, carrying its values in `option1..option3` in the order of
/// the product's options. The page keeps one chosen value per option; this
/// finds the variant they name, and says which other values could still be
/// picked without landing on a combination that does not exist or is sold
/// out. Pure, and tested on its own.
public final class Variants {
    private Variants() {
    }

    /// The value of option `position` (0 to 2) on `variant`.
    public static String value(VariantDto variant, int position) {
        switch (position) {
            case 0:
                return variant.option1();
            case 1:
                return variant.option2();
            case 2:
                return variant.option3();
            default:
                return null;
        }
    }

    /// Whether a unit of `variant` can be bought: in stock, or not counted,
    /// or sold on backorder.
    public static boolean buyable(VariantDto variant) {
        return variant != null && (!variant.trackInventory() || variant.allowBackorder()
                || variant.available() > 0);
    }

    /// The variant every chosen value matches; null while a choice is
    /// missing or the combination does not exist. A null entry of `chosen`
    /// matches anything only when the product has no such option.
    public static VariantDto resolve(List<OptionDto> options, List<VariantDto> variants,
            String[] chosen) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        int count = options == null ? 0 : Math.min(3, options.size());
        if (count == 0) {
            return variants.get(0);
        }
        for (VariantDto variant : variants) {
            if (matches(variant, chosen, count, -1)) {
                return variant;
            }
        }
        return null;
    }

    /// Whether picking `value` for option `position`, keeping the other
    /// choices, names a variant that exists and can be bought.
    public static boolean possible(List<OptionDto> options, List<VariantDto> variants,
            String[] chosen, int position, String value) {
        int count = options == null ? 0 : Math.min(3, options.size());
        if (variants == null) {
            return false;
        }
        for (VariantDto variant : variants) {
            if (same(value(variant, position), value) && matches(variant, chosen, count, position)
                    && buyable(variant)) {
                return true;
            }
        }
        return false;
    }

    /// The choices a page opens with: those of the first variant that can be
    /// bought, else of the first variant.
    public static String[] initial(List<OptionDto> options, List<VariantDto> variants) {
        String[] chosen = new String[3];
        if (variants == null || variants.isEmpty()) {
            return chosen;
        }
        VariantDto pick = variants.get(0);
        for (VariantDto variant : variants) {
            if (buyable(variant)) {
                pick = variant;
                break;
            }
        }
        int count = options == null ? 0 : Math.min(3, options.size());
        for (int iter = 0; iter < count; iter++) {
            chosen[iter] = value(pick, iter);
        }
        return chosen;
    }

    /// The location with the most units of `variant`, or null when it has no
    /// stock anywhere it is counted.
    public static StockDto bestStock(VariantDto variant) {
        if (variant == null || variant.stock() == null) {
            return null;
        }
        StockDto best = null;
        for (StockDto stock : variant.stock()) {
            if (stock.available() > 0 && (best == null || stock.available() > best.available())) {
                best = stock;
            }
        }
        return best;
    }

    /// Whether few enough are left to say how many: "3 left at Downtown".
    public static boolean low(StockDto stock) {
        return stock != null && stock.available() > 0 && stock.available() <= 5;
    }

    private static boolean matches(VariantDto variant, String[] chosen, int count, int skip) {
        for (int iter = 0; iter < count; iter++) {
            if (iter == skip) {
                continue;
            }
            String want = chosen == null || iter >= chosen.length ? null : chosen[iter];
            if (want == null) {
                if (skip < 0) {
                    return false;
                }
                continue;
            }
            if (!same(value(variant, iter), want)) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
