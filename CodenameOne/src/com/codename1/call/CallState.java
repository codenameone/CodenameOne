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

/// Where a call is in its life. The ports map their platform's own state
/// machine onto this one, so app code has a single set of states to reason
/// about.
///
/// The ordinals cross the SPI boundary, so **existing constants must not be
/// reordered** -- new ones go on the end.
public enum CallState {
    /// Reported to the system but not yet connected, and ringing. An
    /// incoming call starts here.
    RINGING,

    /// An outgoing call the system has accepted and is placing, but which
    /// the far end has not yet answered.
    DIALING,

    /// Connected, with audio expected to be flowing.
    ACTIVE,

    /// Connected but held, either by this app or by the system taking the
    /// audio for another call.
    HELD,

    /// Over. Terminal: a call never leaves this state, and acting on one
    /// that has reports [CallError#INVALID_ID].
    ENDED
}
