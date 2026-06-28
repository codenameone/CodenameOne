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
package com.codename1.car;

import java.util.ArrayList;
import java.util.List;

/// The system now-playing surface for audio apps. On both platforms the track metadata, artwork,
/// transport controls and scrubber are driven by the platform media session -- on Android by the
/// `MediaSession` behind Codename One's background audio service, on iOS by `MPNowPlayingInfoCenter`
/// / the remote command center -- so this template carries no metadata of its own. It exists to (a)
/// route the head unit to the now-playing screen and (b) add up-to-two app-specific buttons
/// (shuffle, repeat, like, rate). Maps to `CPNowPlayingTemplate` and the Android Auto now-playing
/// view.
public class CarNowPlayingTemplate extends CarTemplate {
    private boolean upNextVisible;
    private final List<CarAction> actions = new ArrayList<CarAction>();

    /// Adds an app-specific now-playing button (e.g. shuffle / repeat / like). The head unit shows a
    /// small fixed number of these alongside the system transport controls.
    ///
    /// #### Parameters
    ///
    /// - `action`: the button to add
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNowPlayingTemplate addAction(CarAction action) {
        if (action != null) {
            actions.add(action);
        }
        return this;
    }

    /// Returns the app-specific now-playing buttons, in order.
    public List<CarAction> getActions() {
        return actions;
    }

    /// Returns true when an "Up next" affordance should be offered.
    public boolean isUpNextVisible() {
        return upNextVisible;
    }

    /// Sets whether the head unit offers an "Up next" button that lets the driver browse the queue.
    ///
    /// #### Parameters
    ///
    /// - `upNextVisible`: true to show the up-next affordance
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNowPlayingTemplate setUpNextVisible(boolean upNextVisible) {
        this.upNextVisible = upNextVisible;
        return this;
    }
}
