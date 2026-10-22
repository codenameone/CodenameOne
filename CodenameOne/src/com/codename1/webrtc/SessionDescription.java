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
package com.codename1.webrtc;

/// One side's description of a session -- the codecs, the media and the
/// transport keys -- in SDP. An offer from the side that calls, an answer
/// from the side that picks up.
///
/// The app carries it to the other side by whatever signalling it has; the
/// API never sends it anywhere. Its content is opaque to the app.
public final class SessionDescription {
    /// The type of the caller's description.
    public static final String OFFER = "offer";

    /// The type of the callee's description.
    public static final String ANSWER = "answer";

    private final String type;
    private final String sdp;

    /// #### Parameters
    ///
    /// - `type`: [#OFFER] or [#ANSWER]
    ///
    /// - `sdp`: the description, exactly as the other side produced it
    public SessionDescription(String type, String sdp) {
        if (!OFFER.equals(type) && !ANSWER.equals(type)) {
            throw new IllegalArgumentException("Unknown session description type: " + type);
        }
        if (sdp == null) {
            throw new IllegalArgumentException("A session description needs its SDP");
        }
        this.type = type;
        this.sdp = sdp;
    }

    public String getType() {
        return type;
    }

    public String getSdp() {
        return sdp;
    }
}
