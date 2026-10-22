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

/// A chat: a direct conversation between two accounts, a group, or a channel
/// only its admins post to.
///
/// @param id The conversation id.
/// @param kind DIRECT, GROUP or CHANNEL.
/// @param title The group's name; for a direct chat, the other person's.
/// @param about The group's description.
/// @param members Its members.
/// @param myRole The caller's role.
/// @param muted The caller muted it.
/// @param archived The caller archived it.
/// @param pinned The caller pinned it.
/// @param disappearingSeconds The disappearing-message timer, 0 for off.
/// @param adminsOnly Only admins may post.
/// @param bridge TELEGRAM or WHATSAPP when it is bridged, else empty.
/// @param lastActivityAt When the last message arrived.
/// @param hasPhoto A group photo was set.
@Mapped
public record ConversationDto(
        String id,
        String kind,
        String title,
        String about,
        List<MemberDto> members,
        String myRole,
        boolean muted,
        boolean archived,
        boolean pinned,
        int disappearingSeconds,
        boolean adminsOnly,
        String bridge,
        long lastActivityAt,
        boolean hasPhoto) {
}
