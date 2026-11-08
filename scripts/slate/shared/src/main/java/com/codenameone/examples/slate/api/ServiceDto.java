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
package com.codenameone.examples.slate.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// Something that can be booked. `providers` names who offers it. Policy values of -1 are
/// the business's own.
@Mapped
public record ServiceDto(
        String id,
        String name,
        String description,
        String category,
        int minutes,
        long priceCents,
        String currency,
        int bufferBefore,
        int bufferAfter,
        int capacity,
        int providersNeeded,
        String resourceKind,
        boolean online,
        int minNoticeMinutes,
        int depositPercent,
        String color,
        String intakePrompt,
        boolean active,
        List<String> providers) {
}
