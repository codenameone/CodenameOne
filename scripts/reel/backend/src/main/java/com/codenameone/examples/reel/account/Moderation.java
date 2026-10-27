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
package com.codenameone.examples.reel.account;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.api.ModerationEventDto;
import com.codenameone.examples.reel.domain.ModerationEvent;
import com.codenameone.examples.reel.repository.ModerationEventRepository;

import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The record of what the admins did to an account, and why.
///
/// Flagging, blocking, approving and rejecting all change an account's state,
/// and the state alone does not say who decided it or on what grounds. This
/// does. A row is written with the change and never altered afterwards.
@Component
public class Moderation {
    public static final String FLAG = "flag";
    public static final String UNFLAG = "unflag";
    public static final String BLOCK = "block";
    public static final String UNBLOCK = "unblock";
    public static final String APPROVE = "approve";
    public static final String REJECT = "reject";
    public static final String ROLES = "roles";
    /// Account suspension; the column holds 16 characters.
    public static final String SUSPEND = "suspend";
    public static final String UNSUSPEND = "unsuspend";
    /// A title of the account's taken down or restored.
    public static final String TAKEDOWN = "takedown";
    public static final String RESTORE = "restore";

    private final ModerationEventRepository events;

    public Moderation(ModerationEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(String username, String by, String action, String reason)
            throws IOException {
        // Two changes made in the same millisecond -- a block and the flag that
        // came with it -- would otherwise be listed in either order. The later
        // one is stamped after the one before it, so the list is the order in
        // which things were done.
        Instant newest = events.newest(username);
        Instant after = newest == null ? Instant.EPOCH : newest.plusMillis(1L);
        Instant now = Instant.now();
        record(username, by, action, reason, now.isAfter(after) ? now : after);
    }

    /// As [#record(String, String, String, String)], at a time of the caller's
    /// choosing; the demo data is the one caller that has a past to write.
    @Transactional
    public void record(String username, String by, String action, String reason, Instant at)
            throws IOException {
        String why = reason == null ? "" : reason.trim();
        ModerationEvent event = new ModerationEvent();
        event.setId(Ids.next());
        event.setUsername(username);
        event.setCreatedAt(at);
        event.setActor(by);
        event.setAction(action);
        event.setReason(why.length() > 255 ? why.substring(0, 255) : why);
        events.insert(event);
    }

    /// An account's events, newest first.
    @Transactional(readOnly = true)
    public List<ModerationEventDto> events(String username) throws IOException {
        List<ModerationEvent> rows = events.findTop100ByUsernameOrderByCreatedAtDescIdAsc(username);
        List<ModerationEventDto> out = new ArrayList<ModerationEventDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ModerationEvent row = rows.get(iter);
            out.add(new ModerationEventDto(row.getCreatedAt().toEpochMilli(), row.getActor(), row.getAction(),
                    row.getReason()));
        }
        return out;
    }

    @Transactional
    public void forget(String username) throws IOException {
        events.deleteByUsername(username);
    }
}
