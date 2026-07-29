/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.shield.spi;

/// Where the attestation engine registers itself.
///
/// Registration is by direct instance, not by class name. Codename One obfuscates and renames
/// classes -- ProGuard/R8 on Android, the bytecode-to-C translation on iOS -- so a
/// `Class.forName` lookup is unreliable by construction. The port or the build-server-generated
/// bootstrap instantiates the engine itself and passes the instance here, which survives renaming
/// because it is an ordinary symbol reference. The same convention is used elsewhere in the
/// framework for port-supplied implementations.
///
/// Registration happens in one of three places:
///
/// - **Device builds**: the build server splices a bootstrap into the generated application stub,
///   ahead of `Display.init`, when the project is entitled to the enterprise engine.
/// - **Simulator**: the desktop port's post-init bootstrap scan picks it up, which is the one
///   place a name-based lookup is safe because the desktop port is not obfuscated.
/// - **Tests**: call [#setEngine(ShieldEngine)] directly.
///
/// The first registration wins and the registry then seals. Without that, any code running later
/// in the process -- including code an attacker injected -- could swap in an engine that returns
/// whatever it likes. Sealing does not make the app tamper-proof (an attacker who can patch the
/// binary can patch this too); it removes the version of the attack that needs no patching at all.
public final class ShieldEngineRegistry {

    private static ShieldEngine engine;
    private static boolean sealed;

    private ShieldEngineRegistry() {
    }

    /// Registers the engine. The first call wins.
    ///
    /// @throws IllegalStateException if an engine is already registered
    public static void setEngine(ShieldEngine e) {
        if (e == null) {
            throw new IllegalArgumentException("engine is null");
        }
        synchronized (ShieldEngineRegistry.class) {
            if (sealed) {
                throw new IllegalStateException(
                        "A shield engine is already registered: " + engine.getName());
            }
            engine = e;
            sealed = true;
        }
    }

    /// The registered engine, or the inert default when none was registered. Never null, so no
    /// caller needs a null check and no code path can silently skip a check that should have run.
    public static ShieldEngine getEngine() {
        synchronized (ShieldEngineRegistry.class) {
            return engine != null ? engine : UnprotectedEngine.INSTANCE;
        }
    }

    /// The framework-backed [EngineContext] an engine is initialized with. Exposed so a port or a
    /// test can construct an engine against the real services without reimplementing them.
    public static EngineContext getDefaultContext() {
        return DefaultEngineContext.INSTANCE;
    }

    /// True when a real engine was registered.
    public static boolean isEngineRegistered() {
        synchronized (ShieldEngineRegistry.class) {
            return engine != null;
        }
    }

    /// Test hook: drops the registration and unseals. Not for application use -- there is no
    /// legitimate reason for a shipping app to replace its engine at runtime.
    static void resetForTesting() {
        synchronized (ShieldEngineRegistry.class) {
            engine = null;
            sealed = false;
        }
    }
}
