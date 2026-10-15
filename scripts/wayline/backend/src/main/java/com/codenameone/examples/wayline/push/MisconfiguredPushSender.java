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
package com.codenameone.examples.wayline.push;

import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;

import java.util.List;

/// What the server sends with when the push settings are incomplete: nothing
/// goes out, and the log says why.
///
/// The problem is logged once when the server starts and again on every send,
/// naming the settings to fix. Unlike a text message, a notification that does
/// not go out costs nobody a sign-in, so it is not an error to the caller.
public class MisconfiguredPushSender implements PushSender {
    private final String problem;

    /// @param problem what is wrong, naming the settings to fix
    public MisconfiguredPushSender(String problem) {
        this.problem = problem;
        System.err.println("[config] " + problem);
    }

    @Override
    public void send(List<PushTarget> targets, PushNotification message) {
        if (targets != null && !targets.isEmpty()) {
            System.err.println("[config] a push notification was not sent: " + problem);
        }
    }
}
