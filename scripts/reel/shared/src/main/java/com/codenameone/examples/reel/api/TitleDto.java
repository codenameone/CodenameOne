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
package com.codenameone.examples.reel.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// A title's details page.
@Mapped
public record TitleDto(
        String id,
        String kind,
        String name,
        String synopsis,
        String posterUrl,
        String backdropUrl,
        int year,
        String rating,
        List<String> advisories,
        int maxHeight,
        List<String> audioLanguages,
        List<String> subtitleLanguages,
        String shareUrl,
        List<String> genres,
        List<String> tags,
        String creator,
        String creatorName,
        int durationSeconds,
        double averageRating,
        int ratingCount,
        int myRating,
        boolean inMyList,
        int resumeSeconds,
        String access,
        long rentalPriceCents,
        String currency,
        boolean entitled,
        boolean downloadable,
        boolean interactive,
        boolean certificate,
        String trailerVideoId,
        List<ChapterDto> chapters,
        List<EpisodeDto> episodes,
        List<TitleCardDto> similar) {
}
