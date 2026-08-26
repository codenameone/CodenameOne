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
package com.codename1.call;

/// Who is on the other end: an address plus what kind of address it is.
///
/// The system uses both parts. A [CallHandleType#PHONE_NUMBER] is matched
/// against the address book and may be reformatted for display, so it is
/// worth passing E.164; a [CallHandleType#GENERIC] handle is shown
/// verbatim. Passing a username as a phone number produces a call log entry
/// the user cannot call back.
public final class CallHandle {
    private final CallHandleType type;
    private final String value;

    /// Creates a handle.
    ///
    /// @param type what kind of address `value` is; null is treated as
    ///     [CallHandleType#GENERIC]
    /// @param value the address itself, never null or empty
    /// @throws IllegalArgumentException if `value` is null or empty
    public CallHandle(CallHandleType type, String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("Call handle value is required");
        }
        this.type = type == null ? CallHandleType.GENERIC : type;
        this.value = value;
    }

    /// Convenience for a telephone number.
    ///
    /// @param number the number, ideally in E.164
    /// @return a phone-number handle
    public static CallHandle phone(String number) {
        return new CallHandle(CallHandleType.PHONE_NUMBER, number);
    }

    /// Convenience for an email address.
    ///
    /// @param address the address
    /// @return an email handle
    public static CallHandle email(String address) {
        return new CallHandle(CallHandleType.EMAIL_ADDRESS, address);
    }

    /// Convenience for a username, room name or other opaque address.
    ///
    /// @param value the address, shown to the user verbatim
    /// @return a generic handle
    public static CallHandle generic(String value) {
        return new CallHandle(CallHandleType.GENERIC, value);
    }

    /// What kind of address [#getValue()] is. Never null.
    public CallHandleType getType() {
        return type;
    }

    /// The address itself. Never null or empty.
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CallHandle)) {
            return false;
        }
        CallHandle other = (CallHandle) o;
        return type == other.type && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + value.hashCode();
    }

    @Override
    public String toString() {
        return type.name() + ":" + value;
    }
}
