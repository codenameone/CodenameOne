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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// One buyable combination of a product's options.
///
/// @param available Across every location; `stock` per location.
/// @param digital Delivered as a download: needs no shipping.
/// @param hsCode Harmonized System code for customs, 6 to 10 digits; empty to use the product's.
/// @param originCountry ISO 3166 country of origin for customs, or empty.
/// @param unitQuantity The measure sold for unit pricing, in thousandths of `unitName`; 0 for none.
/// @param unitReference The quantity of `unitReferenceName` a unit price is shown per.
@Mapped
public record VariantDto(
        String id,
        String productId,
        String title,
        String option1,
        String option2,
        String option3,
        String sku,
        String barcode,
        long priceCents,
        long compareAtCents,
        long costCents,
        int weightGrams,
        boolean taxable,
        boolean trackInventory,
        boolean allowBackorder,
        int position,
        int available,
        List<StockDto> stock,
        boolean digital,
        String hsCode,
        String originCountry,
        long unitQuantity,
        String unitName,
        int unitReference,
        String unitReferenceName) {
}
