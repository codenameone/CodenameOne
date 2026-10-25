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
package com.codenameone.developerguide.backend.contract;

/** An amount kept in cents, for the Backend chapter's adapter example. */
public final class Money {
    private final long cents;

    public Money(long cents) {
        this.cents = cents;
    }

    /** Parses "12.34"; anything else is refused. */
    public static Money parse(String s) {
        int dot = s.indexOf('.');
        try {
            if (dot < 0) {
                return new Money(Long.parseLong(s) * 100);
            }
            if (s.length() - dot != 3) {
                throw new IllegalArgumentException("expected an amount");
            }
            long whole = Long.parseLong(s.substring(0, dot));
            long frac = Long.parseLong(s.substring(dot + 1));
            return new Money(whole * 100 + (s.startsWith("-") ? -frac : frac));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("expected an amount");
        }
    }

    public String toDecimalString() {
        long abs = Math.abs(cents);
        String frac = String.valueOf(abs % 100);
        return (cents < 0 ? "-" : "") + abs / 100 + "." + (frac.length() == 1 ? "0" + frac : frac);
    }
}
