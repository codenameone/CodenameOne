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
package com.codename1.wearable;

/// Notified when the peer app sends a live message.
///
/// Callbacks arrive on the EDT. A message that arrived while your app was starting -- including the
/// one that caused the platform to launch it -- is replayed to the first listener you register, so
/// register from your app's `init()` rather than from a form.
public interface WearableMessageListener {

    /// Called when a message arrives from the peer app.
    ///
    /// If the sender asked for a reply, answer it by returning a message; returning null sends an
    /// empty reply. The sender is blocked waiting, so answer quickly and do slow work afterwards.
    ///
    /// #### Parameters
    ///
    /// - `message`: the received payload, addressed to the path the sender chose
    /// - `expectsReply`: true when the sender is waiting for an answer
    ///
    /// #### Returns
    ///
    /// the reply to send back, or null for none
    WearableMessage messageReceived(WearableMessage message, boolean expectsReply);
}
