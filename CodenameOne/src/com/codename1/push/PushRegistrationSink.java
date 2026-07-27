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
/// Mirrors native subscription changes to an application-owned server.
///
/// This interface is optional with managed BuildCloud push. It is mandatory
/// when a {@link PushTransport} is supplied, because custom transport mode never
/// sends registration data to BuildCloud. Implementations should upsert by
/// installation ID and replace rotated tokens.
///
/// Callbacks run on the Codename One EDT. Network work should be queued
/// asynchronously rather than blocking the callback.
public interface PushRegistrationSink {
    ///
    /// Stores or replaces a subscription on the application-owned server.
    ///
    /// @param subscription the current native subscription
    void registered(PushSubscription subscription);

    ///
    /// Removes a subscription after the transport unregisters.
    ///
    /// @param subscription the subscription that was active before removal
    void unregistered(PushSubscription subscription);
}
