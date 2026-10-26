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
package com.codename1.crypto.hd;

/// A BIP-32 derivation path such as `m/44'/60'/0'/0/0`: a list of child indices, each
/// hardened when written with `'`, `h` or `H`.
@com.codename1.impl.SharedWithBackend
public final class DerivationPath {
    /// The offset that marks a hardened index.
    public static final long HARDENED = 0x80000000L;

    private final long[] indices;

    private DerivationPath(long[] indices) {
        this.indices = indices;
    }

    /// Parses a path. A leading `m` (or `m/`) is optional; an empty path or `m` alone is
    /// the master key itself.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a component that is not a number below 2^31
    public static DerivationPath parse(String path) {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }
        String p = path.trim();
        if (p.equals("m") || p.equals("M") || p.length() == 0) {
            return new DerivationPath(new long[0]);
        }
        if (p.startsWith("m/") || p.startsWith("M/")) {
            p = p.substring(2);
        }
        java.util.ArrayList<String> parts = new java.util.ArrayList<String>();
        int start = 0;
        for (int iter = 0; iter <= p.length(); iter++) {
            if (iter == p.length() || p.charAt(iter) == '/') {
                parts.add(p.substring(start, iter));
                start = iter + 1;
            }
        }
        long[] out = new long[parts.size()];
        for (int iter = 0; iter < out.length; iter++) {
            String part = parts.get(iter);
            boolean hardened = false;
            if (part.length() > 0) {
                char last = part.charAt(part.length() - 1);
                if (last == '\'' || last == 'h' || last == 'H') {
                    hardened = true;
                    part = part.substring(0, part.length() - 1);
                }
            }
            if (part.length() == 0 || part.length() > 10) {
                throw new IllegalArgumentException("bad path component in " + path);
            }
            long value = 0;
            for (int c = 0; c < part.length(); c++) {
                char ch = part.charAt(c);
                if (ch < '0' || ch > '9') {
                    throw new IllegalArgumentException("bad path component in " + path);
                }
                value = value * 10 + (ch - '0');
            }
            if (value >= HARDENED) {
                throw new IllegalArgumentException("path index too large in " + path);
            }
            out[iter] = hardened ? value + HARDENED : value;
        }
        return new DerivationPath(out);
    }

    /// A path from raw indices, each already including [#HARDENED] where wanted.
    public static DerivationPath of(long[] indices) {
        long[] copy = new long[indices.length];
        for (int iter = 0; iter < copy.length; iter++) {
            if (indices[iter] < 0 || indices[iter] > 0xffffffffL) {
                throw new IllegalArgumentException("index out of range");
            }
            copy[iter] = indices[iter];
        }
        return new DerivationPath(copy);
    }

    /// The BIP-44 path `m/44'/coin'/account'/change/index`.
    public static DerivationPath bip44(int coinType, int account, int change, long index) {
        return of(new long[] {44 + HARDENED, coinType + HARDENED, account + HARDENED, change, index});
    }

    /// This path with one more index.
    public DerivationPath child(long index) {
        long[] out = new long[indices.length + 1];
        System.arraycopy(indices, 0, out, 0, indices.length);
        out[indices.length] = index;
        return of(out);
    }

    /// The number of indices.
    public int length() {
        return indices.length;
    }

    /// The index at a position, including [#HARDENED] when hardened.
    public long get(int position) {
        return indices[position];
    }

    /// Tells whether every index is hardened, which [Slip10] requires for Ed25519.
    public boolean isFullyHardened() {
        for (int iter = 0; iter < indices.length; iter++) {
            if (indices[iter] < HARDENED) {
                return false;
            }
        }
        return true;
    }

    /// The path in `m/44'/60'/0'` form.
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("m");
        for (int iter = 0; iter < indices.length; iter++) {
            out.append('/');
            if (indices[iter] >= HARDENED) {
                out.append(indices[iter] - HARDENED).append('\'');
            } else {
                out.append(indices[iter]);
            }
        }
        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DerivationPath)) {
            return false;
        }
        long[] other = ((DerivationPath) o).indices;
        if (other.length != indices.length) {
            return false;
        }
        for (int iter = 0; iter < other.length; iter++) {
            if (other[iter] != indices[iter]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 17;
        for (int iter = 0; iter < indices.length; iter++) {
            h = h * 31 + (int) (indices[iter] ^ (indices[iter] >>> 32));
        }
        return h;
    }
}
