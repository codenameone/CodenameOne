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

/// One data type paired with one direction of access. The unit of
/// authorization on both platforms.
///
/// ```java
/// store.requestAuthorization(
///         HealthAccess.read(HealthDataType.STEPS),
///         HealthAccess.read(HealthDataType.HEART_RATE),
///         HealthAccess.write(HealthDataType.BODY_MASS));
/// ```
///
/// Request the narrowest set you actually need, at the moment you need it.
/// Both stores show the user every type you ask for in one sheet, and a
/// long list at first launch is the most common reason people decline.
public final class HealthAccess {

    private final HealthDataType type;
    private final boolean write;

    private HealthAccess(HealthDataType type, boolean write) {
        if (type == null) {
            throw new IllegalArgumentException("access requires a data type");
        }
        this.type = type;
        this.write = write;
    }

    /// Permission to read this type.
    public static HealthAccess read(HealthDataType type) {
        return new HealthAccess(type, false);
    }

    /// Permission to write this type.
    public static HealthAccess write(HealthDataType type) {
        return new HealthAccess(type, true);
    }

    /// Both directions for this type, as a two-element array suitable for
    /// spreading into
    /// [HealthStore#requestAuthorization(HealthAccess...)].
    public static HealthAccess[] readWrite(HealthDataType type) {
        return new HealthAccess[] { read(type), write(type) };
    }

    /// The data type this access applies to.
    public HealthDataType getType() {
        return type;
    }

    /// `true` when this is read access.
    public boolean isRead() {
        return !write;
    }

    /// `true` when this is write access.
    public boolean isWrite() {
        return write;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HealthAccess)) {
            return false;
        }
        HealthAccess other = (HealthAccess) o;
        return type == other.type && write == other.write;
    }

    public int hashCode() {
        return type.getId().hashCode() * 31 + (write ? 1 : 0);
    }

    public String toString() {
        return (write ? "write " : "read ") + type.getId();
    }
}
