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
package com.codenameone.examples.parley.api;

import com.codename1.annotations.Mapped;

/// A call, live or in history.
///
/// @param id The call id.
/// @param conversationId The conversation.
/// @param caller Who called.
/// @param callee Who was called.
/// @param video A video call.
/// @param state RINGING, ACCEPTED, ENDED, MISSED, DECLINED or BUSY.
/// @param startedAt When it was placed.
/// @param answeredAt When it was answered, 0 if it never was.
/// @param endedAt When it ended, 0 while live.
@Mapped
public record CallDto(
        String id,
        String conversationId,
        String caller,
        String callee,
        boolean video,
        String state,
        long startedAt,
        long answeredAt,
        long endedAt) {
}
