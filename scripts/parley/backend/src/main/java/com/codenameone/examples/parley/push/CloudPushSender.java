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
package com.codenameone.examples.parley.push;

import com.codename1.backend.Tasks;
import com.codename1.backend.push.CloudPush;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushResult;
import com.codename1.backend.push.PushTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::parley-push-cloud[]
/// Sends through the Codename One push service, which delivers to Firebase,
/// Apple, Huawei and the browsers on the app's behalf.
///
/// The call to the service is made on a thread of its own, so the request that
/// changed a delivery answers without waiting for it, and a refusal is logged and
/// not thrown: the customer is still told over the live channel and on their next
/// look at the delivery.
public class CloudPushSender implements PushSender {
    private final CloudPush push;

    /// @param push the client for the service, holding the app's id and the
    ///     server key
    public CloudPushSender(CloudPush push) {
        this.push = push;
    }

    @Override
    public void send(List<PushTarget> targets, final PushNotification message,
            final Runnable delivered) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        final List<PushTarget> to = new ArrayList<PushTarget>(targets);
        Tasks.platform(() -> deliver(to, message, delivered));
    }

    private void deliver(List<PushTarget> to, PushNotification message, Runnable delivered) {
        boolean accepted = false;
        try {
            PushResult result = push.send(message, to);
            accepted = result.isAccepted();
            if (!accepted) {
                System.err.println("[push] the push service refused a notification: "
                        + result.getStatus() + " " + result.getError());
            }
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] a notification was not sent: " + err);
        }
        if (accepted && delivered != null) {
            try {
                delivered.run();
            } catch (RuntimeException err) {
                System.err.println("[push] a notification was sent, and what follows it failed: "
                        + err);
            }
        }
    }
}
// end::parley-push-cloud[]
