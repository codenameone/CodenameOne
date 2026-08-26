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
package com.codename1.call;

/// Why a call ended. This is what the system writes into the call log, so
/// the value chosen is user-visible: a call reported as
/// [#REMOTE_ENDED] shows differently from one reported as [#UNANSWERED].
///
/// The ordinals cross the SPI boundary, so **existing constants must not be
/// reordered**.
public enum CallEndReason {
    /// The far end hung up.
    REMOTE_ENDED,

    /// This side hung up.
    LOCAL_ENDED,

    /// It rang and nobody picked up.
    UNANSWERED,

    /// The far end was busy.
    BUSY,

    /// The call could not be set up -- no network, signalling failure, or a
    /// far end that never answered the invitation.
    FAILED,

    /// The system, not the user, declined it: Do Not Disturb, or a number
    /// the user has blocked. Distinct from [#UNANSWERED] because the call
    /// never rang.
    FILTERED
}
