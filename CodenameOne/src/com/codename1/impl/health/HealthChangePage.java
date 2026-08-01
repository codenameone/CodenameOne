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
package com.codename1.impl.health;

import com.codename1.health.HealthSample;

import java.util.ArrayList;
import java.util.List;

/// One page of changes drained from a polling backend.
///
/// This is the decoded form of what the Health Connect bridge returns for
/// a change token. It is deliberately a plain carrier rather than a public
/// type: the portable vocabulary for a change is
/// [com.codename1.health.HealthChangeBatch], which this is turned into
/// once the subscription it belongs to is known.
public final class HealthChangePage {

    private final String nextToken;
    private final boolean expired;
    private final boolean more;

    final List<HealthSample> added = new ArrayList<HealthSample>();
    final List<String> deletedIds = new ArrayList<String>();

    HealthChangePage(String nextToken, boolean expired, boolean more) {
        this.nextToken = nextToken;
        this.expired = expired;
        this.more = more;
    }

    /// The token to poll with next. Never null, though it may be empty
    /// when the backend declined to issue one.
    public String getNextToken() {
        return nextToken == null ? "" : nextToken;
    }

    /// True when the token used for this poll had aged out, in which case
    /// the changes are incomplete and a full resync is required. Health
    /// Connect expires tokens after 30 days.
    public boolean isExpired() {
        return expired;
    }

    /// True when the backend has further pages queued behind this one.
    public boolean hasMore() {
        return more;
    }

    /// Samples added or updated since the previous poll.
    public List<HealthSample> getAdded() {
        return added;
    }

    /// Identifiers of records deleted since the previous poll.
    public List<String> getDeletedIds() {
        return deletedIds;
    }
}
