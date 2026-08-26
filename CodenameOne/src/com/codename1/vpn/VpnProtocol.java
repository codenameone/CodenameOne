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
package com.codename1.vpn;

/// The tunnelling protocol a configuration uses.
///
/// The list is short because it is limited to what **both** platforms can be
/// asked to run without the app shipping its own tunnel. Anything else needs
/// [com.codename1.vpn.tunnel], which is a much larger commitment.
///
/// The ordinals cross the SPI boundary, so **existing constants must not be
/// reordered**.
public enum VpnProtocol {
    /// IKEv2, the protocol both platforms implement themselves. This is the
    /// one to use unless there is a reason not to.
    IKEV2,

    /// IPsec with a pre-shared key. Available on iOS; Android's managed
    /// profile API does not offer it, and reports
    /// [VpnError#NOT_SUPPORTED].
    IPSEC,

    /// A tunnel this app implements, through
    /// [com.codename1.vpn.tunnel].
    CUSTOM
}
