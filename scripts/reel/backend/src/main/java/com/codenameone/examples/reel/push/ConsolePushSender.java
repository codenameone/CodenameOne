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
package com.codenameone.examples.reel.push;

import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;

import java.util.List;

/// Prints each notification on the server's console instead of sending it.
///
/// For development only: [com.codenameone.examples.reel.Services] hands it
/// out on a development profile alone.
public class ConsolePushSender implements PushSender {
    @Override
    public void send(List<PushTarget> targets, PushNotification message, Runnable delivered) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        System.out.println("[push] to " + targets.size() + " device(s): "
                + message.toJson());
        // Printed is as delivered as this sender gets.
        if (delivered != null) {
            delivered.run();
        }
    }
}
