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

/// An app's page in the app directory: what its developer submitted and
/// what the platform decided about it.
///
/// @param handle The listing's address in the directory, `/apps/<handle>`.
/// @param clientId The app's OAuth client id.
/// @param category One of `AppStoreText.CATEGORIES`.
/// @param screenshots Addresses of screenshots, `https` only.
/// @param pricing The developer's own words for what the app costs.
/// @param installUrl Where installing starts: the app's own address with the
///        store added, which sends the owner on to the consent page.
/// @param status `PENDING`, `APPROVED`, `REJECTED` or `DELISTED`; only an
///        approved listing is public.
/// @param reason Why it was rejected or delisted, for its developer.
/// @param ratingTenths The average of the visible reviews, times ten (45 is 4.5).
/// @param reviews How many visible reviews that average is over.
@Mapped
public record AppListingDto(
        String id,
        String appId,
        String handle,
        String clientId,
        String name,
        String tagline,
        String description,
        String category,
        String iconUrl,
        List<String> screenshots,
        String pricing,
        String supportUrl,
        String privacyUrl,
        String installUrl,
        String developer,
        String status,
        String reason,
        int ratingTenths,
        int reviews,
        long submittedAt,
        long reviewedAt) {
}
