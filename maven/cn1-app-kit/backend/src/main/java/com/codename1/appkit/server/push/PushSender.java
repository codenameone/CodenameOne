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

import java.util.List;

// tag::appkit-push-sender[]
/// Sends push notifications to devices.
///
/// The application chooses the implementation, normally from a property, when
/// it builds its beans. A notification is a courtesy on top of what the
/// app reads from the server, so sending one never fails the change it is about:
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
// end::appkit-push-sender[]
