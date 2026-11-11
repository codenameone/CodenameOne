/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Licensed under the GNU General Public License version 2 with the Classpath
 * exception, as the rest of the Codename One Python runtime.
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
