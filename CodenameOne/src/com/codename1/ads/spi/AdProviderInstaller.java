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
package com.codename1.ads.spi;

import com.codename1.system.NativeInterface;

/// The discovery hook that lets an ad cn1lib auto-register its [AdProvider]
/// with zero application wiring.
///
/// Codename One has no on-device service loader and forbids `Class.forName`
/// outside [com.codename1.system.NativeLookup], so this rides the sanctioned
/// name based lookup: a plugin ships a class named `AdProviderInstallerImpl`
/// (in the package `com.codename1.ads.spi`) implementing this interface, and
/// [com.codename1.ads.AdManager] resolves it the first time it needs a
/// provider. The implementation's [#install()] method simply calls
/// [com.codename1.ads.AdManager#registerProvider(AdProvider)].
///
/// Despite extending [NativeInterface] the installer is plain Java that lives
/// in a cn1lib's common module; extending the marker interface is only what
/// makes it discoverable through `NativeLookup`.
///
/// @author Shai Almog
public interface AdProviderInstaller extends NativeInterface {
    /// Registers the plugin's provider with [com.codename1.ads.AdManager].
    void install();
}
