/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.shield;

/// A short-lived attestation token, ready to be attached to a request.
///
/// The value is opaque to the app. It is meaningful only to the backend that verifies it against
/// the published Codename One signing keys -- do not parse it, and do not make a security decision
/// on the device based on its contents, because a device the attacker controls can be made to say
/// anything.
///
/// #### Expiry is measured locally, on purpose
///
/// Validity is tracked as "fetched at + time to live" using local elapsed time, never by reading an
/// expiry field out of the token and comparing it against the device clock. On a rooted device the
/// clock is attacker-controlled, so a token-embedded expiry can be made to look valid forever. The
/// verifying backend does its own absolute-time check regardless; [#isValid()] exists so the client
/// knows when to refresh, not to enforce anything.
public final class ShieldToken {

    private final String value;
    private final ShieldStatus status;
    private final long fetchedAt;
    private final long ttlMillis;
    private final String binding;

    public ShieldToken(String value, ShieldStatus status, long fetchedAt,
            long ttlMillis, String binding) {
        this.value = value;
        this.status = status == null ? ShieldStatus.OK : status;
        this.fetchedAt = fetchedAt;
        this.ttlMillis = ttlMillis;
        this.binding = binding;
    }

    /// The opaque token to place in the request header. May be null when [#getStatus()] is not
    /// [ShieldStatus#OK].
    public String getValue() {
        return value;
    }

    /// Outcome of the fetch that produced this token.
    public ShieldStatus getStatus() {
        return status;
    }

    /// Milliseconds until this token stops being worth sending, or 0 once it has lapsed.
    public long getMillisUntilExpiry() {
        long remaining = (fetchedAt + ttlMillis) - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    /// True when the token has a value, was fetched successfully, and has not lapsed.
    public boolean isValid() {
        return value != null && status.isSuccess() && getMillisUntilExpiry() > 0;
    }

    /// True once the token is far enough through its lifetime to be worth refreshing in the
    /// background. Refreshing before expiry is what keeps a request from ever having to block.
    public boolean shouldRefresh(int thresholdPercent) {
        if (ttlMillis <= 0) {
            return true;
        }
        long used = System.currentTimeMillis() - fetchedAt;
        return used * 100 >= ttlMillis * thresholdPercent;
    }

    /// The request-binding data this token was minted for, or null when it is a plain
    /// time-limited token not tied to a specific request.
    public String getBinding() {
        return binding;
    }

    /// True when this token was minted for exactly the supplied binding data. A token bound to one
    /// request must not be reused for another; that is the whole point of binding.
    public boolean isBoundTo(String data) {
        if (binding == null) {
            return data == null;
        }
        return binding.equals(data);
    }

    /// Never renders the token value -- these strings end up in logs.
    public String toString() {
        return "ShieldToken[status=" + status.getId()
                + ", validMs=" + getMillisUntilExpiry()
                + ", bound=" + (binding != null) + "]";
    }
}
