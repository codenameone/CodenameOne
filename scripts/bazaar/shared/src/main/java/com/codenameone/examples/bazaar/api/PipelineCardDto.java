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

/// A card of the sales pipeline: a cart (`kind` CART, `number` 0) or an
/// order (`kind` ORDER).
///
/// `intent` is the purchase-intent score of a cart, 0 to 100, and -1 for an
/// order. `reasons` explain it as codes with a number, `views:7`,
/// `visits:3`, `minutes:12`, `value:12000`, `returning:2`, which the app
/// and the back office put into words in the reader's language.
@Mapped
public record PipelineCardDto(
        String id,
        String kind,
        int number,
        String customer,
        long valueCents,
        String currency,
        String column,
        long createdAt,
        long updatedAt,
        int intent,
        List<String> reasons) {
}
