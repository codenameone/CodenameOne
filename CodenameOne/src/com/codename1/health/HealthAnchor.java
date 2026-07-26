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
package com.codename1.health;

/// An opaque cursor marking how far through a data type's change history
/// the app has read. Wraps HealthKit's `HKQueryAnchor` and Health
/// Connect's changes token.
///
/// You normally never touch this: [HealthStore] persists the anchor for
/// each subscription itself, which is what lets a subscription survive the
/// app being killed. It is exposed only so that an app syncing to its own
/// server can checkpoint the cursor alongside its upload watermark and
/// keep the two consistent.
///
/// Anchors expire. Health Connect change tokens are invalid after 30 days,
/// and an iOS anchor can be rejected after a restore from backup. When
/// that happens the next batch reports
/// [HealthChangeBatch#isResyncRequired()] and the app must fall back to a
/// full time-range read.
public final class HealthAnchor {

    private final String token;

    private HealthAnchor(String token) {
        this.token = token;
    }

    /// Wraps a platform cursor. Called by ports.
    public static HealthAnchor of(String token) {
        return token == null ? null : new HealthAnchor(token);
    }

    /// Restores an anchor previously written out with
    /// [#toStorableString()]. Returns null for null or empty input, which
    /// callers should treat as "start from the beginning".
    public static HealthAnchor fromStorableString(String stored) {
        if (stored == null || stored.length() == 0) {
            return null;
        }
        return new HealthAnchor(stored);
    }

    /// A form of this anchor safe to persist in `Storage`, `Preferences`
    /// or your own database, and to pass back to
    /// [#fromStorableString(String)] on a later launch.
    ///
    /// The contents are platform-specific and must not be parsed, compared
    /// for ordering, or sent to a different device.
    public String toStorableString() {
        return token;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HealthAnchor)) {
            return false;
        }
        HealthAnchor other = (HealthAnchor) o;
        return token == null ? other.token == null : token.equals(other.token);
    }

    public int hashCode() {
        return token == null ? 0 : token.hashCode();
    }

    public String toString() {
        return "HealthAnchor[" + (token == null ? "none"
                : token.length() + " chars") + "]";
    }
}
