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

/// A network path one side found to itself, to be tried by the other side.
/// A peer connection gathers these as it goes and hands each to
/// [PeerConnection.Listener#iceCandidate(PeerConnection, IceCandidate)]; the
/// app sends it across and the other side passes it to
/// [PeerConnection#addIceCandidate(IceCandidate)].
public final class IceCandidate {
    private final String sdpMid;
    private final int sdpMLineIndex;
    private final String candidate;

    public IceCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("An ICE candidate needs its candidate line");
        }
        this.sdpMid = sdpMid == null ? "" : sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
        this.candidate = candidate;
    }

    /// The media section this candidate belongs to, by its `a=mid` value.
    public String getSdpMid() {
        return sdpMid;
    }

    /// The media section this candidate belongs to, by its position.
    public int getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    /// The `candidate:` line itself.
    public String getCandidate() {
        return candidate;
    }
}
