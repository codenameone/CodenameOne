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
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// One account in full, for the admin deciding what to do about it.
///
/// @param rides Rides taken part in, as rider or driver.
/// @param cancellations How many of those rides were cancelled.
/// @param rating As a driver; 0 for an account that has not driven.
/// @param spent Cents paid as a rider.
/// @param earned Cents earned as a driver.
/// @param events Newest first.
@Mapped
public record UserDetailDto(
        UserDto user,
        int rides,
        int cancellations,
        double rating,
        long spent,
        long earned,
        List<ModerationEventDto> events) {
}
