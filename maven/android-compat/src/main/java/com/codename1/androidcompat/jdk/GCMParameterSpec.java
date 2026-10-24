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
package com.codename1.androidcompat.jdk;

/// `javax.crypto.spec.GCMParameterSpec` for the Codename One runtime.
public class GCMParameterSpec implements java.security.spec.AlgorithmParameterSpec {
    private final int tLen;
    private final byte[] iv;

    public GCMParameterSpec(int tLen, byte[] src) {
        this(tLen, src, 0, src == null ? 0 : src.length);
    }

    public GCMParameterSpec(int tLen, byte[] src, int offset, int len) {
        if (tLen < 0) {
            throw new IllegalArgumentException("Length argument is negative");
        }
        if (src == null) {
            throw new IllegalArgumentException("src array is null");
        }
        if (offset < 0 || len < 0 || offset + len > src.length) {
            throw new IllegalArgumentException("Invalid offset/length combination");
        }
        this.tLen = tLen;
        this.iv = new byte[len];
        System.arraycopy(src, offset, this.iv, 0, len);
    }

    public int getTLen() {
        return tLen;
    }

    public byte[] getIV() {
        byte[] out = new byte[iv.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        return out;
    }
}
