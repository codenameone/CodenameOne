/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley;

import com.codename1.system.Lifecycle;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.net.Account;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.notify.Notifications;
import com.codenameone.examples.parley.ui.Lang;
import com.codenameone.examples.parley.ui.Look;
import com.codenameone.examples.parley.ui.Nav;
import com.codenameone.examples.parley.ui.Privacy;

/// The app. It opens on whatever the stored session allows: the chats when
/// the user is still signed in, the welcome screen when not.
///
/// Where things are:
///
/// - `crypto` -- the end-to-end protocol: device keys, X3DH, the Double
///   Ratchet, safety numbers, and the encrypted vault everything local lives in.
/// - `chat` -- sending and receiving: the per-device fan-out, sync, receipts,
///   typing, attachments and calls.
/// - `store` -- the conversations and their history on this device.
/// - `net` -- the session, and the typed clients generated from the contracts
///   in the `shared` module. Nothing else in the app builds a URL.
/// - `live` -- the WebSocket the server pushes envelopes and signalling down.
/// - `notify` -- push notifications: content-free wake-ups.
/// - `ui`, `admin` -- the screens.
/// - `src/main/css/theme.css` -- every colour, font and margin.
/// - `src/main/l10n` -- every word, in each language the app speaks.
/// - `Prefs` -- what the user chose in Settings that stays on this device.
/// - `Telemetry` -- what is reported about how the app is used, once the user
///   has agreed to it. Never what anybody wrote.
public class Parley extends Lifecycle {
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

    /// Coming back to the app: the push token is asked for again (the
    /// platform answers at once when nothing changed), anything that arrived
    /// while away is fetched, and the app lock asks again.
    @Override
    public void start() {
        super.start();
        Notifications.start();
        Privacy.resumed();
        Messenger.sync();
    }

    /// The app is put away, which is the last sure moment to send the usage
    /// reports gathered since the last batch, and when the screen is covered
    /// if the user asked for that.
    @Override
    public void stop() {
        super.stop();
        Telemetry.flush();
        Privacy.paused();
    }
}
