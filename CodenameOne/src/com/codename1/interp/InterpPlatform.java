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
package com.codename1.interp;

/// Where a port publishes the linker that binds interpreted code to the app.
///
/// The linker is the one part of the device runtime that cannot be shared:
/// reflection on Android and in the simulator, the translator's invoke thunks
/// on iOS. The object factory is not here because it is per-program rather than
/// per-platform -- it holds the runtime that a given pushed bundle is running
/// under.
///
/// A registry rather than a lookup by name, because the obvious alternative
/// (`Class.forName` on a per-platform class) is exactly what ParparVM cannot
/// do. The port registers itself while it is initialising, which is code the
/// translator has already proven reachable.
///
/// A build with no device runtime registers nothing, so the whole feature
/// reduces to one null field.
///
/// @author Shai Almog
public final class InterpPlatform {
    private static InterpLinker linker;

    private InterpPlatform() {
    }

    /// Registers this platform's linker. Called by the port during startup.
    public static void register(InterpLinker platformLinker) {
        linker = platformLinker;
    }

    /// The registered linker, or null when this build has no device runtime.
    public static InterpLinker getLinker() {
        return linker;
    }

    /// Whether this build can run pushed code.
    public static boolean isAvailable() {
        return linker != null;
    }
}
