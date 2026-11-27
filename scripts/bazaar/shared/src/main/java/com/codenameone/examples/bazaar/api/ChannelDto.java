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

/// A connected marketplace account as the back office shows it. Tokens and
/// the notification secret are never part of it.
///
/// @param kind `SIMULATED` or `EBAY`
/// @param status `ACTIVE`, `ERROR` (backing off after failures) or `PAUSED`
/// @param configured whether the server holds the marketplace application's
///     credentials; an unconfigured kind runs the simulated connector
/// @param notifyUrl where the marketplace posts order notifications
@Mapped
public record ChannelDto(
        String id,
        String kind,
        String name,
        String status,
        boolean configured,
        int listings,
        int errors,
        long lastPullAt,
        String lastError,
        String notifyUrl) {
}
