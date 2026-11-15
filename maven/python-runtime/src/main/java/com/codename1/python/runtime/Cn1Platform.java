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
import com.codename1.ui.Display;

/// What a client application lends the runtime from the port: its resource
/// lookup and its AES. The generated bootstrap of a client application calls
/// [#install]; a server or command-line program never names this class, so its
/// translation never reaches `Display` or `Util`, which a server's class
/// library does not have.
public final class Cn1Platform {
    private Cn1Platform() {
    }

    /// Installs the client's resource lookup and cipher.
    public static void install() {
        PyPackageData.setOpener(name -> Display.isInitialized()
                ? Display.getInstance().getResourceAsStream(PyPackageData.class, name) : null);
        PyAes.setCipher((encrypt, transformation, key, iv, data) -> encrypt
                ? Util.aesEncrypt(transformation, key, iv, null, data)
                : Util.aesDecrypt(transformation, key, iv, null, data));
    }
}
