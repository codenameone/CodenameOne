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
package com.codenameone.examples.wayline;

import com.codename1.system.Lifecycle;
import com.codenameone.examples.wayline.net.Account;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.pay.CardForm;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Look;
import com.codenameone.examples.wayline.ui.Nav;

/// The app. It opens on whatever the stored session allows: the mode the user
/// was last in when they are still signed in, the welcome screen when not.
///
/// Where things are:
///
/// - `net` -- the session, and the typed clients generated from the contracts in
///   the `shared` module. Nothing else in the app builds a URL.
/// - `live` -- the WebSocket the server pushes changes down.
/// - `ui`, `rider`, `driver`, `admin` -- the screens.
/// - `map` -- the map, its markers and the device's position.
/// - `src/main/css/theme.css` -- every colour, font and margin.
/// - `src/main/l10n` -- every word, in each language the app speaks.
/// - `Prefs` -- what the user chose in Settings.
/// - `Telemetry` -- what is reported about how the app is used, once the user
///   has agreed to it.
public class Wayline extends Lifecycle {
    @Override
    public void runApp() {
        // Before the first screen is built: a screen keeps the look and the
        // words it was built with.
        Look.apply();
        Lang.apply();
        Telemetry.start();
        Session.start(AppConfig.serverUrl());
        Account.start();
        Nav.launch();
    }

    /// Coming back to the app is how a card added on the payment provider's
    /// page is noticed: the browser it opened in tells the app nothing.
    @Override
    public void start() {
        super.start();
        CardForm.resumed();
    }

    /// The app is put away, which is the last sure moment to send the usage
    /// reports gathered since the last batch.
    @Override
    public void stop() {
        super.stop();
        Telemetry.flush();
    }
}
