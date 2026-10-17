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
package com.codenameone.examples.savor.api;

import java.util.List;

/// A restaurant with its menu, as its page shows it.
///
/// @param opensAt Minutes after local midnight.
/// @param status `pending`, `approved`, `rejected` or `suspended`.
/// @param reason Why an admin rejected or suspended it.
public record RestaurantDto(
        String id,
        String name,
        String cuisine,
        String description,
        String address,
        double lat,
        double lng,
        String photoId,
        double rating,
        long ratingCount,
        int priceLevel,
        int etaMinutes,
        long deliveryFeeCents,
        long minimumCents,
        boolean open,
        boolean paused,
        int opensAt,
        int closesAt,
        int prepMinutes,
        boolean favorite,
        String status,
        String reason,
        List<MenuSectionDto> menu) {
}
