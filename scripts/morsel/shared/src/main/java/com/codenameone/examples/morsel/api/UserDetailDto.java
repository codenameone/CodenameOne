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
package com.codenameone.examples.morsel.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// One account in full, for the admin deciding what to do about it.
///
/// @param deliveries Deliveries taken part in, as customer or courier.
/// @param cancellations How many of those deliveries were cancelled.
/// @param rating As a courier; 0 for an account that has not driven.
/// @param spent Cents paid as a customer.
/// @param earned Cents earned as a courier.
/// @param events Newest first.
@Mapped
public record UserDetailDto(
        UserDto user,
        int deliveries,
        int cancellations,
        double rating,
        long spent,
        long earned,
        List<ModerationEventDto> events) {
}
