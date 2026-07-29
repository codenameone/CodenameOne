/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.shield;

/// Outcome of a shield operation.
///
/// The single most important distinction in this class is between *"I could not reach the
/// attestation service"* ([#NO_NETWORK], [#POOR_NETWORK], [#SERVICE_DOWN], [#RATE_LIMITED]) and
/// *"the attestation service looked at this device and said no"* ([#REJECTED]). An app should
/// almost always treat the first group as a transient condition to retry through, and only the
/// second as evidence that something is actually wrong with the device it is running on.
/// Collapsing the two into a single "attestation failed" boolean is the most common way to build
/// an app that either locks out users on a train or trusts a rooted phone.
///
/// This is a class of constants rather than an enum because the vocabulary is wire-visible: the
/// attestation engine may report a status that this build of the framework predates, and
/// [#getId()] round-trips it rather than failing to resolve.
public final class ShieldStatus {

    /// The operation succeeded and any token returned is usable.
    public static final ShieldStatus OK = new ShieldStatus("ok", true);

    /// The app was built without the enterprise attestation engine. Everything degrades to a
    /// no-op: no token is issued, no pin is enforced, and no request is blocked.
    public static final ShieldStatus UNPROTECTED = new ShieldStatus("unprotected", false);

    /// [AppShield#init(ShieldConfig)] has not been called yet.
    public static final ShieldStatus NOT_INITIALIZED = new ShieldStatus("notInitialized", false);

    /// The device has no connectivity. Transient.
    public static final ShieldStatus NO_NETWORK = new ShieldStatus("noNetwork", false);

    /// The request timed out or DNS failed. Transient.
    public static final ShieldStatus POOR_NETWORK = new ShieldStatus("poorNetwork", false);

    /// The attestation service answered with a server error. Transient.
    public static final ShieldStatus SERVICE_DOWN = new ShieldStatus("serviceUnavailable", false);

    /// This device is asking too often and is being throttled. Transient, but back off before
    /// retrying rather than looping.
    public static final ShieldStatus RATE_LIMITED = new ShieldStatus("rateLimited", false);

    /// The service evaluated this device and declined to issue a token. **Not** transient: the
    /// device itself is what failed the policy. Retrying will not help.
    public static final ShieldStatus REJECTED = new ShieldStatus("rejected", false);

    /// The certificate chain presented by a protected host matched no configured pin. The request
    /// was refused before any request body was sent.
    public static final ShieldStatus PIN_MISMATCH = new ShieldStatus("pinMismatch", false);

    private static final ShieldStatus[] KNOWN = {
        OK, UNPROTECTED, NOT_INITIALIZED, NO_NETWORK, POOR_NETWORK,
        SERVICE_DOWN, RATE_LIMITED, REJECTED, PIN_MISMATCH
    };

    private final String id;
    private final boolean success;

    private ShieldStatus(String id, boolean success) {
        this.id = id;
        this.success = success;
    }

    /// The stable wire identifier, e.g. `rateLimited`.
    public String getId() {
        return id;
    }

    /// True only for [#OK]. Every other status means no usable token was produced.
    public boolean isSuccess() {
        return success;
    }

    /// True when the failure is about reaching the service rather than about this device. Retrying
    /// later may succeed. False for [#REJECTED] and [#PIN_MISMATCH], which describe the device and
    /// the connection respectively.
    public boolean isTransient() {
        return this == NO_NETWORK || this == POOR_NETWORK
                || this == SERVICE_DOWN || this == RATE_LIMITED;
    }

    /// Resolves a wire identifier to a constant, or synthesises a non-success status for an
    /// identifier this build does not know about. Never returns null.
    public static ShieldStatus forId(String id) {
        if (id == null) {
            return NOT_INITIALIZED;
        }
        for (ShieldStatus known : KNOWN) {
            if (known.id.equals(id)) {
                return known;
            }
        }
        return new ShieldStatus(id, false);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShieldStatus)) {
            return false;
        }
        return id.equals(((ShieldStatus) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
