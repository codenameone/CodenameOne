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
package com.codename1.impl.android;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Template;

/**
 * An androidx.car.app {@link Screen} that proxies a single portable
 * {@link com.codename1.car.CarScreen}. Its template is produced on demand by
 * {@link CN1AndroidAutoBridge}. The root proxy starts with no CarScreen (renders a loading state)
 * until the com.codename1.car session supplies the root screen; deeper screens wrap their CarScreen
 * directly. Injected by the Codename One build.
 */
final class CN1CarScreen extends Screen {
    /** The portable screen this proxy renders; null on the root proxy until the session starts. */
    com.codename1.car.CarScreen cn1Screen;
    /** True for the root proxy (uses the app-icon header action instead of a back action). */
    final boolean root;
    private final CN1AndroidAutoBridge bridge;

    CN1CarScreen(CarContext carContext, CN1AndroidAutoBridge bridge,
                 com.codename1.car.CarScreen cn1Screen, boolean root) {
        super(carContext);
        this.bridge = bridge;
        this.cn1Screen = cn1Screen;
        this.root = root;
    }

    @Override
    public Template onGetTemplate() {
        return bridge.buildTemplate(cn1Screen, root);
    }
}
