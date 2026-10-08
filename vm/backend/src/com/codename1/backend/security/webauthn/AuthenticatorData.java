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
package com.codename1.backend.security.webauthn;

/// The authenticator data of a ceremony, taken apart: the 37 bytes every one
/// has -- the hash of the relying party's id, the flags, the signature counter
/// -- and after them, when the flags say so, the new credential and the
/// extensions.
///
/// The bytes are an authenticator's, relayed by a client that may be anyone:
/// every length is checked against what is there, and bytes left over after
/// what the flags announce are refused.
final class AuthenticatorData {
    static final int UP = 0x01;
    static final int UV = 0x04;
    static final int BE = 0x08;
    static final int BS = 0x10;
    static final int AT = 0x40;
    static final int ED = 0x80;

    final byte[] rpIdHash = new byte[32];
    final int flags;
    final long signCount;
    /// Of the attested credential, when [#AT] is set; null otherwise.
    final byte[] aaguid;
    final byte[] credentialId;
    final CoseKey key;

    AuthenticatorData(byte[] data) {
        if (data == null || data.length < 37) {
            throw malformed("it is shorter than the 37 bytes every one has");
        }
        if (data.length > Cbor.MAX_BYTES) {
            throw malformed("it is over " + Cbor.MAX_BYTES + " bytes");
        }
        System.arraycopy(data, 0, rpIdHash, 0, 32);
        flags = data[32] & 0xff;
        signCount = ((data[33] & 0xffL) << 24) | ((data[34] & 0xffL) << 16)
                | ((data[35] & 0xffL) << 8) | (data[36] & 0xffL);
        int at = 37;
        if ((flags & AT) != 0) {
            if (data.length - at < 18) {
                throw malformed("it ends inside the attested credential");
            }
            aaguid = new byte[16];
            System.arraycopy(data, at, aaguid, 0, 16);
            at += 16;
            int length = ((data[at] & 0xff) << 8) | (data[at + 1] & 0xff);
            at += 2;
            if (length == 0 || length > 1023) {
                throw malformed("a credential id of " + length + " bytes; 1 to 1023 are allowed");
            }
            if (data.length - at < length) {
                throw malformed("it ends inside the credential id");
            }
            credentialId = new byte[length];
            System.arraycopy(data, at, credentialId, 0, length);
            at += length;
            if (at >= data.length) {
                throw malformed("it ends where the credential's public key should start");
            }
            int[] next = new int[1];
            key = CoseKey.of(Cbor.decode(data, at, next));
            at = next[0];
        } else {
            aaguid = null;
            credentialId = null;
            key = null;
        }
        if ((flags & ED) != 0) {
            if (at >= data.length) {
                throw malformed("it announces extensions and has none");
            }
            int[] next = new int[1];
            // Read to know where they end; nothing in them is acted on.
            if (!(Cbor.decode(data, at, next) instanceof java.util.Map)) {
                throw malformed("its extensions are not a map");
            }
            at = next[0];
        }
        if (at != data.length) {
            throw malformed((data.length - at) + " bytes follow what its flags announce");
        }
        // Backed up without being eligible for it is a state no authenticator is in.
        if ((flags & BE) == 0 && (flags & BS) != 0) {
            throw new WebAuthnException(WebAuthnException.BACKUP_STATE_INVALID, "The "
                    + "authenticator says the credential is backed up and cannot be");
        }
    }

    boolean has(int flag) {
        return (flags & flag) != 0;
    }

    private static WebAuthnException malformed(String what) {
        return new WebAuthnException(WebAuthnException.MALFORMED_AUTHENTICATOR_DATA,
                "The authenticator data is malformed: " + what);
    }
}
