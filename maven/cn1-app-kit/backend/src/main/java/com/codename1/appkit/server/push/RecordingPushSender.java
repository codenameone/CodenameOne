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
package com.codename1.appkit.server.push;

import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Keeps every notification instead of sending it, for the tests to read.
///
/// What the `test` profile gets: a test signs a phone in, changes something, and
/// asks this what each device was sent. Refused on any profile that is not a
/// development one.
public class RecordingPushSender implements PushSender {
    private final List<Sent> sent = new ArrayList<Sent>();
    private int refusals;
    private int unconfirmed;

    /// One notification as one device was sent it.
    public static final class Sent {
        private final String provider;
        private final String token;
        private final Map<String, Object> message;

        Sent(String provider, String token, Map<String, Object> message) {
            this.provider = provider;
            this.token = token;
            this.message = message;
        }

        public String provider() {
            return provider;
        }

        public String token() {
            return token;
        }

        /// The notification as it would go on the wire: `title`, `body`,
        /// `data`, `surface` and the rest.
        public Map<String, Object> message() {
            return message;
        }
    }

    @Override
    public void send(List<PushTarget> targets, PushNotification message, Runnable delivered) {
        // Outside the lock: what follows a delivery writes to the database.
        if (record(targets, message) && delivered != null) {
            delivered.run();
        }
    }

    /// Records a send, and answers whether the push service would confirm it.
    private synchronized boolean record(List<PushTarget> targets, PushNotification message) {
        if (refusals > 0) {
            refusals--;
            throw new IllegalStateException("The push provider is unavailable");
        }
        if (targets != null) {
            for (PushTarget target : targets) {
                sent.add(new Sent(target.getProvider(), target.getToken(), message.toMap()));
            }
        }
        if (unconfirmed > 0) {
            unconfirmed--;
            return false;
        }
        return true;
    }

    /// What the device with `token` was sent, oldest first.
    public synchronized List<Sent> sentTo(String token) {
        List<Sent> out = new ArrayList<Sent>();
        for (Sent each : sent) {
            if (each.token.equals(token)) {
                out.add(each);
            }
        }
        return out;
    }

    /// Everything sent so far, oldest first.
    public synchronized List<Sent> all() {
        return new ArrayList<Sent>(sent);
    }

    public synchronized void clear() {
        sent.clear();
        refusals = 0;
        unconfirmed = 0;
    }

    /// Makes the next `count` sends go out and never be confirmed, as a
    /// service that refuses them later, on its own thread, does.
    public synchronized void leaveUnconfirmed(int count) {
        unconfirmed = count;
    }

    /// Makes the next `count` sends throw, as a provider that is down does.
    public synchronized void refuse(int count) {
        refusals = count;
    }
}
