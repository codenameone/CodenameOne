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

import com.codename1.backend.Tasks;
import com.codename1.backend.push.CloudPush;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushResult;
import com.codename1.backend.push.PushTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::wayline-push-cloud[]
/// Sends through the Codename One push service, which delivers to Firebase,
/// Apple, Huawei and the browsers on the app's behalf.
///
/// The call to the service is made on a thread of its own, so the request that
/// changed a ride answers without waiting for it, and a refusal is logged and
/// not thrown: the rider is still told over the live channel and on their next
/// look at the ride.
public class CloudPushSender implements PushSender {
    private final CloudPush push;

    /// @param push the client for the service, holding the app's id and the
    ///     server key
    public CloudPushSender(CloudPush push) {
        this.push = push;
    }

    @Override
    public void send(List<PushTarget> targets, final PushNotification message) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        final List<PushTarget> to = new ArrayList<PushTarget>(targets);
        Tasks.platform(() -> deliver(to, message));
    }

    private void deliver(List<PushTarget> to, PushNotification message) {
        try {
            PushResult result = push.send(message, to);
            if (!result.isAccepted()) {
                System.err.println("[push] the push service refused a notification: "
                        + result.getStatus() + " " + result.getError());
            }
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] a notification was not sent: " + err);
        }
    }
}
// end::wayline-push-cloud[]
