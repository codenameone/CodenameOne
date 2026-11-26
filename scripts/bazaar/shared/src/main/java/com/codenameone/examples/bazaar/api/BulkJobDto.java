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

/// A bulk operation and how it went.
///
/// @param kind `EXPORT_PRODUCTS`, `EXPORT_ORDERS` or `IMPORT_PRODUCTS`.
/// @param status `QUEUED`, `RUNNING`, `DONE` or `FAILED`.
/// @param input The JSON Lines to import; empty for an export, and empty in answers.
/// @param errors One line per refused input line, `line N: reason`; at most fifty.
@Mapped
public record BulkJobDto(
        String id,
        String kind,
        String status,
        String input,
        int lineCount,
        int errorCount,
        List<String> errors,
        long createdAt,
        long finishedAt) {
}
