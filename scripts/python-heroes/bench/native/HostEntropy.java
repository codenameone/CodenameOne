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
package com.pyhero;

import com.codename1.impl.JdkHost;

/// The host's entropy for a native benchmark binary. An application's port
/// installs a `JdkHost` that reads the device's secure generator; this binary
/// has no port, and without one `java.security.SecureRandom` -- `os.urandom`,
/// `secrets` and `uuid.uuid4` in Python -- has nothing to read.
public final class HostEntropy extends JdkHost {
    @Override
    public void randomBytes(byte[] out) {
        if (fillImpl(out) != 0) {
            throw new IllegalStateException("the host has no entropy to give");
        }
    }

    private static native int fillImpl(byte[] out);
}
