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
package com.codename1.impl;

import com.codename1.io.Log;
import com.codename1.l10n.L10NManager;
import com.codename1.security.Hash;
import com.codename1.ui.Display;

/// The [JdkHost] of a Codename One application: the class library's event
/// thread waits, log, secure random bytes, digests and locale symbols, served
/// by `Display`, `Log`, `SecureRandom`, `Hash` and `L10NManager`.
///
/// Internal. The ParparVM ports (iOS and macOS, Windows, Linux, JavaScript)
/// install it from their `init` through [CoreJdkHostInstaller]. On Java SE and
/// Android the real JDK runs and nothing installs it. `JdkHost` itself is part
/// of the class library, not of the core: the core compiles against it, and
/// the Java SE and Android ports carry a copy so this class always has its
/// superclass. Nothing here runs before the display is initialized except
/// what checks for it.
public class CoreJdkHost extends JdkHost {

    /// The levels of [JdkHost] are those of `Log`.
    @Override
    public void log(int level, String message, Throwable thrown) {
        if (message != null) {
            Log.p(message, Math.max(Log.DEBUG, Math.min(Log.ERROR, level)));
        }
        if (thrown != null) {
            Log.e(thrown);
        }
    }

    @Override
    public boolean isUiThread() {
        return Display.isInitialized() && Display.getInstance().isEdt();
    }

    /// On the event dispatch thread the task runs through `invokeAndBlock`,
    /// so the UI keeps serving events while it blocks.
    @Override
    public void blockUi(Runnable task) {
        if (isUiThread()) {
            Display.getInstance().invokeAndBlock(task);
        } else {
            task.run();
        }
    }

    /// Derived from what `L10NManager` formats, since it exposes the
    /// separators only through formatting.
    @Override
    public String[] localeNumberSymbols() {
        if (!Display.isInitialized()) {
            return null;
        }
        L10NManager l10n = L10NManager.getInstance();
        char decimal = firstSeparator(l10n.format(1.5, 1));
        char grouping = firstSeparator(l10n.format(1234567.0, 0));
        return new String[] {
            decimal == 0 ? null : String.valueOf(decimal),
            grouping == 0 || grouping == decimal ? null : String.valueOf(grouping),
            emptyToNull(l10n.getCurrencySymbol())
        };
    }

    /// `null` for a null or empty string. A port's `L10NManager` may answer
    /// either when it knows no currency symbol.
    private static String emptyToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    /// The first character of `text` that sits between two ASCII digits, or 0.
    private static char firstSeparator(String text) {
        if (text == null) {
            return 0;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                boolean between = i > 0 && i + 1 < text.length()
                        && text.charAt(i + 1) >= '0' && text.charAt(i + 1) <= '9';
                return between ? c : 0;
            }
        }
        return 0;
    }

    @Override
    public void randomBytes(byte[] out) {
        com.codename1.security.SecureRandom.fill(out);
    }

    @Override
    public Digest digest(String algorithm) {
        if (!Hash.MD5.equalsIgnoreCase(algorithm) && !Hash.SHA1.equalsIgnoreCase(algorithm)
                && !Hash.SHA224.equalsIgnoreCase(algorithm) && !Hash.SHA256.equalsIgnoreCase(algorithm)
                && !Hash.SHA384.equalsIgnoreCase(algorithm) && !Hash.SHA512.equalsIgnoreCase(algorithm)) {
            return null;
        }
        try {
            return new HashDigest(Hash.create(algorithm));
        } catch (com.codename1.security.CryptoException e) {
            // A port without this digest; the class library reports the
            // algorithm as unavailable.
            return null;
        }
    }

    /// A core `Hash` as a [JdkHost.Digest].
    private static final class HashDigest implements Digest {
        private final Hash hash;

        HashDigest(Hash hash) {
            this.hash = hash;
        }

        @Override
        public void update(byte[] data, int offset, int length) {
            hash.update(data, offset, length);
        }

        @Override
        public byte[] digest() {
            return hash.digest();
        }

        @Override
        public void reset() {
            hash.reset();
        }

        @Override
        public int length() {
            return hash.digestLength();
        }
    }
}
