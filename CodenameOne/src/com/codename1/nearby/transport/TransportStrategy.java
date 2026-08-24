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
package com.codename1.nearby.transport;

/// The connection topology a transport session uses. The platforms trade
/// bandwidth against the number of simultaneous links, and this is where an
/// app says which side of that trade it wants.
public enum TransportStrategy {
    /// Many-to-many: every device may connect to every other. The most
    /// flexible and the slowest per link. Android
    /// `Strategy.P2P_CLUSTER`; the natural fit for MultipeerConnectivity,
    /// which is a mesh by nature.
    CLUSTER,

    /// One advertiser, many discoverers. The advertiser accepts several
    /// connections and each discoverer holds exactly one. Android
    /// `Strategy.P2P_STAR`.
    STAR,

    /// Exactly one connection on each side, and the highest bandwidth of
    /// the three. Android `Strategy.P2P_POINT_TO_POINT`.
    POINT_TO_POINT
}
