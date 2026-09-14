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
package com.codename1.backend;

/**
 * ASCII hexadecimal, and nothing else.
 *
 * Integer.parseInt(text, 16) is not a hex parser for protocol input. It accepts
 * a leading sign, so "+1" is 1 and "-0" is 0; it accepts any Unicode digit that
 * Character.digit knows, so U+0661 (ARABIC-INDIC DIGIT ONE) is also 1. Every one
 * of those is a second spelling of a value, and a conforming intermediary in front
 * of this server rejects them -- which is the definition of a request-smuggling
 * gap when the value being spelled is a chunk size.
 *
 * The same leniency had already produced three separate defects here: percent
 * escapes in static paths, in generated routers and in generated dispatchers.
 * This is the one implementation, so the fifth caller cannot disagree with the
 * other four.
 */
final class Hex {
    private Hex() {
    }

    /** The value of one ASCII hex digit, or -1 for anything else. */
    static int digit(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /**
     * `1*HEXDIG` over [from, to), or -1 for an empty run, a non-digit, or a value
     * past Integer.MAX_VALUE. -1 rather than an exception because every caller
     * answers a protocol error with a status, not a stack trace.
     */
    static int parse(String text, int from, int to) {
        if(text == null || to <= from || to > text.length()) {
            return -1;
        }
        long value = 0;
        for(int iter = from ; iter < to ; iter++) {
            int d = digit(text.charAt(iter));
            if(d < 0) {
                return -1;
            }
            value = (value << 4) | d;
            if(value > Integer.MAX_VALUE) {
                return -1;
            }
        }
        return (int)value;
    }
}
