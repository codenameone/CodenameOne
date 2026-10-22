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

import java.util.List;

/// A message on its way out, already encrypted once per receiving device.
///
/// @param conversationId The conversation.
/// @param senderDevice The sending device, which must be the caller's.
/// @param clientId The sender's id for it, unique per sender, so a retry is not stored twice.
/// @param kind What the payload is, for routing and push only: MESSAGE, RECEIPT, TYPING, KEYS or SIGNAL.
/// @param copies One copy per recipient device, including the sender's other devices.
/// @param plaintext Only for bridged conversations, which are not end-to-end encrypted.
@Mapped
public record OutgoingDto(
        String conversationId,
        String senderDevice,
        String clientId,
        String kind,
        List<EnvelopeCopyDto> copies,
        String plaintext) {
}
