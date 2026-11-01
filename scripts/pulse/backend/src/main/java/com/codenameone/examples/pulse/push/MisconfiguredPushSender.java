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
package com.codenameone.examples.pulse.push;

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
    public void send(List<PushTarget> targets, PushNotification message, Runnable delivered) {
        if (targets != null && !targets.isEmpty()) {
            System.err.println("[config] a push notification was not sent: " + problem);
        }
    }
}
