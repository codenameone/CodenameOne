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
package com.codename1.nearby.ranging;

/// The handle one device publishes so another can range against it.
///
/// A token is **opaque and platform-specific**. On iOS it wraps an archived
/// `NIDiscoveryToken`; on Android it carries the controller's UWB address,
/// complex channel, session id and session key. There is no cross-platform
/// UWB ranging on either OS, so a token is never portable between them --
/// [#fromByteArray] rejects a token minted by a different platform with a
/// clear failure rather than handing garbage to a native call.
///
/// What the token *is* for is the out-of-band exchange both platforms
/// require: prepare a session, publish [#toByteArray()] over some other
/// channel the two devices already share -- a GATT characteristic from
/// `com.codename1.bluetooth` is the usual one -- and feed what comes back
/// into [RangingSession#start].
///
/// ```java
/// byte[] mine = session.getLocalToken().toByteArray();
/// characteristic.writeValue(mine);
/// // ... later, when the peer's token arrives ...
/// session.start(RangingToken.fromByteArray(theirs));
/// ```
public final class RangingToken {

    /// Token minted by Apple's Nearby Interaction, wrapping an archived
    /// `NIDiscoveryToken`.
    public static final int PLATFORM_APPLE_NI = 1;

    /// Token minted by the Android UWB stack, carrying address, channel,
    /// session id and key.
    public static final int PLATFORM_ANDROID_UWB = 2;

    /// Token minted by the simulated implementation used on the desktop
    /// ports, the simulator and the JavaScript port.
    public static final int PLATFORM_SIMULATED = 3;

    private static final byte[] MAGIC = {'C', 'N', '1', 'R'};
    private static final int VERSION = 1;

    private final int platform;
    private final byte[] payload;

    private RangingToken(int platform, byte[] payload) {
        this.platform = platform;
        this.payload = payload;
    }

    /// Builds an Android-shaped token from parameters a third-party UWB
    /// accessory reported out of band.
    ///
    /// Android has no equivalent of Apple's Nearby Interaction Accessory
    /// Protocol, so an accessory there simply tells the phone which channel
    /// and session to join and the phone joins it -- that is what this
    /// builds. On iOS, use [RangingSession#startAccessory] with the
    /// accessory's configuration data instead; a token built here is
    /// rejected there.
    ///
    /// #### Parameters
    ///
    /// - `address`: the accessory's UWB MAC address, 2 or 8 bytes
    /// - `channel`: the UWB channel number
    /// - `preambleIndex`: the preamble index that goes with the channel
    /// - `sessionId`: the session id both ends agreed on
    /// - `sessionKey`: the session key, or `null` for an unprovisioned
    ///   session
    ///
    /// #### Returns
    ///
    /// a token that [RangingSession#start] accepts on Android
    public static RangingToken forUwbAddress(byte[] address, int channel,
            int preambleIndex, int sessionId, byte[] sessionKey) {
        if (address == null || (address.length != 2 && address.length != 8)) {
            throw new IllegalArgumentException(
                    "a UWB address is 2 or 8 bytes");
        }
        byte[] key = sessionKey == null ? new byte[0] : sessionKey;
        byte[] out = new byte[4 + address.length + 12 + 4 + key.length];
        int p = 0;
        p = writeInt(out, p, address.length);
        System.arraycopy(address, 0, out, p, address.length);
        p += address.length;
        p = writeInt(out, p, channel);
        p = writeInt(out, p, preambleIndex);
        p = writeInt(out, p, sessionId);
        p = writeInt(out, p, key.length);
        System.arraycopy(key, 0, out, p, key.length);
        return new RangingToken(PLATFORM_ANDROID_UWB, out);
    }

    /// Rebuilds a token from the bytes [#toByteArray()] produced, typically
    /// after they travelled to this device over Bluetooth.
    ///
    /// #### Parameters
    ///
    /// - `data`: the encoded token
    ///
    /// #### Returns
    ///
    /// the decoded token
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the bytes are not a Codename One
    ///   ranging token, or carry a version this build does not understand
    public static RangingToken fromByteArray(byte[] data) {
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("not a ranging token");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) {
                throw new IllegalArgumentException("not a ranging token");
            }
        }
        if ((data[4] & 0xff) != VERSION) {
            throw new IllegalArgumentException(
                    "unsupported ranging token version " + (data[4] & 0xff));
        }
        int plat = data[5] & 0xff;
        int len = readInt(data, 6);
        if (len < 0 || 10 + len > data.length) {
            throw new IllegalArgumentException("truncated ranging token");
        }
        byte[] payload = new byte[len];
        System.arraycopy(data, 10, payload, 0, len);
        return new RangingToken(plat, payload);
    }

    /// The encoded form to hand to the peer. Self-describing, so the
    /// receiving side can tell a corrupt or foreign token from a usable one.
    ///
    /// #### Returns
    ///
    /// a fresh byte array; mutating it does not affect this token
    public byte[] toByteArray() {
        byte[] out = new byte[10 + payload.length];
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
        out[4] = (byte) VERSION;
        out[5] = (byte) platform;
        writeInt(out, 6, payload.length);
        System.arraycopy(payload, 0, out, 10, payload.length);
        return out;
    }

    /// Which platform minted this token -- one of [#PLATFORM_APPLE_NI],
    /// [#PLATFORM_ANDROID_UWB] or [#PLATFORM_SIMULATED]. Useful for telling
    /// the user that the device they are pointing at is the wrong kind,
    /// rather than letting the session fail with
    /// `NearbyError.INVALID_TOKEN`.
    public int getPlatform() {
        return platform;
    }

    /// The platform payload, without the framing.
    ///
    /// @hidden not part of the public API; ports read this to reach the
    /// native token.
    ///
    /// #### Returns
    ///
    /// a fresh copy of the payload bytes
    public byte[] getPayload() {
        byte[] copy = new byte[payload.length];
        System.arraycopy(payload, 0, copy, 0, payload.length);
        return copy;
    }

    /// Wraps a native payload in a token.
    ///
    /// @hidden not part of the public API; ports call this to publish the
    /// local token.
    ///
    /// #### Parameters
    ///
    /// - `platform`: one of the `PLATFORM_` constants
    /// - `payload`: the native payload bytes
    ///
    /// #### Returns
    ///
    /// the wrapped token
    public static RangingToken forPayload(int platform, byte[] payload) {
        return new RangingToken(platform,
                payload == null ? new byte[0] : payload);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RangingToken)) {
            return false;
        }
        RangingToken t = (RangingToken) o;
        if (t.platform != platform || t.payload.length != payload.length) {
            return false;
        }
        for (int i = 0; i < payload.length; i++) {
            if (t.payload[i] != payload[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = platform;
        for (byte b : payload) {
            h = h * 31 + b;
        }
        return h;
    }

    @Override
    public String toString() {
        return "RangingToken[platform=" + platform
                + ", " + payload.length + " bytes]";
    }

    private static int writeInt(byte[] b, int p, int v) {
        b[p] = (byte) ((v >> 24) & 0xff);
        b[p + 1] = (byte) ((v >> 16) & 0xff);
        b[p + 2] = (byte) ((v >> 8) & 0xff);
        b[p + 3] = (byte) (v & 0xff);
        return p + 4;
    }

    private static int readInt(byte[] b, int p) {
        return ((b[p] & 0xff) << 24) | ((b[p + 1] & 0xff) << 16)
                | ((b[p + 2] & 0xff) << 8) | (b[p + 3] & 0xff);
    }
}
