/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.bitcoin;

/// Strict DER (BIP-66) encoding of an ECDSA signature.
@com.codename1.impl.SharedWithBackend
public final class Der {
    private Der() {
    }

    /// `30 len 02 len r 02 len s` of two 32-byte big-endian integers.
    public static byte[] encode(byte[] r, byte[] s) {
        byte[] ri = integer(r);
        byte[] si = integer(s);
        byte[] out = new byte[6 + ri.length + si.length];
        out[0] = 0x30;
        out[1] = (byte) (4 + ri.length + si.length);
        out[2] = 0x02;
        out[3] = (byte) ri.length;
        System.arraycopy(ri, 0, out, 4, ri.length);
        out[4 + ri.length] = 0x02;
        out[5 + ri.length] = (byte) si.length;
        System.arraycopy(si, 0, out, 6 + ri.length, si.length);
        return out;
    }

    /// The `r || s` 64 bytes of a strict DER signature (without the sighash byte).
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for anything BIP-66 rejects
    public static byte[] decode(byte[] der) {
        if (der.length < 8 || der.length > 72 || der[0] != 0x30 || (der[1] & 0xff) != der.length - 2) {
            throw new IllegalArgumentException("not a DER signature");
        }
        int rLen = der[3] & 0xff;
        if (der[2] != 0x02 || 5 + rLen >= der.length) {
            throw new IllegalArgumentException("not a DER signature");
        }
        int sLen = der[5 + rLen] & 0xff;
        if (der[4 + rLen] != 0x02 || 6 + rLen + sLen != der.length) {
            throw new IllegalArgumentException("not a DER signature");
        }
        byte[] out = new byte[64];
        copyInteger(der, 4, rLen, out, 0);
        copyInteger(der, 6 + rLen, sLen, out, 32);
        return out;
    }

    private static void copyInteger(byte[] der, int at, int len, byte[] out, int to) {
        if (len == 0 || (der[at] & 0x80) != 0 || (len > 1 && der[at] == 0 && (der[at + 1] & 0x80) == 0)) {
            throw new IllegalArgumentException("non-canonical DER integer");
        }
        int start = at;
        int n = len;
        if (der[at] == 0) {
            start++;
            n--;
        }
        if (n > 32) {
            throw new IllegalArgumentException("DER integer over 256 bits");
        }
        System.arraycopy(der, start, out, to + 32 - n, n);
    }

    private static byte[] integer(byte[] v) {
        int start = 0;
        while (start < v.length - 1 && v[start] == 0) {
            start++;
        }
        boolean pad = (v[start] & 0x80) != 0;
        byte[] out = new byte[v.length - start + (pad ? 1 : 0)];
        System.arraycopy(v, start, out, pad ? 1 : 0, v.length - start);
        return out;
    }
}
