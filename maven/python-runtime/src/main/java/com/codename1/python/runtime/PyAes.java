/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

import com.codename1.io.Util;

/// `_cn1aes`: the AES block cipher the `cryptography` package's
/// `hazmat.primitives.ciphers` is written over. It is the platform's own,
/// through `com.codename1.io.Util`, so every port supplies the cipher and
/// none is written here. Only unpadded CBC is exposed; ECB is built from it a
/// block at a time on the Python side, and padding is the caller's, as in the
/// real package.
final class PyAes {
    private static final String CBC = "AES/CBC/NoPadding";

    private PyAes() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1aes", new String[0]);
        m.setAttr("cbc_encrypt", PyBuiltin.positional("cbc_encrypt", a ->
                new PyBytes(Util.aesEncrypt(CBC, PyBytes.like(a[0]), PyBytes.like(a[1]), null,
                        PyBytes.like(a[2])))));
        m.setAttr("cbc_decrypt", PyBuiltin.positional("cbc_decrypt", a ->
                new PyBytes(Util.aesDecrypt(CBC, PyBytes.like(a[0]), PyBytes.like(a[1]), null,
                        PyBytes.like(a[2])))));
        return m;
    }
}
