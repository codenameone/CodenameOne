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

/// API shielding: attestation tokens, over-the-air certificate pinning, and runtime
/// self-protection reporting.
///
/// Start at [com.codename1.security.shield.AppShield].
///
/// #### What this can and cannot do
///
/// Worth being precise about, because the category attracts overclaiming. On a device the attacker
/// fully controls, no client-side check is unbypassable -- detection code can be patched out and
/// headers can be stripped. What this buys you is threefold:
///
/// 1. Your backend gets a **cryptographically verifiable statement from Apple or Google** about the
///    app and device, evaluated by a service the attacker does not control. That is a categorically
///    different thing from a boolean your own app computed about itself.
/// 2. The cost of scripted abuse rises from "reproduce the API calls with a shell script" to
///    "reverse-engineer and re-sign a native binary, per release".
/// 3. Certificate pins rotate over the air, so a pin change no longer needs an app store release --
///    which is what makes pinning practical to run at all.
///
/// It does not make an app unhackable, and any product in this space that says otherwise is selling
/// something.
///
/// #### The load-bearing part is on your server
///
/// The token means nothing until your backend refuses to serve requests without a valid one. Until
/// that check exists, adding the shield changes nothing about your security.
package com.codename1.security.shield;
