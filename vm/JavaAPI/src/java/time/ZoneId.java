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
package java.time;

import java.util.TimeZone;

/// A time zone: either a fixed [ZoneOffset], or a region such as
/// `Europe/Paris` whose offsets come from the platform's time zone rules.
///
/// As in the JDK, `ZoneId.of("UTC")` and `ZoneId.of("GMT")` are regions
/// named that way, not [ZoneOffset#UTC] (whose id is `Z`), and
/// `ZoneId.of("UTC+01:00")` keeps its prefix in its id.
public class ZoneId {
    private final String id;
    /// The offset of a region that never changes it (`UTC`, `GMT`, `UT`
    /// and those followed by an offset), or null.
    private final ZoneOffset fixed;

    ZoneId(String id) {
        this(id, null);
    }

    private ZoneId(String id, ZoneOffset fixed) {
        this.id = id;
        this.fixed = fixed;
    }

    public static ZoneId of(String zoneId) {
        if (zoneId == null) {
            throw new NullPointerException("zoneId");
        }
        if (zoneId.length() <= 1 || zoneId.startsWith("+") || zoneId.startsWith("-")) {
            return ZoneOffset.of(zoneId);
        }
        if (zoneId.startsWith("UTC") || zoneId.startsWith("GMT")) {
            return ofPrefixed(zoneId, 3);
        }
        if (zoneId.startsWith("UT")) {
            return ofPrefixed(zoneId, 2);
        }
        for (int i = 0; i < zoneId.length(); i++) {
            char c = zoneId.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '~' || c == '/' || c == '.' || c == '_' || c == '+' || c == '-';
            if (!ok) {
                throw new DateTimeException("Invalid ID for region-based ZoneId, invalid format: " + zoneId);
            }
        }
        return new ZoneId(zoneId, null);
    }

    private static ZoneId ofPrefixed(String zoneId, int prefixLength) {
        String prefix = zoneId.substring(0, prefixLength);
        if (zoneId.length() == prefixLength) {
            return new ZoneId(prefix, ZoneOffset.UTC);
        }
        char sign = zoneId.charAt(prefixLength);
        if (sign != '+' && sign != '-') {
            // GMT0 and the like: a region of its own.
            return of0(zoneId);
        }
        ZoneOffset offset;
        try {
            offset = ZoneOffset.of(zoneId.substring(prefixLength));
        } catch (DateTimeException ex) {
            throw new DateTimeException("Invalid ID for offset-based ZoneId: " + zoneId);
        }
        if (offset.getTotalSeconds() == 0) {
            return new ZoneId(prefix, ZoneOffset.UTC);
        }
        return new ZoneId(prefix + offset.getId(), offset);
    }

    private static ZoneId of0(String zoneId) {
        return new ZoneId(zoneId, "GMT0".equals(zoneId) ? ZoneOffset.UTC : null);
    }

    public static ZoneId systemDefault() {
        return of(TimeZone.getDefault().getID());
    }

    /// The zone `prefix` followed by `offset`: `UTC+01:00` for `UTC`,
    /// just `UTC` for a zero offset.
    public static ZoneId ofOffset(String prefix, ZoneOffset offset) {
        if (prefix == null || offset == null) {
            throw new NullPointerException();
        }
        if (prefix.length() == 0) {
            return offset;
        }
        if (!prefix.equals("GMT") && !prefix.equals("UTC") && !prefix.equals("UT")) {
            throw new IllegalArgumentException("prefix should be GMT, UTC or UT, is: " + prefix);
        }
        if (offset.getTotalSeconds() == 0) {
            return new ZoneId(prefix, ZoneOffset.UTC);
        }
        return new ZoneId(prefix + offset.getId(), offset);
    }

    public String getId() {
        return id;
    }

    /// This zone as a [ZoneOffset] when its offset never changes, and
    /// itself otherwise.
    public ZoneId normalized() {
        ZoneOffset f = fixedOffset();
        return f != null ? f : this;
    }

    /// The offset this zone always has, or null for a region whose offset
    /// changes.
    ZoneOffset fixedOffset() {
        return fixed;
    }

    /// The platform time zone with this zone's rules.
    TimeZone toTimeZone() {
        ZoneOffset offset = fixedOffset();
        if (offset != null) {
            return TimeZone.getTimeZone(offset.getTotalSeconds() == 0 ? "GMT" : "GMT" + offset.getId());
        }
        return TimeZone.getTimeZone(id);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZoneId) || obj instanceof ZoneOffset) {
            return false;
        }
        return id.equals(((ZoneId) obj).id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return id;
    }
}
