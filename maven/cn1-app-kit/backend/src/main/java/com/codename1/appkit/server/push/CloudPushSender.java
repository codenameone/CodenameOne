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

import com.codename1.backend.Tasks;
import com.codename1.backend.push.CloudPush;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushResult;
import com.codename1.backend.push.PushTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::appkit-push-cloud[]
/// Sends through the Codename One push service, which delivers to Firebase,
/// Apple, Huawei and the browsers on the app's behalf.
///
/// The call to the service is made on a thread of its own, so the request that
/// changed something answers without waiting for it, and a refusal is logged and
/// not thrown: the user still learns of the change the next time the app
/// reads it.
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
// end::appkit-push-cloud[]
