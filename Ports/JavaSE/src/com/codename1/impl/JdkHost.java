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

/// The few services the pure-logic part of the Java class library needs from
/// the platform it runs on.
///
/// Internal API. The `java.*` classes of the ParparVM runtime (`vm/JavaAPI`)
/// are translated alone for the translator itself, the backend and the VM
/// tests, so they cannot name Codename One. Where a kept class cannot do its
/// job in plain Java it calls this class, and a Codename One application
/// installs `com.codename1.impl.CoreJdkHost` from its port's `init`. Anything
/// else gets the defaults below, which touch no native method.
///
/// The hook is deliberately narrow. Every member exists because a class the
/// library ships cannot work correctly without it:
///
/// - [#isUiThread] and [#blockUi]: `Future.get`, `CountDownLatch.await`,
///   `CompletableFuture.join` and the executors' waits block. On the event
///   dispatch thread that would freeze the UI, so they block through
///   `invokeAndBlock` instead, which only the platform can reach.
/// - [#log]: an executor or a delayed task that throws has nobody to report
///   to; the platform's log is where such a failure belongs.
/// - [#randomBytes]: `SecureRandom` and `UUID.randomUUID` need entropy that a
///   pure-Java generator cannot supply.
/// - [#digest]: `MessageDigest` and name-based `UUID`s use the platform's
///   digests rather than shipping a second implementation of each.
/// - [#localeNumberSymbols]: `DecimalFormatSymbols` reports the device
///   locale's separators, which only the platform knows.
///
/// On Java SE and Android the real JDK runs and nothing calls this class.
/// Copies of it ship in those ports only so that `CoreJdkHost`, which is part
/// of the core sources, always has its superclass.
///
/// The file is kept byte for byte identical in `vm/JavaAPI`, `Ports/CLDC11`,
/// `Ports/JavaSE` and `Ports/Android`. It may use only API all four class
/// libraries have.
public abstract class JdkHost {
    /// [#log] level of a debug message.
    public static final int LOG_DEBUG = 1;
    /// [#log] level of an informational message.
    public static final int LOG_INFO = 2;
    /// [#log] level of a warning.
    public static final int LOG_WARNING = 3;
    /// [#log] level of an error.
    public static final int LOG_ERROR = 4;

    private static final JdkHost DEFAULTS = new Defaults();
    private static JdkHost current;

    /// Subclasses are installed with [#install].
    protected JdkHost() {
    }

    /// The installed host, or the plain Java defaults when none was installed.
    public static JdkHost get() {
        JdkHost h = current;
        return h == null ? DEFAULTS : h;
    }

    /// Makes `host` the one [#get] answers; null restores the defaults.
    /// Called once, while the program starts and before it starts a thread
    /// of its own.
    public static void install(JdkHost host) {
        current = host;
    }

    /// Logs `message` and `thrown`, either of which may be null, at `level`
    /// ([#LOG_DEBUG] to [#LOG_ERROR]).
    public void log(int level, String message, Throwable thrown) {
        if (message != null) {
            System.err.println(message);
        }
        if (thrown != null) {
            thrown.printStackTrace();
        }
    }

    /// Whether the caller is the UI thread, which must not block.
    public boolean isUiThread() {
        return false;
    }

    /// Runs `task`, which blocks, where blocking is allowed: off the UI
    /// thread while that thread keeps serving events, or right here.
    public void blockUi(Runnable task) {
        task.run();
    }

    /// The decimal separator, grouping separator and currency symbol of the
    /// device's locale, any of them null when unknown; or null when the
    /// platform cannot tell.
    public String[] localeNumberSymbols() {
        return null;
    }

    /// Fills `out` with cryptographically secure random bytes.
    public void randomBytes(byte[] out) {
        throw new UnsupportedOperationException("No secure random source");
    }

    /// A new digest for `algorithm` (`MD5`, `SHA-1`, `SHA-256` and the like),
    /// or null when the platform has none.
    public Digest digest(String algorithm) {
        return null;
    }

    /// A running message digest, as [#digest] makes it.
    public interface Digest {
        /// Feeds `length` bytes of `data` from `offset`.
        void update(byte[] data, int offset, int length);

        /// Finishes the digest and resets it.
        byte[] digest();

        /// Discards what was fed.
        void reset();

        /// The length of the digest in bytes.
        int length();
    }

    /// The host [#get] answers when none was installed.
    private static final class Defaults extends JdkHost {
    }
}
