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

    /// Makes `value` safe to carry in a tab-delimited field WITHOUT changing
    /// what it says.
    ///
    /// The call wire's sanitizer turns a tab, carriage return or newline into
    /// a space, which is right for display text and wrong for a secret: a
    /// password or pre-shared key containing one of them was installed as a
    /// different string, the platform acknowledged the install, and
    /// authentication then failed with nothing anywhere to say the credential
    /// had been altered. Every field is escaped rather than only the two that
    /// hold secrets, so there is one rule to remember and no field that is
    /// quietly the exception.
    ///
    /// A backslash escape, matched by `cn1vpUnescape` in CN1Vpn.m -- the
    /// iOS port parses this record itself, in C.
    ///
    /// @param value the field, or null
    /// @return the escaped field, never null
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder b = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String rep = null;
            if (c == '\\') {
                rep = "\\\\";
            } else if (c == '\t') {
                rep = "\\t";
            } else if (c == '\r') {
                rep = "\\r";
            } else if (c == '\n') {
                rep = "\\n";
            }
            if (rep != null) {
                if (b == null) {
                    b = new StringBuilder(value.substring(0, i));
                }
                b.append(rep);
            } else if (b != null) {
                b.append(c);
            }
        }
        return b == null ? value : b.toString();
    }

    /// Reverses [#escape].
    ///
    /// A trailing lone backslash, or one before any other character, is kept
    /// as itself: this decodes records this class wrote, and refusing to
    /// guess is better than dropping a character out of a password.
    ///
    /// @param value the escaped field, or null
    /// @return the original field, never null
    static String unescape(String value) {
        if (value == null) {
            return "";
        }
        int at = value.indexOf('\\');
        if (at < 0) {
            return value;
        }
        StringBuilder b = new StringBuilder(value.substring(0, at));
        int i = at;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                b.append(c);
                i++;
                continue;
            }
            char n = value.charAt(i + 1);
            if (n == '\\') {
                b.append('\\');
            } else if (n == 't') {
                b.append('\t');
            } else if (n == 'r') {
                b.append('\r');
            } else if (n == 'n') {
                b.append('\n');
            } else {
                b.append(c);
                i++;
                continue;
            }
            i += 2;
        }
        return b.toString();
    }

    /// Encodes a profile.
    ///
    /// The credentials travel in the clear within the process and are handed
    /// straight to the platform keychain by the port, which is the only place
    /// they can go. Fields 7 and 8 are reserved empty slots; see the comment
    /// at the field list.
    public static String encodeProfile(VpnProfile p) {
        if (p == null) {
            return "";
        }
        return CallWire.join(new String[]{
            escape(p.getServerAddress()),
            String.valueOf(p.getProtocol().ordinal()),
            escape(p.getRemoteIdentifier()),
            escape(p.getLocalIdentifier()),
            escape(p.getUsername()),
            escape(p.getPassword()),
            escape(p.getSharedSecret()),
            // Field 7 is reserved and always empty: it carried a PKCS#12
            // certificate that neither port could install without its
            // passphrase. Kept as a slot rather than removed so the indices
            // the native parsers use do not shift.
            "",
            // Field 8 likewise: it carried an always-on flag that no ordinary
            // app can ask either platform for.
            "",
            CallWire.flagOf(p.isOnDemand()),
            escape(p.getDisplayName())
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
        String server = unescape(CallWire.field(f, 0));
        if (server.length() == 0) {
            return null;
        }
        VpnProfile p = new VpnProfile(server)
                .protocol(protocol(CallWire.integer(f, 1, 0)))
                .remoteIdentifier(emptyToNull(unescape(CallWire.field(f, 2))))
                .localIdentifier(emptyToNull(unescape(CallWire.field(f, 3))))
                .onDemand(CallWire.flag(f, 9))
                .displayName(emptyToNull(unescape(CallWire.field(f, 10))));
        String user = unescape(CallWire.field(f, 4));
        String pass = unescape(CallWire.field(f, 5));
        if (user.length() > 0 || pass.length() > 0) {
            p.usernamePassword(emptyToNull(user), emptyToNull(pass));
        }
        String secret = unescape(CallWire.field(f, 6));
        if (secret.length() > 0) {
            p.sharedSecret(secret);
        }
        // Fields 7 and 8 are reserved and ignored; see encodeProfile.
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
