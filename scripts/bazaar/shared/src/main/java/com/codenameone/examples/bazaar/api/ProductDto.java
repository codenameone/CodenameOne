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

/// A product in full. Sent to create or update one: variants without `id` are added, those
/// left out are deleted; options likewise. `costCents` is only answered to staff.
///
/// @param rating Average stars times ten.
/// @param categoryId Its Google product taxonomy category, or 0.
@Mapped
public record ProductDto(
        String id,
        String storeId,
        String handle,
        String title,
        String description,
        String vendor,
        String productType,
        List<String> tags,
        String status,
        String seoTitle,
        String seoDescription,
        String photoId,
        int rating,
        int ratingCount,
        List<OptionDto> options,
        List<VariantDto> variants,
        List<ProductPhotoDto> photos,
        List<String> collectionIds,
        int categoryId) {
}
