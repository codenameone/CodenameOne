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
        if (cents == Long.MIN_VALUE) {
            // Has no positive counterpart, so it has no decimal form below.
            throw new IllegalArgumentException("amount out of range");
        }
        this.cents = cents;
    }

    /**
     * Parses a whole amount ("12") or one with exactly two decimals ("12.34"),
     * optionally negative; anything else, or an amount too large to hold, is
     * refused.
     */
    public static Money parse(String s) {
        int i = s.startsWith("-") ? 1 : 0;
        int dot = s.indexOf('.');
        int end = dot < 0 ? s.length() : dot;
        if (end == i || (dot >= 0 && s.length() - dot != 3)) {
            throw new IllegalArgumentException("expected an amount");
        }
        long units = 0;
        for (int p = i; p < s.length(); p++) {
            if (p == dot) {
                continue;
            }
            char c = s.charAt(p);
            if (c < '0' || c > '9' || units > (Long.MAX_VALUE - (c - '0')) / 10) {
                throw new IllegalArgumentException("expected an amount");
            }
            units = units * 10 + (c - '0');
        }
        if (dot < 0) {
            if (units > Long.MAX_VALUE / 100) {
                throw new IllegalArgumentException("expected an amount");
            }
            units *= 100;
        }
        return new Money(i == 1 ? -units : units);
    }

    public String toDecimalString() {
        long abs = Math.abs(cents);
        long frac = abs % 100;
        return (cents < 0 ? "-" : "") + abs / 100 + (frac < 10 ? ".0" : ".") + frac;
    }
}
