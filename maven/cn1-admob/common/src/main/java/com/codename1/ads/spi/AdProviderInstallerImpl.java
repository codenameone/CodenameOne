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

import com.codename1.ads.AdManager;
import com.codename1.ads.admob.AdMobProvider;

/// Auto-registers the [AdMobProvider] with [AdManager]. This class is discovered
/// by [com.codename1.system.NativeLookup] using the standard `*Impl` naming
/// convention applied to [AdProviderInstaller], so simply having the cn1-admob
/// library on the classpath wires up AdMob with no application code.
///
/// Despite the package and `NativeInterface` lineage this is plain cross
/// platform Java; it carries no native implementation of its own.
///
/// @author Shai Almog
public class AdProviderInstallerImpl implements AdProviderInstaller {
    @Override
    public void install() {
        AdManager.registerProvider(new AdMobProvider());
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
