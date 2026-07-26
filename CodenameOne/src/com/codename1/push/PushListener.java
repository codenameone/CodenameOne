/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.push;

///
/// Receives registration, message, and error events from a {@link PushClient}.
///
/// <p>All methods run on the Codename One EDT. The listener is retained by the
/// client and remains active until unregistration completes. It is installed
/// before native registration starts, so a synchronous custom transport cannot
/// outrun the listener.</p>
///
/// <p>Messages received before a client is active are replayed after
/// {@link PushClient#register()}. Operating systems may delay or suppress
/// background and silent delivery; an application should refresh authoritative
/// state when it resumes instead of treating push as a durable data channel.</p>
public interface PushListener {
    ///
    /// Called when the native provider creates or rotates a subscription.
    ///
    /// <p>This method may run more than once during the life of an installation.
    /// Replace any previously stored token rather than assuming it is permanent.</p>
    ///
    /// @param subscription the current subscription
    void onRegistration(PushSubscription subscription);

    ///
    /// Called for a parsed schema-3 message.
    ///
    /// <p>For a visible background notification, this normally runs when the
    /// user opens the notification. Foreground and silent delivery varies by
    /// platform and OS state.</p>
    ///
    /// @param message the immutable message envelope
    void onMessage(PushMessage message);

    ///
    /// Called when registration or envelope processing fails.
    ///
    /// @param error a stable error code, diagnostic message, and retry hint
    void onError(PushError error);
}
