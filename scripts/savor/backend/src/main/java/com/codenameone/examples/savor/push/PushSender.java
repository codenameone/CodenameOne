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
package com.codenameone.examples.savor.push;

import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;

import java.util.List;

// tag::savor-push-sender[]
/// Sends push notifications to devices.
///
/// [com.codenameone.examples.savor.Services] chooses the implementation from
/// `savor.push.provider`. A notification is a courtesy on top of what the
/// app reads from the server, so sending one never fails the delivery it is about:
/// an implementation logs what went wrong and returns.
public interface PushSender {
    /// Sends `message` to every one of `targets`. Returns at once; the sending
    /// itself may happen later, on another thread.
    ///
    /// @param delivered run once the push service has taken the message, on
    ///     whatever thread learnt it; never when it was refused or could not be
    ///     sent. Null when nobody needs to know.
    void send(List<PushTarget> targets, PushNotification message, Runnable delivered);
}
// end::savor-push-sender[]
