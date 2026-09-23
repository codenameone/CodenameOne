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
package com.codename1.backend.otel;

/**
 * W3C Trace Context (https://www.w3.org/TR/trace-context/): parsing a
 * {@code traceparent}, writing one, and vetting a {@code tracestate}.
 *
 * <p>Parsing is STRICT the way the specification asks, because a header that
 * arrives from the internet is input, and the answer to a malformed one is always
 * the same: ignore it and start a new trace. Upper-case hex, an all-zero id, the
 * forbidden version ff and a version-00 header with anything after the flags are
 * all refused.
 */
final class TraceContext {
    final long traceHi;
    final long traceLo;
    final long spanId;
    final int flags;

    TraceContext(long traceHi, long traceLo, long spanId, int flags) {
        this.traceHi = traceHi;
        this.traceLo = traceLo;
        this.spanId = spanId;
        this.flags = flags;
    }

    boolean sampled() {
        return (flags & 1) != 0;
    }

    /** The header, or null when it is not a valid one. */
    static TraceContext parse(String header) {
        if(header == null) {
            return null;
        }
        String h = header.trim();
        // version "-" trace-id "-" parent-id "-" flags: 2+1+32+1+16+1+2
        if(h.length() < 55) {
            return null;
        }
        int version = hexByte(h, 0);
        if(version < 0 || version == 0xff || h.charAt(2) != '-' || h.charAt(35) != '-'
                || h.charAt(52) != '-') {
            return null;
        }
        if(version == 0 && h.length() != 55) {
            return null;
        }
        // A LATER version may append fields, and a parser of this version reads
        // the ones it knows -- but only if the next character ends the flags.
        if(version > 0 && h.length() > 55 && h.charAt(55) != '-') {
            return null;
        }
        long hi = hex64(h, 3);
        long lo = hex64(h, 19);
        long span = hex64(h, 36);
        int flags = hexByte(h, 53);
        if(hi == BAD || lo == BAD || span == BAD || flags < 0) {
            return null;
        }
        if((hi == 0 && lo == 0) || span == 0) {
            return null;
        }
        return new TraceContext(hi, lo, span, flags);
    }

    /** Sixteen lower-case hex digits at {@code from}, or {@link #BAD}. */
    private static final long BAD = 0x7fffffffffffffffL;

    private static long hex64(String text, int from) {
        long value = 0;
        for(int iter = 0 ; iter < 16 ; iter++) {
            int d = lowerHex(text.charAt(from + iter));
            if(d < 0) {
                // BAD is itself a legal id, so a real one that happens to equal it
                // would be refused; one in 2^64, and the cost is a new trace.
                return BAD;
            }
            value = (value << 4) | d;
        }
        return value;
    }

    private static int hexByte(String text, int from) {
        int hi = lowerHex(text.charAt(from));
        int lo = lowerHex(text.charAt(from + 1));
        return hi < 0 || lo < 0 ? -1 : (hi << 4) | lo;
    }

    private static int lowerHex(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return -1;
    }

    /** {@code 00-<trace>-<span>-<flags>}. */
    static String format(long traceHi, long traceLo, long spanId, boolean sampled) {
        StringBuilder out = new StringBuilder(55);
        out.append("00-");
        appendHex(out, traceHi);
        appendHex(out, traceLo);
        out.append('-');
        appendHex(out, spanId);
        out.append(sampled ? "-01" : "-00");
        return out.toString();
    }

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    static void appendHex(StringBuilder out, long value) {
        for(int shift = 60 ; shift >= 0 ; shift -= 4) {
            out.append(DIGITS[(int)((value >>> shift) & 0xf)]);
        }
    }

    static String hex(long value) {
        StringBuilder out = new StringBuilder(16);
        appendHex(out, value);
        return out.toString();
    }

    /**
     * The tracestate to carry on, or null. It is opaque to this server -- vendors
     * keep their own state in it -- so a valid one is passed through exactly as it
     * came. A header that is not a valid list is DISCARDED whole, which is what the
     * specification requires of a receiver: forwarding it would hand the next hop a
     * value a strict parser rejects, and with it every vendor's entry.
     *
     * Valid means at most 32 members, each {@code key=value} with the grammar the
     * specification gives (below), no key twice, and no more than 512 characters in
     * all. Empty members -- ",," -- are allowed and dropped by the grammar itself.
     */
    static String vetTracestate(String state) {
        if(state == null) {
            return null;
        }
        String s = state.trim();
        if(s.length() == 0 || s.length() > 512) {
            return null;
        }
        java.util.HashSet keys = new java.util.HashSet();
        int members = 0;
        int at = 0;
        while(at <= s.length()) {
            int comma = s.indexOf(',', at);
            if(comma < 0) {
                comma = s.length();
            }
            String member = s.substring(at, comma).trim();
            at = comma + 1;
            if(member.length() == 0) {
                continue;
            }
            int eq = member.indexOf('=');
            if(eq <= 0 || !validKey(member.substring(0, eq))
                    || !validValue(member.substring(eq + 1))) {
                return null;
            }
            if(!keys.add(member.substring(0, eq)) || ++members > 32) {
                return null;
            }
        }
        return members == 0 ? null : s;
    }

    /**
     * {@code simple-key = lcalpha 0*255( lcalpha / DIGIT / "_" / "-"/ "*" / "/" )}, or
     * {@code multi-tenant-key = tenant-id "@" system-id} where the tenant is
     * {@code ( lcalpha / DIGIT ) 0*240( ... )} and the system
     * {@code lcalpha 0*13( ... )}.
     */
    static boolean validKey(String key) {
        int at = key.indexOf('@');
        if(at < 0) {
            return key.length() <= 256 && isLower(key.charAt(0)) && keyChars(key, 1);
        }
        String tenant = key.substring(0, at);
        String system = key.substring(at + 1);
        return tenant.length() >= 1 && tenant.length() <= 241
                && (isLower(tenant.charAt(0)) || isDigit(tenant.charAt(0))) && keyChars(tenant, 1)
                && system.length() >= 1 && system.length() <= 14
                && isLower(system.charAt(0)) && keyChars(system, 1);
    }

    private static boolean keyChars(String text, int from) {
        for(int iter = from ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if(!isLower(c) && !isDigit(c) && c != '_' && c != '-' && c != '*' && c != '/') {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code value = 0*255(chr) nblk-chr}: printable ASCII except comma and equals,
     * spaces allowed inside but not at the end, 256 characters at most.
     */
    static boolean validValue(String value) {
        if(value.length() == 0 || value.length() > 256) {
            return false;
        }
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c < 0x20 || c > 0x7e || c == ',' || c == '=') {
                return false;
            }
        }
        return value.charAt(value.length() - 1) != ' ';
    }

    private static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
