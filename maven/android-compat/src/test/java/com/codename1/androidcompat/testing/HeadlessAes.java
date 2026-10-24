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
package com.codename1.androidcompat.testing;

/// [HeadlessImplementation]'s AES, through the host JDK, kept in a file of
/// its own so the CodeQL exclusion for it covers nothing else.
final class HeadlessAes {

    private HeadlessAes() {
    }

    static byte[] run(int mode, String transformation, byte[] key, byte[] iv, byte[] aad, byte[] data) {
        try {
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance(transformation);
            javax.crypto.spec.SecretKeySpec k = new javax.crypto.spec.SecretKeySpec(key, "AES");
            if (iv == null) {
                c.init(mode, k);
            } else if (transformation.indexOf("GCM") >= 0) {
                c.init(mode, k, new javax.crypto.spec.GCMParameterSpec(128, iv));
            } else {
                c.init(mode, k, new javax.crypto.spec.IvParameterSpec(iv));
            }
            if (aad != null) {
                c.updateAAD(aad);
            }
            return c.doFinal(data);
        } catch (java.security.GeneralSecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
