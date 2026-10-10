/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.account;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.ModerationEventDto;
import com.codenameone.examples.wayline.domain.ModerationEvent;

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

    private final ModerationRepository events;

    public Moderation(ModerationRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(String username, String by, String action, String reason)
            throws IOException {
        // Two changes made in the same millisecond -- a block and the flag that
        // came with it -- would otherwise be listed in either order. The later
        // one is stamped after the one before it, so the list is the order in
        // which things were done.
        Long newest = events.newest(username);
        long after = newest == null ? 0L : newest.longValue() + 1L;
        record(username, by, action, reason, Math.max(System.currentTimeMillis(), after));
    }

    /// As [#record(String, String, String, String)], at a time of the caller's
    /// choosing; the demo data is the one caller that has a past to write.
    @Transactional
    public void record(String username, String by, String action, String reason, long at)
            throws IOException {
        String why = reason == null ? "" : reason.trim();
        ModerationEvent event = new ModerationEvent();
        event.id = Ids.next();
        event.username = username;
        event.createdAt = at;
        event.actor = by;
        event.action = action;
        event.reason = why.length() > 255 ? why.substring(0, 255) : why;
        events.add(event);
    }

    /// An account's events, newest first.
    @Transactional(readOnly = true)
    public List<ModerationEventDto> events(String username) throws IOException {
        List<ModerationEvent> rows = events.of(username, 100);
        List<ModerationEventDto> out = new ArrayList<ModerationEventDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ModerationEvent row = rows.get(iter);
            ModerationEventDto dto = new ModerationEventDto();
            dto.at = row.createdAt;
            dto.by = row.actor;
            dto.action = row.action;
            dto.reason = row.reason;
            out.add(dto);
        }
        return out;
    }

    @Transactional
    public void forget(String username) throws IOException {
        events.removeAll(username);
    }
}
