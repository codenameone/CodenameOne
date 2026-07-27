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
/// Low-level native push integration for a CN1Lib or private provider.
///
/// A custom transport owns the provider SDK and native registration flow.
/// When installed with {@link PushClient.Builder#transport(PushTransport)},
/// BuildCloud registration is disabled. The matching
/// {@link PushRegistrationSink} is responsible for synchronizing tokens with
/// the application's server.
///
/// The transport must invoke exactly one registration result callback for
/// each {@link #register(Callback)} attempt, report token rotations through
/// {@link Callback#registered(PushSubscription)}, and pass complete schema-3
/// JSON envelopes to {@link Callback#message(String)}. The callback accepts
/// calls from native threads; {@link PushClient} moves application callbacks to
/// the EDT.
public interface PushTransport {
    ///
    /// Returns the stable provider identifier stored with subscriptions.
    ///
    /// @return a non-empty identifier such as {@code company-push}
    String getId();

    ///
    /// Tests whether this transport is available on the current device.
    ///
    /// @return {@code true} when registration can be attempted
    boolean isSupported();

    ///
    /// Starts or refreshes native registration.
    ///
    /// @param callback the callback used for registration and incoming messages
    void register(Callback callback);

    ///
    /// Removes the native subscription.
    ///
    /// @param callback the callback that must receive {@link Callback#unregistered()}
    ///                 when removal completes
    void unregister(Callback callback);

    ///
    /// Receives events emitted by a custom transport.
    interface Callback {
        ///
        /// Reports initial registration or a later token rotation.
        ///
        /// @param subscription the current subscription
        void registered(PushSubscription subscription);

        ///
        /// Confirms that native unregistration completed.
        void unregistered();

        ///
        /// Delivers a complete schema-3 JSON envelope.
        ///
        /// @param envelopeJson the encoded envelope
        void message(String envelopeJson);

        ///
        /// Reports a registration or transport error.
        ///
        /// @param error the error details
        void failed(PushError error);
    }
}
