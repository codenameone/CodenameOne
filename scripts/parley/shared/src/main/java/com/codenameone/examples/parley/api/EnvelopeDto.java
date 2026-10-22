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

/// One message as one device receives it.
///
/// @param id The envelope id, acknowledged to delete it.
/// @param messageId The message id, shared by every copy.
/// @param conversationId The conversation.
/// @param sender The sending account.
/// @param senderDevice The sending device.
/// @param kind MESSAGE, RECEIPT, TYPING, KEYS, SIGNAL or SYSTEM.
/// @param header The ratchet header, base64.
/// @param ciphertext The payload, base64; for SYSTEM and bridged messages, plain text.
/// @param sentAt The server's timestamp.
/// @param expiresAt When the disappearing timer deletes it, 0 for never.
@Mapped
public record EnvelopeDto(
        String id,
        String messageId,
        String conversationId,
        String sender,
        String senderDevice,
        String kind,
        String header,
        String ciphertext,
        long sentAt,
        long expiresAt) {
}
