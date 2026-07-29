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

import java.io.IOException;

/// Raised when a shield operation cannot produce a usable token, or when a request to a protected
/// host is refused because its certificate chain matched no configured pin.
///
/// Extends `IOException` so it flows through the normal `ConnectionRequest` error path rather than
/// needing its own handling. Always check [#getStatus()] before deciding what to show the user:
/// [ShieldStatus#isTransient()] distinguishes "could not reach the service" from "this device was
/// rejected", and those deserve very different UX.
public class ShieldException extends IOException {

    /// The status identifier rather than the [ShieldStatus] itself.
    ///
    /// `IOException` is serializable, and holding a non-serializable field on a
    /// serializable class is both a static-analysis error and a latent null after a
    /// round trip -- which would break [#getStatus()]'s never-null contract at exactly
    /// the moment someone is trying to work out why a request failed. A `String`
    /// survives serialization, and [ShieldStatus#forId(String)] resolves it back to the
    /// canonical constant, so identity comparisons still hold.
    private final String statusId;

    public ShieldException(ShieldStatus status, String message) {
        super(message);
        this.statusId = (status == null ? ShieldStatus.NOT_INITIALIZED : status).getId();
    }

    /// Why the operation failed. Never null.
    public ShieldStatus getStatus() {
        return ShieldStatus.forId(statusId);
    }
}
