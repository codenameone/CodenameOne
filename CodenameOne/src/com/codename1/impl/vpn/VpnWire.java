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
package com.codename1.impl.vpn;

import com.codename1.impl.call.CallWire;
import com.codename1.vpn.VpnError;
import com.codename1.vpn.VpnException;
import com.codename1.vpn.VpnProtocol;
import com.codename1.vpn.VpnStatus;
import com.codename1.vpn.profile.VpnProfile;
import com.codename1.util.Base64;

/// The encoding `com.codename1.vpn.spi.VpnBridge` speaks.
///
/// The field primitives are shared with `com.codename1.impl.call.CallWire`
/// rather than copied: the two families cross the same ParparVM boundary with
/// the same tab-delimited convention, and a second implementation of `split`
/// is a second place for the trailing-empty-field bug to be reintroduced.
///
/// @hidden not part of the public API.
public final class VpnWire {

    private VpnWire() {
    }

    /// Encodes a profile.
    ///
    /// The certificate travels base64-encoded because the record is a text
    /// line; the credentials travel in the clear within the process and are
    /// handed straight to the platform keychain by the port, which is the
    /// only place they can go.
    public static String encodeProfile(VpnProfile p) {
        if (p == null) {
            return "";
        }
        byte[] cert = p.getCertificate();
        return CallWire.join(new String[]{
            p.getServerAddress(),
            String.valueOf(p.getProtocol().ordinal()),
            p.getRemoteIdentifier(),
            p.getLocalIdentifier(),
            p.getUsername(),
            p.getPassword(),
            p.getSharedSecret(),
            cert == null ? "" : Base64.encodeNoNewline(cert),
            CallWire.flagOf(p.isAlwaysOn()),
            CallWire.flagOf(p.isOnDemand()),
            p.getDisplayName()
        });
    }

    /// Decodes a profile, answering null when the record is unusable.
    ///
    /// A record with no server address decodes to null rather than to a
    /// profile with an empty address: the latter would be installable and
    /// would fail later, somewhere with less context.
    public static VpnProfile decodeProfile(String record) {
        if (record == null || record.length() == 0) {
            return null;
        }
        String[] f = CallWire.split(record);
        String server = CallWire.field(f, 0);
        if (server.length() == 0) {
            return null;
        }
        VpnProfile p = new VpnProfile(server)
                .protocol(protocol(CallWire.integer(f, 1, 0)))
                .remoteIdentifier(emptyToNull(CallWire.field(f, 2)))
                .localIdentifier(emptyToNull(CallWire.field(f, 3)))
                .alwaysOn(CallWire.flag(f, 8))
                .onDemand(CallWire.flag(f, 9))
                .displayName(emptyToNull(CallWire.field(f, 10)));
        String user = CallWire.field(f, 4);
        String pass = CallWire.field(f, 5);
        if (user.length() > 0 || pass.length() > 0) {
            p.usernamePassword(emptyToNull(user), emptyToNull(pass));
        }
        String secret = CallWire.field(f, 6);
        if (secret.length() > 0) {
            p.sharedSecret(secret);
        }
        String cert = CallWire.field(f, 7);
        if (cert.length() > 0) {
            p.certificate(Base64.decode(cert.getBytes()));
        }
        return p;
    }

    /// Maps a protocol ordinal, tolerating one this build does not know.
    public static VpnProtocol protocol(int ordinal) {
        VpnProtocol[] values = VpnProtocol.values();
        return ordinal < 0 || ordinal >= values.length
                ? VpnProtocol.IKEV2 : values[ordinal];
    }

    /// Maps a status ordinal, tolerating one this build does not know.
    public static VpnStatus status(int ordinal) {
        VpnStatus[] values = VpnStatus.values();
        return ordinal < 0 || ordinal >= values.length
                ? VpnStatus.UNKNOWN : values[ordinal];
    }

    /// Maps an error ordinal, tolerating one this build does not know.
    public static VpnError error(int ordinal) {
        VpnError[] values = VpnError.values();
        return ordinal < 0 || ordinal >= values.length
                ? VpnError.UNKNOWN : values[ordinal];
    }

    /// Builds the exception a port's failure answer describes. Always
    /// produces something, for the reason `CallWire.decodeError` gives.
    public static VpnException decodeError(int errorOrdinal, String message) {
        VpnError e = error(errorOrdinal);
        if (message == null || message.length() == 0) {
            return new VpnException(e);
        }
        return new VpnException(e, message);
    }

    private static String emptyToNull(String v) {
        return v == null || v.length() == 0 ? null : v;
    }
}
